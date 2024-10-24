/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.sharding;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;

public class LinearSharding {
    public static Map<Slice, Set<Slice>> getShardMapping(
            DocCollection sourceCollection, DocCollection destinationCollection) {
        return destinationCollection.getActiveSlices().stream()
                .collect(Collectors.toMap(
                        Function.identity(), (slice) -> Set.of(sourceCollection.getSlice(slice.getName()))));
    }
}
