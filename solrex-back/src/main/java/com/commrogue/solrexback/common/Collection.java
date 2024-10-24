/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common;

import com.commrogue.solrexback.common.properties.SolrProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@RequiredArgsConstructor
public class Collection {
    private final String collectionName;
    private final String zkConnectionString;

    // allow access to SolrProperties from static context, to allow @JsonCreator to access it
    @Component
    @RequiredArgsConstructor
    private static class SolrPropertiesProvider {
        private static SolrProperties solrProperties;
        private final SolrProperties _solrProperties;

        @PostConstruct
        public void init() {
            SolrPropertiesProvider.solrProperties = _solrProperties;
        }
    }

    @JsonCreator
    public Collection(
            @JsonProperty(value = "collectionName", required = true) String collectionName,
            @JsonProperty("zkConnectionString") String zkConnectionString,
            @JsonProperty("env") String env) {

        if (env != null) {
            this.zkConnectionString = Optional.ofNullable(
                            SolrPropertiesProvider.solrProperties.getEnvs().get(env))
                    .orElseThrow(() -> new IllegalArgumentException("Environment %s not found".formatted(env)));
        } else {
            this.zkConnectionString = zkConnectionString;
        }

        this.collectionName = collectionName;
    }
}
