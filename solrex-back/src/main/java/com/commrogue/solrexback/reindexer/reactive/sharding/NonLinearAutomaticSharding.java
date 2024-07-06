package com.commrogue.solrexback.reindexer.reactive.sharding;

import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class NonLinearAutomaticSharding {
    public static HashMap<Slice, HashSet<Slice>> getShardMapping(
            DocCollection sourceCollection,
            DocCollection destinationCollection) {
        double shardsBalance =
                destinationCollection.getActiveSlices().size() / (double) sourceCollection.getActiveSlices().size();
        AtomicReference<Double> currentShardBalance = new AtomicReference<>(1.0);

        return sourceCollection.getActiveSlices().stream().sorted(Comparator.comparing((s) -> Integer.parseInt(s.getName().substring(5))))
                .reduce(new HashMap<>(), (sliceSetHashMap, sourceSlice) -> {
                    Slice destinationSlice = destinationCollection.getSlice(
                            "shard" + Math.round(currentShardBalance.getAndAccumulate(shardsBalance, Double::sum)));
                    HashSet<Slice> previousSliceSet = sliceSetHashMap.getOrDefault(destinationSlice, new HashSet<>());
                    previousSliceSet.add(sourceSlice);
                    sliceSetHashMap.put(destinationSlice, previousSliceSet);
                    return sliceSetHashMap;
                }, (f, s) -> {
                    f.putAll(s);
                    return f;
                });
    }
}
