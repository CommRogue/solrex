/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.helpers;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.DocCollection;

public class ZkHelpers {
    public static DocCollection getCollection(CloudSolrClient cloudSolrClient, String collectionName) {
        return cloudSolrClient.getClusterState().getCollection(collectionName);
    }
}
