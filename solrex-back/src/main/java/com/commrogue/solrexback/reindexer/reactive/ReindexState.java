package com.commrogue.solrexback.reindexer.reactive;

import com.commrogue.solrexback.common.SolrCoreGatewayInformation;

import java.util.HashMap;
import java.util.Map;

public class ReindexState extends HashMap<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, Integer>> {
    public int totalIndexed;

    public ReindexState(Map<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, Integer>> shardMapping) {
        super(shardMapping);
    }
}
