/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.deserializers;

import com.commrogue.solrexback.common.configuration.SolrConfiguration;
import com.commrogue.solrexback.common.exceptions.UnknownEnvironmentException;
import com.commrogue.solrexback.common.properties.SolrProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.DocCollection;
import org.springframework.boot.jackson.JsonComponent;

// TODO - this is probably not the correct usage of @JsonComponent. I couldn't get it to Autowire when @JsonComponent is
// placed directly on the DocCollectionDeserializer class. Furthermore, if trying to use constructor injection via
// @RequiredArgsConstructor, it results in a content type not supported error since deserillizers are supposed to have a
// non-args constructor. Anyhow, this works, but it would still work without the @JsonComponent, by just having the
// @PostConstruct on a regular @Component.
@JsonComponent
@RequiredArgsConstructor
public class DocCollectionDeserializerComponent {
    private final ObjectMapper objectMapper;
    private final SolrConfiguration.SolrCloudSupplier solrCloudSupplier;
    private final SolrProperties solrProperties;

    @PostConstruct
    public void init() {
        DocCollectionDeserializer.objectMapper = objectMapper;
        DocCollectionDeserializer.solrCloudSupplier = solrCloudSupplier;
        DocCollectionDeserializer.solrProperties = solrProperties;
    }

    public static class DocCollectionDeserializer extends JsonDeserializer<DocCollection> {
        static ObjectMapper objectMapper;
        static SolrConfiguration.SolrCloudSupplier solrCloudSupplier;
        static SolrProperties solrProperties;

        private record CollectionDTO(String env, String collection, String zkConnectionString) {}

        @Override
        public DocCollection deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            CollectionDTO collectionDTO = objectMapper.readValue(p, CollectionDTO.class);
            String zkConnectionString = Optional.ofNullable(collectionDTO.zkConnectionString())
                    .orElseGet(() -> Optional.ofNullable(
                                    solrProperties.getEnvs().get(collectionDTO.env()))
                            .orElseThrow(() -> new UnknownEnvironmentException(
                                    "Environment \"%s\" was not found.\nAvailable environments are: \n%s"
                                            .formatted(
                                                    collectionDTO.env(),
                                                    solrProperties.getEnvs().keySet()))));

            try (CloudSolrClient solrCloudClient = solrCloudSupplier.apply(zkConnectionString)) {
                return solrCloudClient.getClusterState().getCollection(collectionDTO.collection());
            }
        }
    }
}
