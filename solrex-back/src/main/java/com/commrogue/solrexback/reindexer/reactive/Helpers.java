package com.commrogue.solrexback.reindexer.reactive;

import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import java.util.Arrays;
import java.util.Optional;

public class Helpers {
    public static CloudLegacySolrClient getCloudSolrClientFromZk(String zkConnectionString) {
        return new CloudLegacySolrClient.Builder(Arrays.asList(zkConnectionString.split(",")), Optional.empty()).build();
    }
}
