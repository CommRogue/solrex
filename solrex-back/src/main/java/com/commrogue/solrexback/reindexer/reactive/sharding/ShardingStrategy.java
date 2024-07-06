package com.commrogue.solrexback.reindexer.reactive.sharding;

import com.commrogue.solrexback.common.SolrCoreGatewayInformation;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface ShardingStrategy {
    Map<Slice, ? extends Set<Slice>> getShardMapping(DocCollection sourceCollection,
                                                                          DocCollection destinationCollection);
}
