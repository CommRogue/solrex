package com.commrogue.solrexback.reindexer.reactive;

import com.commrogue.solrexback.common.exceptions.InvalidResponseException;
import com.commrogue.solrexback.reindexer.exceptions.OngoingDataImportException;
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

    private void init() {
        dataImportRequest.set("qt", "/dataimport");
        dataImportRequest.set("internalDih", diRequestHandler);
        dataImportRequest.set("command", "full-import");
        dataImportRequest.set("commit", "true");
        dataImportRequest.set("url", sourceSolrUrl);
        dataImportRequest.set("fq", constructDataImportFqsParam(fqs));
        statusRequest.set("qt", "/dataimport");
        statusRequest.set("command", "status");
    }

    private static String constructDataImportFqsParam(List<String> fqs) {
        return String.join(",", fqs);
    }

    private static int extractNumIndexed(QueryResponse response) {
        try {
            return Integer.parseInt(((LinkedHashMap<String, String>) response.getResponse().get("statusMessages")).get(
                    "Total Rows Fetched"));
        } catch (NullPointerException e) {
            throw new InvalidResponseException("Unable to extract number of indexed documents from DataImport " +
                    "response. Verify that you are using the correct DataImport request handler", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static int extractStatusCode(QueryResponse response) {
        try {
            return ((SimpleOrderedMap<Integer>) (response.getResponse().get("responseHeader"))).get(
                    "status");
        } catch (NullPointerException e) {
            throw new InvalidResponseException("Unable to extract status code from DataImport response", e);
        }
    }

    private Flux<Integer> observeShardReindex(Mono<QueryResponse> statusObservable) {
        return statusObservable.repeatWhen((status) -> status.delayElements(Duration.ofSeconds(2)))
                .takeUntil(response -> extractStatus(response).equals("idle")).map(
                        DataImportRequest::extractNumIndexed);
    }

    @SuppressWarnings("unchecked")
    private static String extractStatus(QueryResponse response) {
        String status = (String) (response.getResponse().get("status"));

        if (status != null) {
            return status;
        }

        throw new InvalidResponseException(
                "Unable to extract status from DataImport response. Verify that you are using the correct DataImport request handler");
    }

    public static DataImportRequestBuilder builder(JavaAsyncSolrClient destinationClient, String sourceSolrUrl) {
        return new PostBuilder().withDestinationClient(destinationClient).withSourceSolrUrl(sourceSolrUrl);
    }

    public static DataImportRequestBuilder builder(String destinationSolrUrl, String sourceSolrUrl) {
        return builder(JavaAsyncSolrClient.create(destinationSolrUrl), sourceSolrUrl);
    }

    public static Mono<QueryResponse> makeRequestAndLog(JavaAsyncSolrClient destinationClient, SolrQuery request) {
        log.debug("Sending SolrQuery to {} - {}", destinationClient,
                request);

        return Mono.fromCompletionStage(destinationClient.query(request));
    }

    public Flux<Integer> getSubscribable() {
        return Mono.defer(() -> makeRequestAndLog(destinationClient, statusRequest)
                        .map(DataImportRequest::extractStatus
                        ).doOnNext((status) -> {
                            if (status.equals("busy")) {
                                throw new OngoingDataImportException("A DataImport request is already in progress");
                            }
                        }).then(Mono.defer(() -> {
                            log.debug("Sending DataImport request to {} - {}", this.sourceSolrUrl,
                                    dataImportRequest);
                            return makeRequestAndLog(destinationClient, dataImportRequest);
                        })))
                .thenMany(observeShardReindex(Mono.defer(() ->
                        makeRequestAndLog(destinationClient, dataImportRequest))));
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