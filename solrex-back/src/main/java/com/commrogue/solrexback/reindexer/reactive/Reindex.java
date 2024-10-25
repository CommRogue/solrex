/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import com.commrogue.solrexback.reindexer.exceptions.OngoingDataImportException;
import com.commrogue.solrexback.reindexer.reactive.models.ReindexState;
import com.commrogue.solrexback.reindexer.reactive.sharding.NonLinearAutomaticSharding;
import com.commrogue.solrexback.reindexer.reactive.sharding.ShardingStrategy;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.DocCollection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Builder(setterPrefix = "with")
@Getter
@Slf4j
public class Reindex {

    @Builder.Default
    private final String timestampField = "timestamp";

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    @Singular
    private final List<String> fqs;

    private final ReindexState reindexState;

    @Builder.Default
    private final String srcDiRequestHandler = "/dih";

    @Builder.Default
    private final String dstDiRequestHandler = "/dataimport";

    @Builder.Default
    private final boolean commit = true;

    @Builder.Default
    private final int rowsPerBatch = 2000;

    public static AspectBuilder builder() {
        return new AspectBuilder();
    }

    // TODO - either remove or find another way to implement this without having a parameterized AspectBuilder
    // constructor
    //    public static AspectBuilder builderWithCustomSharding(Map<Slice, ? extends Set<Slice>> shardMapping) {
    //        return new AspectBuilder(() -> shardMapping);
    //    }

    public Mono<Void> getSubscribable() {
        return Flux.fromIterable(reindexState.entrySet())
                .flatMap(entry -> Flux.fromIterable(entry.getValue().entrySet())
                        .concatMap(sourceEntry -> DataImportRequest.builder(
                                        entry.getKey().getExternalAddress(),
                                        sourceEntry.getKey().getInternalAddress())
                                .withFqs(fqs)
                                .withSrcDiRequestHandler(this.getSrcDiRequestHandler())
                                .withDstDiRequestHandler(this.getDstDiRequestHandler())
                                .withCommit(this.isCommit())
                                .build()
                                .getSubscribable()
                                .doOnSubscribe(e -> {
                                    log.atDebug()
                                            .addKeyValue("targets", entry.getKey())
                                            .setMessage("Sub-Reindex started")
                                            .log();

                                    sourceEntry.getValue().setStarted(LocalDateTime.now());
                                })
                                .doOnNext(progress -> {
                                    log.atDebug()
                                            .addKeyValue("targets", sourceEntry.getKey())
                                            .addKeyValue("progress", progress)
                                            .setMessage("Sub-Reindex progress")
                                            .log();
                                    sourceEntry.getValue().setIndexed(progress);
                                })
                                .doOnComplete(() -> {
                                    log.atDebug()
                                            .addKeyValue("targets", sourceEntry.getKey())
                                            .setMessage("Sub-Reindex complete")
                                            .log();
                                    sourceEntry.getValue().setFinished(LocalDateTime.now());
                                })
                                .doOnError(
                                        e -> e instanceof OngoingDataImportException,
                                        e -> log.warn("A reindex is already in progress for {}", sourceEntry.getKey())))
                        .doOnCancel(() -> log.info("Reindex cancelled for {}", entry.getKey()))
                        .doOnComplete(() -> log.info("Reindex complete for {}", entry.getKey())))
                .then();
    }

    public static class ReindexBuilder {

        protected boolean isNatNetworking = false;
        protected DocCollection srcCollection;
        protected DocCollection dstCollection;
        protected ShardingStrategy shardingStrategy = NonLinearAutomaticSharding::getShardMapping;

        // TODO - necessary?
        // protect @Builder's withReindexState, as it should not be used outside of Builder class
        protected ReindexBuilder withReindexState(ReindexState reindexState) {
            return null;
        }

        public ReindexBuilder withIsNatNetworking(boolean isNatNetworking) {
            this.isNatNetworking = isNatNetworking;

            return this;
        }

        public ReindexBuilder withSrcCollection(DocCollection srcCollection) {
            this.srcCollection = srcCollection;
            return this;
        }

        public ReindexBuilder withDstCollection(DocCollection dstCollection) {
            this.dstCollection = dstCollection;
            return this;
        }

        public ReindexBuilder withShardingStrategy(ShardingStrategy shardingStrategy) {
            this.shardingStrategy = shardingStrategy;
            return this;
        }
    }

    // TODO - possibly implement with staged builder pattern.
    // TODO - what the fuck do I call this builder? it's not PostBuilder anymore because it is also
    // a PreBuilder...
    public static class AspectBuilder extends ReindexBuilder {
        @Override
        public Reindex build() {
            this.withReindexState(ReindexState.fromSliceMapping(
                    this.shardingStrategy.getShardMapping(this.srcCollection, dstCollection), this.isNatNetworking));

            Reindex reindex = super.build();

            if (reindex.getStartTime() != null) {
                if (reindex.getTimestampField() == null) {
                    throw new IllegalArgumentException(
                            "A start time as been specified for the reindex but no timestamp field was specified");
                } else if (reindex.getEndTime() != null) {
                    reindex.getFqs()
                            .add("timestamp:[%s TO %s]".formatted(reindex.getStartTime(), reindex.getEndTime()));
                } else {
                    reindex.getFqs().add("timestamp:[%s TO *]".formatted(reindex.getStartTime()));
                }
            } else if (reindex.getEndTime() != null) {
                if (reindex.getTimestampField() == null) {
                    throw new IllegalArgumentException(
                            "An end time has been specified for the reindex but no timestamp field was specified");
                } else {
                    reindex.getFqs().add("timestamp:[* TO %s]".formatted(reindex.getEndTime()));
                }
            }

            return reindex;
        }
    }
}
