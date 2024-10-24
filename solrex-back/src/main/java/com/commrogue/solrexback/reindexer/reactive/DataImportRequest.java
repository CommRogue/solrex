/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import com.commrogue.solrexback.common.exceptions.InvalidResponseException;
import com.commrogue.solrexback.reindexer.exceptions.OngoingDataImportException;
import io.ino.solrs.JavaAsyncSolrClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.SimpleOrderedMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Builder(setterPrefix = "with")
public class DataImportRequest {

    private final JavaAsyncSolrClient destinationClient;
    private final String sourceSolrUrl;
    private final String srcDiRequestHandler;
    private final String dstDiRequestHandler;
    private final boolean commit;

    @Singular
    private final List<String> fqs;

    private final SolrQuery dataImportRequest = new SolrQuery();
    private final SolrQuery statusRequest = new SolrQuery();
    private final SolrQuery cancelRequest = new SolrQuery();

    private void init() {
        dataImportRequest.set("qt", this.dstDiRequestHandler);
        dataImportRequest.set("internalDih", this.srcDiRequestHandler);
        dataImportRequest.set("command", "full-import");
        dataImportRequest.set("commit", this.commit);
        dataImportRequest.set("url", sourceSolrUrl);
        dataImportRequest.set("fq", constructDataImportFqsParam(fqs));
        statusRequest.set("qt", dstDiRequestHandler);
        statusRequest.set("command", "status");
        cancelRequest.set("qt", dstDiRequestHandler);
        cancelRequest.set("command", "abort");
    }

    private static String constructDataImportFqsParam(List<String> fqs) {
        return String.join(",", fqs);
    }

    private static int extractNumIndexed(QueryResponse response) {
        try {
            return Integer.parseInt(
                    ((LinkedHashMap<String, String>) response.getResponse().get("statusMessages"))
                            .get("Total Rows Fetched"));
        } catch (NullPointerException e) {
            throw new InvalidResponseException(
                    "Unable to extract number of indexed documents from DataImport response. Verify that you are using the correct DataImport request handler",
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private static int extractStatusCode(QueryResponse response) {
        try {
            return ((SimpleOrderedMap<Integer>) (response.getResponse().get("responseHeader"))).get("status");
        } catch (NullPointerException e) {
            throw new InvalidResponseException("Unable to extract status code from DataImport response", e);
        }
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
        log.atInfo()
                .addKeyValue("request", request)
                .addKeyValue("destinationClient", destinationClient)
                .setMessage("Sending SolrQuery")
                .log();

        return Mono.fromCompletionStage(destinationClient.query(request));
    }

    private Flux<Integer> observeShardReindex(Mono<QueryResponse> statusObservable) {
        return statusObservable
                .repeatWhen(status -> status.delayElements(Duration.ofSeconds(2)))
                .takeUntil(response -> extractStatus(response).equals("idle"))
                .map(DataImportRequest::extractNumIndexed);
    }

    public Flux<Integer> getSubscribable() {
        return Mono.defer(() -> makeRequestAndLog(destinationClient, statusRequest)
                        .map(DataImportRequest::extractStatus)
                        .doOnNext(status -> {
                            if (status.equals("busy")) {
                                throw new OngoingDataImportException("A DataImport request is already in progress");
                            }
                        })
                        .then(Mono.defer(() -> {
                            log.debug("Sending DataImport request to {} - {}", this.sourceSolrUrl, dataImportRequest);

                            return makeRequestAndLog(destinationClient, dataImportRequest);
                        })))
                .thenMany(observeShardReindex(Mono.defer(() -> makeRequestAndLog(destinationClient, statusRequest))))
                .doOnCancel(() -> makeRequestAndLog(destinationClient, cancelRequest)
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe());
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
