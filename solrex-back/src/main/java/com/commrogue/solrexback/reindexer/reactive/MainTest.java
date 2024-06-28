package com.commrogue.solrexback.reindexer.reactive;

import org.apache.solr.client.solrj.impl.CloudLegacySolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainTest {
    public static void main(String[] args) throws IOException {
        try (CloudLegacySolrClient destinationClient = new CloudLegacySolrClient.Builder(List.of("localhost:2182"), Optional.empty()).build();
             CloudLegacySolrClient sourceClient = new CloudLegacySolrClient.Builder(List.of("localhost:2181"), Optional.empty()).build()) {
            var destinationCollection = destinationClient.getClusterState().getCollection("products");
            var sourceCollection = sourceClient.getClusterState().getCollection("xax");
            destinationCollection.getActiveSlices().forEach(slice -> {
                System.out.println(slice.getLeader().getCoreUrl());
            });
            System.out.println("ew");
        }
    }
}
