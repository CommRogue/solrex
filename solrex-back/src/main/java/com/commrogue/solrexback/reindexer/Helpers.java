package com.commrogue.solrexback.reindexer;

import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;

import java.util.Arrays;
import java.util.Optional;

public class Helpers {
    public static CloudLegacySolrClient getCloudSolrClientFromZk(String zkConnectionString) {
        return new CloudLegacySolrClient.Builder(Arrays.asList(zkConnectionString.split(",")), Optional.empty()).build();
    }
}
