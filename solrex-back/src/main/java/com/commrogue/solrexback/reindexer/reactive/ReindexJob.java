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
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Valid
@Slf4j
@Getter
public class ReindexJob implements StatefulJob {

    private String timestampField;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final Collection srcCollection;
    private final Collection dstCollection;
    private final int stagingAmount;
    private final String srcDiRequestHandler;
    private final String dstDiRequestHandler;
    // boxed in order to check for nullability, and if so, delegate default values to be specified by Reindex
    private final Boolean isNatNetworking;
    private final Boolean commit;

    private final AtomicReference<Reindex> currentStage = new AtomicReference<>();

    @Getter(lazy = true)
    private final Queue<Reindex> remainingStages = generateStages();

    private Disposable jobDisposable;
    private boolean finished;

    @Builder(setterPrefix = "with")
    private ReindexJob(
            String timestampField,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Collection srcCollection,
            Collection dstCollection,
            int stagingAmount,
            String srcDiRequestHandler,
            String dstDiRequestHandler,
            Boolean isNatNetworking,
            Boolean commit) {
        this.timestampField = timestampField;
        this.startDate = startDate;
        this.endDate = endDate;
        this.srcCollection = srcCollection;
        this.dstCollection = dstCollection;
        this.stagingAmount = stagingAmount;
        this.srcDiRequestHandler = srcDiRequestHandler;
        this.dstDiRequestHandler = dstDiRequestHandler;
        this.isNatNetworking = isNatNetworking;
        this.commit = commit;
    }

    public ReindexJob(ReindexSpecification reindexSpecification) {
        this.timestampField = reindexSpecification.getTimestampField();
        this.startDate = reindexSpecification.getStartDate();
        this.endDate = reindexSpecification.getEndDate();
        this.srcCollection = reindexSpecification.getSrcCollection();
        this.dstCollection = reindexSpecification.getDstCollection();
        this.stagingAmount = reindexSpecification.getStagingAmount();
        this.srcDiRequestHandler = reindexSpecification.getSrcDiRequestHandler();
        this.dstDiRequestHandler = reindexSpecification.getDstDiRequestHandler();
        this.isNatNetworking = reindexSpecification.getIsNatNetworking();
        this.commit = reindexSpecification.getShouldCommit();
    }

    public Queue<Reindex> generateStages() {
        DocCollection sourceCollection = getCloudSolrClientFromZk(
                        this.getSrcCollection().getZkConnectionString())
                .getClusterState()
                .getCollection(this.getSrcCollection().getCollectionName());
        DocCollection destinationCollection = getCloudSolrClientFromZk(
                        this.getDstCollection().getZkConnectionString())
                .getClusterState()
                .getCollection(this.getDstCollection().getCollectionName());

        Duration stageDuration =
                Duration.between(this.getStartDate(), this.getEndDate()).dividedBy(this.getStagingAmount());

        return IntStream.range(0, this.getStagingAmount())
                .mapToObj(stageIndex -> {
                    Reindex.ReindexBuilder builder = Reindex.builder(
                                    sourceCollection,
                                    destinationCollection,
                                    NonLinearAutomaticSharding::getShardMapping)
                            .withTimestampField(this.getTimestampField())
                            .withStartTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex)))
                            .withEndTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1)));

                    Optional.ofNullable(this.getSrcDiRequestHandler()).ifPresent(builder::withSrcDiRequestHandler);
                    Optional.ofNullable(this.getDstDiRequestHandler()).ifPresent(builder::withDstDiRequestHandler);
                    Optional.ofNullable(this.getCommit()).ifPresent(builder::withCommit);

                    return builder.build();
                })
                .collect(Collectors.toCollection(
                        () -> new PriorityQueue<>(Comparator.comparing(Reindex::getStartTime))));
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
                    log.atInfo()
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
        this.jobDisposable = this.run().subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
