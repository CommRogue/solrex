/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import static com.commrogue.solrexback.reindexer.helpers.SolrHelpers.getCloudSolrClientFromZk;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.common.web.jobmanager.StatefulJob;
import com.commrogue.solrexback.reindexer.reactive.sharding.NonLinearAutomaticSharding;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
@Valid
@Slf4j
public class ReindexJob implements StatefulJob {

    @NonNull private final ReindexSpecification reindexSpecification;

    @Getter
    private final AtomicReference<Reindex> currentStage =
        new AtomicReference<>();

    @Getter(lazy = true)
    private final Queue<Reindex> remainingStages = generateStages();

    private Disposable jobDisposable;
    private boolean finished;

    public ReindexJob(
        String timestampField,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Collection srcCollection,
        Collection dstCollection,
        int stagingAmount,
        String diRequestHandler,
        boolean isNatNetworking
    ) {
        this.reindexSpecification = ReindexSpecification.builder()
            .withTimestampField(timestampField)
            .withStartDate(startDate)
            .withEndDate(endDate)
            .withSrcCollection(srcCollection)
            .withDstCollection(dstCollection)
            .withStagingAmount(stagingAmount)
            .withDiRequestHandler(diRequestHandler)
            .withIsNatNetworking(isNatNetworking)
            .build();
    }

    public Queue<Reindex> generateStages() {
        DocCollection sourceCollection = getCloudSolrClientFromZk(
            reindexSpecification.getSrcCollection().getZkConnectionString()
        )
            .getClusterState()
            .getCollection(
                reindexSpecification.getSrcCollection().getCollectionName()
            );
        DocCollection destinationCollection = getCloudSolrClientFromZk(
            reindexSpecification.getDstCollection().getZkConnectionString()
        )
            .getClusterState()
            .getCollection(
                reindexSpecification.getDstCollection().getCollectionName()
            );

        Duration stageDuration = Duration.between(
            reindexSpecification.getStartDate(),
            reindexSpecification.getEndDate()
        ).dividedBy(reindexSpecification.getStagingAmount());

        return IntStream.range(0, reindexSpecification.getStagingAmount())
            .mapToObj(stageIndex ->
                Reindex.builder(
                    sourceCollection,
                    destinationCollection,
                    NonLinearAutomaticSharding::getShardMapping
                )
                    .withDiRequestHandler(
                        reindexSpecification.getDiRequestHandler()
                    )
                    .withTimestampField(
                        reindexSpecification.getTimestampField()
                    )
                    .withStartTime(
                        reindexSpecification
                            .getStartDate()
                            .plus(stageDuration.multipliedBy(stageIndex))
                    )
                    .withEndTime(
                        reindexSpecification
                            .getStartDate()
                            .plus(stageDuration.multipliedBy(stageIndex + 1))
                    )
                    .withIsNatNetworking(reindexSpecification.isNatNetworking())
                    .build()
            )
            .collect(
                Collectors.toCollection(() ->
                    new PriorityQueue<>(
                        Comparator.comparing(Reindex::getStartTime)
                    )
                )
            );
    }

    public Mono<Void> run() {
        return Flux.<Reindex>generate(sink -> {
            Reindex stage = this.getRemainingStages().poll();
            this.currentStage.set(stage);
            if (stage == null) {
                this.currentStage.set(null);
                sink.complete();
            } else {
                sink.next(stage);
            }
        })
            .concatMap(stage -> stage.getSubscribable().thenReturn(stage))
            .doOnNext(x -> {
                log
                    .atInfo()
                    .addKeyValue("timestamp", x.getStartTime())
                    .setMessage("Reindex complete")
                    .log();
                this.finished = true;
            })
            .then();
    }

    @Override
    public State getState() {
        if (finished) {
            return State.FINISHED;
        }

        if (this.jobDisposable == null) {
            return State.AWAITING;
        } else {
            if (this.jobDisposable.isDisposed()) {
                return State.TERMINATED;
            } else {
                return State.RUNNING;
            }
        }
    }

    @Override
    public void terminate() {
        if (this.jobDisposable != null) {
            this.jobDisposable.dispose();
        }
    }

    @Override
    public void start() {
        this.jobDisposable = this.run()
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
}
