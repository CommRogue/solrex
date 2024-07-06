package com.commrogue.solrexback.reindexer.reactive;

import io.ino.solrs.JavaAsyncSolrClient;

import java.util.HashMap;
import java.util.Map;

public class ReindexState extends HashMap<String, Map<String, Integer>> {
    public int totalIndexed;

    public ReindexState(Map<String, Map<String, Integer>> shardMapping) {
        super(shardMapping);
    }
}
