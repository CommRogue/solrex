/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.sharding;

import java.util.Map;
import java.util.Set;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;

@FunctionalInterface
public interface ShardingStrategy {
    Map<Slice, ? extends Set<Slice>> getShardMapping(
            DocCollection sourceCollection, DocCollection destinationCollection);
}
