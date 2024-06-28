package com.commrogue.solrexback.reindexer.reactive;

import io.ino.solrs.JavaAsyncSolrClient;

import java.util.HashMap;
import java.util.Map;

public class ShardMapping extends HashMap<JavaAsyncSolrClient, Map<JavaAsyncSolrClient, Integer>> {
    public int totalIndexed;

    public ShardMapping(Map<JavaAsyncSolrClient, Map<JavaAsyncSolrClient, Integer>> shardMapping) {
        super(shardMapping);
    }
}
