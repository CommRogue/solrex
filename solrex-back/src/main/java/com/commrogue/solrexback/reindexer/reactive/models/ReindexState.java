/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.models;

import com.commrogue.solrexback.common.SolrCoreGatewayInformation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.solr.common.cloud.Slice;

public class ReindexState
        extends HashMap<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, DataImportRequestState>> {

    private ReindexState(
            Map<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, DataImportRequestState>> shardMapping) {
        super(shardMapping);
    }

    private static SolrCoreGatewayInformation getLeaderUrlForShardSlice(Slice shardSlice, boolean isNatNetworking) {
        var coreUrl = shardSlice.getLeader().getCoreUrl();

        return new SolrCoreGatewayInformation(
                coreUrl, isNatNetworking ? coreUrl.replaceFirst("192\\.168\\.\\d+\\.\\d+", "localhost") : coreUrl);
    }

    public static ReindexState fromSliceMapping(
            Map<Slice, ? extends Set<Slice>> shardMapping, boolean isNatNetworking) {
        return new ReindexState(shardMapping.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> getLeaderUrlForShardSlice(entry.getKey(), isNatNetworking),
                        entry -> entry.getValue().stream()
                                .collect(Collectors.toMap(
                                        slice -> getLeaderUrlForShardSlice(slice, isNatNetworking),
                                        slice -> new DataImportRequestState())))));
    }

    public long getSumImportedDocuments() {
        return this.values().stream()
                .mapToLong(sourceMap -> sourceMap.values().stream()
                        .mapToLong(DataImportRequestState::getIndexed)
                        .sum())
                .sum();
    }
}
