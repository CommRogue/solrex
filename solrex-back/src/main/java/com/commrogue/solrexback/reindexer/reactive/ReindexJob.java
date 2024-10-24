/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import static com.commrogue.solrexback.reindexer.helpers.SolrHelpers.getCloudSolrClientFromZk;
import static com.commrogue.solrexback.reindexer.reactive.Reindex.*;
import static com.commrogue.solrexback.reindexer.web.models.ReindexSpecification.StageOrdering;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.common.web.jobmanager.StatefulJob;
import com.commrogue.solrexback.reindexer.reactive.sharding.NonLinearAutomaticSharding;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import com.commrogue.solrexback.reindexer.web.models.ReindexStageSpecification;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Valid
@Slf4j
@Getter
public class ReindexJob implements StatefulJob {
    private final Function<String, CloudSolrClient> solrClientProvider;
    private final String timestampField;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final Collection srcCollection;
    private final Collection dstCollection;
    private final int timeRangeSplitAmount;
    private final String srcDiRequestHandler;
    private final String dstDiRequestHandler;
    private final List<ReindexStageSpecification> reindexStageSpecifications;
    private final StageOrdering stageOrdering;

    // boxed in order to check for nullability, and if so, delegate default values to be specified by Reindex
    private final Boolean isNatNetworking;
    private final Boolean commit;

    private final AtomicReference<Reindex> currentReindex = new AtomicReference<>();

    @Getter(lazy = true)
    private final Queue<Reindex> remainingReindexes = generateReindexes();

    private Disposable jobDisposable;
    private boolean finished;

    @Builder(setterPrefix = "with")
    private ReindexJob(
            String timestampField,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Collection srcCollection,
            Collection dstCollection,
            Integer timeRangeSplitAmount,
            String srcDiRequestHandler,
            String dstDiRequestHandler,
            Boolean isNatNetworking,
            Boolean commit,
            List<ReindexStageSpecification> reindexStageSpecifications,
            StageOrdering stageOrdering,
            Function<String, CloudSolrClient> solrClientProvider) {
        if (timeRangeSplitAmount == null) {
            this.timeRangeSplitAmount = 1;
        } else if (timeRangeSplitAmount < 1) {
            throw new IllegalArgumentException("timeRangeSplitAmount must be positive if given");
        } else {
            this.timeRangeSplitAmount = timeRangeSplitAmount;
        }

        this.timestampField = timestampField;
        this.startDate = startDate;
        this.endDate = endDate;
        this.srcCollection = srcCollection;
        this.dstCollection = dstCollection;
        this.srcDiRequestHandler = srcDiRequestHandler;
        this.dstDiRequestHandler = dstDiRequestHandler;
        this.isNatNetworking = isNatNetworking;
        this.commit = commit;
        this.reindexStageSpecifications = reindexStageSpecifications;
        this.stageOrdering = stageOrdering;
        this.solrClientProvider = solrClientProvider;
    }

    public static ReindexJobBuilder builder(Function<String, CloudSolrClient> solrClientProvider) {
        return new ReindexJobBuilder().withSolrClientProvider(solrClientProvider);
    }

    public ReindexJob(ReindexSpecification reindexSpecification, Function<String, CloudSolrClient> solrClientProvider) {
        this(
                reindexSpecification.getTimestampField(),
                reindexSpecification.getStartDate(),
                reindexSpecification.getEndDate(),
                reindexSpecification.getSrcCollection(),
                reindexSpecification.getDstCollection(),
                reindexSpecification.getTimeRangeSplitAmount(),
                reindexSpecification.getSrcDiRequestHandler(),
                reindexSpecification.getDstDiRequestHandler(),
                reindexSpecification.getIsNatNetworking(),
                reindexSpecification.getShouldCommit(),
                reindexSpecification.getStages(),
                reindexSpecification.getStageOrdering(),
                solrClientProvider);
    }

    // TODO - absolute garbage code
    private Stream<Reindex> generateStages(ReindexBuilder baseReindexBuilder, boolean globalCommit) {
        return reindexStageSpecifications.stream().map(stage -> {
            baseReindexBuilder.withFqs(stage.getFqs());
            if (stage.getShouldCommitOverride() != null) {
                baseReindexBuilder.withCommit(stage.getShouldCommitOverride());
            } else {
                baseReindexBuilder.withCommit(globalCommit);
            }

            Reindex reindex = baseReindexBuilder.build();
            baseReindexBuilder.clearFqs();

            return reindex;
        });
    }

    private Queue<Reindex> generateReindexes() {
        DocCollection sourceCollection = getCloudSolrClientFromZk(
                        this.getSrcCollection().getZkConnectionString())
                .getClusterState()
                .getCollection(this.getSrcCollection().getCollectionName());
        DocCollection destinationCollection = getCloudSolrClientFromZk(
                        this.getDstCollection().getZkConnectionString())
                .getClusterState()
                .getCollection(this.getDstCollection().getCollectionName());

        Duration stageDuration =
                Duration.between(this.getStartDate(), this.getEndDate()).dividedBy(this.getTimeRangeSplitAmount());

        return IntStream.range(0, this.getTimeRangeSplitAmount())
                .mapToObj(stageIndex -> {
                    ReindexBuilder builder = Reindex.builder(
                                    sourceCollection,
                                    destinationCollection,
                                    NonLinearAutomaticSharding::getShardMapping)
                            .withTimestampField(this.getTimestampField())
                            .withStartTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex)))
                            .withEndTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1)));

                    Optional.ofNullable(this.getSrcDiRequestHandler()).ifPresent(builder::withSrcDiRequestHandler);
                    Optional.ofNullable(this.getDstDiRequestHandler()).ifPresent(builder::withDstDiRequestHandler);
                    Optional.ofNullable(this.getCommit()).ifPresent(builder::withCommit);

                    if (this.getReindexStageSpecifications() == null
                            || !this.getReindexStageSpecifications().isEmpty()) {
                        return Stream.of(builder.build());
                    }

                    return generateStages(builder, Boolean.TRUE.equals(this.getCommit()));
                })
                .flatMap(Function.identity())
                .collect(Collectors.toCollection(
                        // TODO - when prioritizing time splits, stage order is not guaranteed to be maintained
                        () -> {
                            if (this.stageOrdering == StageOrdering.PRIORITIZE_TIME_SPLITS) {
                                return new PriorityQueue<>(Comparator.comparing(Reindex::getStartTime));
                            }
                            return new LinkedList<>();
                        }));
    }

    public Mono<Void> run() {
        return Flux.<Reindex>generate(sink -> {
                    Reindex stage = this.getRemainingReindexes().poll();
                    this.currentReindex.set(stage);
                    if (stage == null) {
                        this.currentReindex.set(null);
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
