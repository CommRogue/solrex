package com.commrogue.solrexback.reindexer.reactive;

import io.ino.solrs.JavaAsyncSolrClient;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.SimpleOrderedMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Builder(setterPrefix = "with")
public class DataImportRequest {
    private final JavaAsyncSolrClient destinationClient;
    private final String sourceSolrUrl;
    @Builder.Default
    private final String diRequestHandler = "/dih";
    @Singular
    private final List<String> fqs;

    private final SolrQuery dataImportRequest = new SolrQuery();
    private final SolrQuery statusRequest = new SolrQuery();

    void init() {
        dataImportRequest.set("qt", diRequestHandler);
        dataImportRequest.set("command", "full-import");
        dataImportRequest.set("commit", "true");
        dataImportRequest.set("url", sourceSolrUrl);
        dataImportRequest.set("fq", constructDataImportFqsParam(fqs));
        statusRequest.set("qt", diRequestHandler);
        statusRequest.set("command", "status");
    }

    private static String constructDataImportFqsParam(List<String> fqs) {
        return String.join(",", fqs);
    }

    private static int extractNumIndexed(QueryResponse response) {
        return Integer.parseInt(((LinkedHashMap<String, String>) response.getResponse().get("statusMessages")).get(
                "Total Rows Fetched"));
    }

    @SuppressWarnings("unchecked")
    private static int extractStatusCode(QueryResponse response) {
        return ((SimpleOrderedMap<Integer>) (response.getResponse().get("responseHeader"))).get(
                "status");
    }

    private Flux<Integer> observeShardReindex(Mono<QueryResponse> statusObservable) {
        return statusObservable.repeat().delayElements(Duration.ofSeconds(2)).takeUntil(response -> extractStatus(response).equals("idle")).map(
                DataImportRequest::extractNumIndexed);
    }

    @SuppressWarnings("unchecked")
    private static String extractStatus(QueryResponse response) {
        return (String) (response.getResponse().get("status"));
    }

    public static DataImportRequestBuilder builder(JavaAsyncSolrClient destinationClient, String sourceSolrUrl,
                                                   String diRequestHandler) {
        return new PostBuilder().withDestinationClient(destinationClient).withSourceSolrUrl(sourceSolrUrl).withDiRequestHandler(diRequestHandler);
    }

    public static DataImportRequestBuilder builder(String destinationSolrUrl, String sourceSolrUrl, String diRequestHandler) {
        return builder(JavaAsyncSolrClient.create(destinationSolrUrl), sourceSolrUrl, diRequestHandler);
    }

    public Flux<Integer> getSubscribable() {
        return Mono.defer(() -> Mono.fromCompletionStage(this.destinationClient.query(dataImportRequest)))
                .thenMany(observeShardReindex(Mono.defer(() ->
                        Mono.fromCompletionStage(this.destinationClient.query(dataImportRequest)))));
//        return Mono.just(1).doOnNext((x) -> log.info("%s to %s".formatted(this.sourceSolrUrl,
//                this.destinationClient.toString()))).thenMany((Flux.defer(() -> Flux.interval(Duration.ofSeconds(
//                ThreadLocalRandom.current().nextLong(1, 5))).take(3))).map((x) -> (int)x.longValue()));
    }

    public static class PostBuilder extends DataImportRequestBuilder {
        @Override
        public DataImportRequest build() {
            DataImportRequest dataImportRequest = super.build();
            dataImportRequest.init();

            return dataImportRequest;
        }
    }
}