/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.configuration;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SolrConfiguration {
    @FunctionalInterface
    public interface SolrCloudSupplier extends Function<String, CloudSolrClient> {}

    @Bean
    @ConditionalOnProperty(name = "solr.networking", havingValue = "legacy", matchIfMissing = true)
    public SolrCloudSupplier getLegacyCloudSolrClient() {
        return zkConnectionString -> new CloudLegacySolrClient.Builder(
                        Arrays.asList(zkConnectionString.split(",")), Optional.empty())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "solr.networking", havingValue = "modern")
    public SolrCloudSupplier SolrCloudSuppliergetHttp2CloudSolrClient() {
        return zkConnectionString ->
                new CloudSolrClient.Builder(Arrays.asList(zkConnectionString.split(",")), Optional.empty()).build();
    }
}
