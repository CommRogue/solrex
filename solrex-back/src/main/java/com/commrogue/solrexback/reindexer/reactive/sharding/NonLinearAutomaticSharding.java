/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.sharding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;

public class NonLinearAutomaticSharding {
    public static HashMap<Slice, HashSet<Slice>> getShardMapping(
            DocCollection sourceCollection, DocCollection destinationCollection) {
        double shardsBalanceRatio = destinationCollection.getActiveSlices().size()
                / (double) sourceCollection.getActiveSlices().size();
        AtomicReference<Double> currentBalancedDestinationShard = new AtomicReference<>(1.0);

        return sourceCollection.getActiveSlices().stream()
                .sorted(Comparator.comparing((s) -> Integer.parseInt(s.getName().substring(5))))
                .reduce(
                        new HashMap<>(),
                        (sliceSetHashMap, sourceSlice) -> {
                            Slice destinationSlice = destinationCollection.getSlice("shard"
                                    + Math.round(currentBalancedDestinationShard.getAndAccumulate(
                                            shardsBalanceRatio, Double::sum)));
                            HashSet<Slice> previousSliceSet =
                                    sliceSetHashMap.getOrDefault(destinationSlice, new HashSet<>());
                            previousSliceSet.add(sourceSlice);
                            sliceSetHashMap.put(destinationSlice, previousSliceSet);
                            return sliceSetHashMap;
                        },
                        (f, s) -> {
                            f.putAll(s);
                            return f;
                        });
    }
}
