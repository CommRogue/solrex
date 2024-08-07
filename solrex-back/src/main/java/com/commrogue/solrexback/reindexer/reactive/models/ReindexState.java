package com.commrogue.solrexback.reindexer.reactive.models;

import com.commrogue.solrexback.common.SolrCoreGatewayInformation;
import org.apache.solr.common.cloud.Slice;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReindexState
        extends HashMap<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, DataImportRequestState>> {
    public ReindexState(
            Map<SolrCoreGatewayInformation, Map<SolrCoreGatewayInformation, DataImportRequestState>> shardMapping) {
        super(shardMapping);
    }

    private static SolrCoreGatewayInformation getLeaderUrlForShardSlice(Slice shardSlice,
                                                                        boolean isNatNetworking) {
        var coreUrl = shardSlice.getLeader().getCoreUrl();

        return new SolrCoreGatewayInformation(coreUrl, isNatNetworking ? coreUrl.replaceFirst("192\\.168\\.\\d+\\" +
                        ".\\d+",
                "localhost") : coreUrl);
    }

    public static ReindexState fromSliceMapping(Map<Slice, ? extends Set<Slice>> shardMapping,
                                                boolean isNatNetworking) {
        return new ReindexState(shardMapping.entrySet().stream()
                .collect(Collectors.toMap((entry) -> getLeaderUrlForShardSlice(entry.getKey(), isNatNetworking),
                        (entry) ->
                                entry.getValue().stream().collect(Collectors.toMap((e) -> getLeaderUrlForShardSlice(e,
                                        isNatNetworking), (_) -> new DataImportRequestState())))));
    }
}
