package com.commrogue.solrexback.reindexer.reactive;

import lombok.Data;
import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Data
public class ReindexStage {
    private final LocalDateTime startTime;

    private final LocalDateTime endTime;

    private final ShardMapping shardMapping;

    @Value("${reindexer.dataimport-rh}")
    String diRequestHandler;

    public Mono<Void> run() {
        return this.getShardMapping().forEach((dstClient, sources) -> {
            SolrQuery dataImportRequest = new SolrQuery();
            dataImportRequest.set("qt", diRequestHandler);
            dataImportRequest.set("command", "full-import");
            dataImportRequest.set("commit", "true");
            SolrQuery statusRequest = new SolrQuery();
            statusRequest.set("qt", diRequestHandler);
            statusRequest.set("command", "status");
            Mono.fromCompletionStage(dstClient.query(dataImportRequest)).then(Mono.fromCompletionStage(dstClient.query(statusRequest))).()
        });
    }
}
