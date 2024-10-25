/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import static com.commrogue.solrexback.reindexer.reactive.Reindex.*;

import com.commrogue.solrexback.common.web.jobmanager.Job;
import com.commrogue.solrexback.reindexer.reactive.models.ReindexState;
import com.commrogue.solrexback.reindexer.reactive.sharding.ShardingStrategy;
import com.commrogue.solrexback.reindexer.web.models.ReindexStageSpecification;
import com.commrogue.solrexback.reindexer.web.models.StageOrdering;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Valid
@Slf4j
@Getter
@SuperBuilder(setterPrefix = "with")
@Jacksonized
public class ReindexJob extends BaseReindexJob implements Job {
    public abstract static class ReindexJobBuilder<C extends ReindexJob, B extends ReindexJob.ReindexJobBuilder<C, B>>
            extends BaseReindexJob.BaseReindexJobBuilder<C, B> {
        protected ReindexJobBuilder withReindexBuilderTemplate(ReindexBuilder reindexBuilderTemplate) {
            this.reindexBuilderTemplate = reindexBuilderTemplate;
            return this;
        }

        public ReindexJobBuilder withIsNatNetworking(boolean isNatNetworking) {
            this.reindexBuilderTemplate.withIsNatNetworking(isNatNetworking);
            return this;
        }

        public ReindexJobBuilder withSrcDiRequestHandler(String srcDiRequestHandler) {
            this.reindexBuilderTemplate.withSrcDiRequestHandler(srcDiRequestHandler);
            return this;
        }

        public ReindexJobBuilder withDstDiRequestHandler(String dstDiRequestHandler) {
            this.reindexBuilderTemplate.withDstDiRequestHandler(dstDiRequestHandler);
            return this;
        }

        public ReindexJobBuilder withGlobalCommit(boolean globalCommit) {
            this.reindexBuilderTemplate.withCommit(globalCommit);
            return this;
        }

        public ReindexJobBuilder withTimestampField(String timestampField) {
            this.reindexBuilderTemplate.withTimestampField(timestampField);
            return this;
        }

        public ReindexJobBuilder withShardingStrategy(ShardingStrategy shardingStrategy) {
            this.reindexBuilderTemplate.withShardingStrategy(shardingStrategy);
            return this;
        }
    }

    @NonNull @Schema(hidden = true)
    private final ReindexBuilder reindexBuilderTemplate;

    @Schema(defaultValue = "timestamp")
    private final String timestampField;

    @Builder.Default
    @JsonProperty("commit")
    private final boolean globalCommit = true;

    @Builder.Default
    @Schema(defaultValue = "1")
    private final int timeRangeSplitAmount = 1;

    @Schema(defaultValue = "/dih")
    private final String srcDiRequestHandler;

    @Schema(defaultValue = "/dataimport")
    private final String dstDiRequestHandler;

    @Singular
    @JsonProperty("stages")
    private final List<ReindexStageSpecification> reindexStageSpecifications;

    private final StageOrdering stageOrdering;

    @Schema(hidden = true)
    private final AtomicReference<Reindex> currentReindex = new AtomicReference<>();

    @Getter(lazy = true)
    @Schema(hidden = true)
    private final Queue<Reindex> remainingReindexes = generateReindexes();

    @Schema(hidden = true)
    private final List<Reindex> completedReindexes = new ArrayList<>();

    // TODO - absolute garbage code
    private Stream<Reindex> generateStages(ReindexBuilder baseReindexBuilder) {
        return reindexStageSpecifications.stream().map(stage -> {
            baseReindexBuilder.withFqs(stage.getFqs());
            if (stage.getShouldCommitOverride() != null) {
                baseReindexBuilder.withCommit(stage.getShouldCommitOverride());
            }

            Reindex reindex = baseReindexBuilder.build();
            baseReindexBuilder.clearFqs();
            baseReindexBuilder.withCommit(this.isGlobalCommit());

            return reindex;
        });
    }

    private Queue<Reindex> generateReindexes() {
        Duration stageDuration =
                Duration.between(this.getStartDate(), this.getEndDate()).dividedBy(this.getTimeRangeSplitAmount());

        return IntStream.range(0, this.getTimeRangeSplitAmount())
                .mapToObj(stageIndex -> {
                    reindexBuilderTemplate
                            .withStartTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex)))
                            .withEndTime(this.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1)))
                            .withCommit(this.isGlobalCommit());

                    if (this.getReindexStageSpecifications() == null
                            || !this.getReindexStageSpecifications().isEmpty()) {
                        return Stream.of(reindexBuilderTemplate.build());
                    }

                    return generateStages(reindexBuilderTemplate);
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

    @Schema(hidden = true)
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
                .then(Mono.defer(() ->
                        Mono.just(("Reindex job complete. \nTotal reindex stages: %s\nTotal documents imported: %s"
                                .formatted(this.completedReindexes.size(), this.getSumIndexed())))));
    }
}
