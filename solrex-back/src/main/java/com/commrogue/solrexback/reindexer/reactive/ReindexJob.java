/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import static com.commrogue.solrexback.reindexer.helpers.SolrHelpers.getCloudSolrClientFromZk;
import static com.commrogue.solrexback.reindexer.reactive.Reindex.*;
import static com.commrogue.solrexback.reindexer.web.models.ReindexSpecification.StageOrdering;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.common.web.jobmanager.Job;
import com.commrogue.solrexback.reindexer.reactive.models.ReindexState;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Valid
@Slf4j
@Getter
@Builder(setterPrefix = "with")
@AllArgsConstructor
public class ReindexJob implements Job {
    private final Function<String, CloudSolrClient> solrClientProvider;
    private final String timestampField;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final Collection srcCollection;
    private final Collection dstCollection;

    @Builder.Default
    private final int timeRangeSplitAmount = 1;

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

    private final List<Reindex> completedReindexes = new ArrayList<>();

    public static ReindexJobBuilder builder(Function<String, CloudSolrClient> solrClientProvider) {
        return new ReindexJobBuilder().withSolrClientProvider(solrClientProvider);
    }

    public static ReindexJob fromSpecification(
            ReindexSpecification reindexSpecification, Function<String, CloudSolrClient> solrClientProvider) {
        ReindexJobBuilder builder = ReindexJob.builder(solrClientProvider)
                .withTimestampField(reindexSpecification.getTimestampField())
                .withStartDate(reindexSpecification.getStartDate())
                .withEndDate(reindexSpecification.getEndDate())
                .withSrcCollection(reindexSpecification.getSrcCollection())
                .withDstCollection(reindexSpecification.getDstCollection())
                .withSrcDiRequestHandler(reindexSpecification.getSrcDiRequestHandler())
                .withDstDiRequestHandler(reindexSpecification.getDstDiRequestHandler())
                .withTimeRangeSplitAmount(reindexSpecification.getTimeRangeSplitAmount())
                .withStageOrdering(reindexSpecification.getStageOrdering())
                .withIsNatNetworking(reindexSpecification.getIsNatNetworking())
                .withCommit(reindexSpecification.getShouldCommit());

        if (reindexSpecification.getTimeRangeSplitAmount() != null) {
            builder.withTimeRangeSplitAmount(reindexSpecification.getTimeRangeSplitAmount());
        }

        return builder.build();
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

    public long getSumIndexed() {
        return Stream.concat(this.completedReindexes.stream(), Stream.of(this.currentReindex.get()))
                .map(Reindex::getReindexState)
                .mapToLong(ReindexState::getSumImportedDocuments)
                .sum();
    }

    @Override
    public Mono<String> start() {
        return Flux.<Reindex>generate(sink -> {
                    Reindex stage = this.getRemainingReindexes().poll();
                    this.currentReindex.set(stage);
                    if (stage == null) {
                        sink.complete();
                    } else {
                        sink.next(stage);
                    }
                })
                .concatMap(reindex -> reindex.getSubscribable().thenReturn(reindex))
                .doOnNext(completedReindex -> {
                    log.atInfo()
                            .addKeyValue("timestamp", completedReindex.getStartTime())
                            .setMessage("Reindex complete")
                            .log();

                    completedReindexes.add(completedReindex);
                })
                .then(Mono.just("Reindex job complete. \nTotal reindex stages: %s\nTotal documents imported: %s"
                        .formatted(this.completedReindexes.size(), this.getSumIndexed())));
    }
}
