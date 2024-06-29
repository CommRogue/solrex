package com.commrogue.solrexback.reindexer.reactive;

import lombok.Data;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;

@Data
public class ReindexStage {
    private final LocalDateTime startTime;

    private final LocalDateTime endTime;

    private final ShardMapping shardMapping;

    @Value("${reindexer.dataimport-rh}")
    String diRequestHandler;

    @SuppressWarnings("unchecked")
    private static int extractNumIndexed(QueryResponse response) {
        return Integer.parseInt(((LinkedHashMap<String, String>) response.getResponse().get("statusMessages")).get(
                "Total Rows Fetched"));
    }

    @SuppressWarnings("unchecked")
    private static int extractStatusCode(QueryResponse response) {
        return ((SimpleOrderedMap<Integer>) (response.getResponse().get("responseHeader"))).get(
                "status");
    }

    @SuppressWarnings("unchecked")
    private static String extractStatus(QueryResponse response) {
        return (String) (response.getResponse().get("status"));
    }

    private Flux<Integer> observeShardReindex(Mono<QueryResponse> statusObservable) {
        return statusObservable.repeat().takeUntil(response -> extractStatus(response).equals("idle")).map(
                ReindexStage::extractNumIndexed);
    }

    public Mono<Void> run() {
        SolrQuery dataImportRequest = new SolrQuery();
        dataImportRequest.set("qt", diRequestHandler);
        dataImportRequest.set("command", "full-import");
        dataImportRequest.set("commit", "true");
        SolrQuery statusRequest = new SolrQuery();
        statusRequest.set("qt", diRequestHandler);
        statusRequest.set("command", "status");

        return this.getShardMapping().entrySet().stream().reduce(Flux.<Void>empty(), (stageMono, entry) ->
                stageMono.mergeWith(
                        entry.getValue().entrySet().stream().reduce(Flux.<Void>empty(), (destShardMono, sourceEntry) ->
                                destShardMono.concatWith(Mono.defer(
                                                () -> Mono.fromCompletionStage(entry.getKey().query(dataImportRequest)))
                                        .then(observeShardReindex(Mono.defer(() -> Mono.fromCompletionStage(
                                                entry.getKey().query(dataImportRequest)))).doOnNext(
                                                sourceEntry::setValue).then())
                                ), Flux::concatWith).then()
                ), Flux::mergeWith).then();
    }
}
