/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.helpers;

import java.util.Arrays;
import java.util.Optional;
import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;

public class SolrHelpers {

    public static CloudLegacySolrClient getCloudSolrClientFromZk(String zkConnectionString) {
        return new CloudLegacySolrClient.Builder(Arrays.asList(zkConnectionString.split(",")), Optional.empty())
                .build();
    }
}
