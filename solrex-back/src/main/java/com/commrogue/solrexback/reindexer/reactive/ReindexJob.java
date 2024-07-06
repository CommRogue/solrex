package com.commrogue.solrexback.reindexer.reactive;


import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commrogue.solrexback.reindexer.reactive.Helpers.getCloudSolrClientFromZk;

@RequiredArgsConstructor
@Valid
@Slf4j
public class ReindexJob {
    @NonNull
    private final ReindexSpecification reindexSpecification;
    @Getter
    private final AtomicReference<Reindex> currentStage = new AtomicReference<>();

    public ReindexJob(LocalDateTime startDate, LocalDateTime endDate, Collection srcCollection,
                      Collection dstCollection, int stagingAmount, String diRequestHandler, boolean isNatNetworking) {
        this.reindexSpecification =
                new ReindexSpecification(srcCollection, dstCollection, startDate, endDate, stagingAmount,
                        diRequestHandler, isNatNetworking);
    }

    @Getter(lazy = true)
    private final Queue<Reindex> stages = generateStages();

    public Queue<Reindex> generateStages() {
        DocCollection sourceCollection = getCloudSolrClientFromZk(
                reindexSpecification.getSrcCollection().getZkConnectionString()).getClusterState()
                .getCollection(reindexSpecification.getSrcCollection().getCollectionName());
        DocCollection destinationCollection = getCloudSolrClientFromZk(
                reindexSpecification.getDstCollection().getZkConnectionString()).getClusterState()
                .getCollection(reindexSpecification.getDstCollection().getCollectionName());

        Duration stageDuration =
                Duration.between(reindexSpecification.getStartDate(), reindexSpecification.getEndDate())
                        .dividedBy(reindexSpecification.getStagingAmount());

        return IntStream.range(0, reindexSpecification.getStagingAmount()).mapToObj(
                        (stageIndex) -> Reindex.builder(reindexSpecification.getDiRequestHandler())
                                .withStartTime(reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex)))
                                .withEndTime(
                                        reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1)))
                                .withIsNatNetworking(reindexSpecification.isNatNetworking())
                                .withLinearSharding(sourceCollection, destinationCollection).build())
                .collect(Collectors.toCollection(
                        () -> new PriorityQueue<>(Comparator.comparing(Reindex::getStartTime))));
    }

    public Mono<Void> run() {
        return Flux.<Reindex>generate((sink) -> {
            Reindex stage = this.getStages().poll();
            this.currentStage.set(stage);
            if (stage == null) {
                this.currentStage.set(null);
                sink.complete();
            } else {
                sink.next(stage);
            }
        }).concatMap((stage) -> stage.getSubscribable().thenReturn(stage)).doOnNext((x) -> log.info(
                "Reindex complete %s".formatted(x.getStartTime()))).then();
    }
}
