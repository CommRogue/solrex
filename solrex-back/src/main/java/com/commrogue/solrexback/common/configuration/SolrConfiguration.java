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
    @Bean
    @ConditionalOnProperty(name = "solr.networking", havingValue = "legacy", matchIfMissing = true)
    public Function<String, CloudSolrClient> getLegacyCloudSolrClient() {
        return zkConnectionString -> new CloudLegacySolrClient.Builder(
                        Arrays.asList(zkConnectionString.split(",")), Optional.empty())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "solr.networking", havingValue = "modern")
    public Function<String, CloudSolrClient> getHttp2CloudSolrClient() {
        return zkConnectionString ->
                new CloudSolrClient.Builder(Arrays.asList(zkConnectionString.split(",")), Optional.empty()).build();
    }
}
