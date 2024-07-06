package com.commrogue.solrexback.reindexer.reactive;

import com.commrogue.solrexback.reindexer.exceptions.OngoingDataImportException;
import com.commrogue.solrexback.reindexer.reactive.models.ReindexState;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.Slice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder(setterPrefix = "with")
@Getter
@Slf4j
public class Reindex {
    private final String timestampField;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    @Singular
    private final List<String> fqs;
    private final ReindexState reindexState;
    private final boolean isNatNetworking;
    private String diRequestHandler;

    public static PostBuilder builder() {
        return new PostBuilder();
    }

    public Mono<Void> getSubscribable() {
        return Flux.fromIterable(reindexState.entrySet())
                .flatMap((entry) -> Flux.fromIterable(entry.getValue().entrySet()).concatMap(
                                (sourceEntry) -> DataImportRequest.builder(entry.getKey().getExternalAddress(),
                                                sourceEntry.getKey().getInternalAddress()).withFqs(fqs).build()
                                        .getSubscribable().doOnSubscribe((_) -> {
                                            log.info("Sub-Reindex for {} started"
                                                    , entry.getKey());
                                            sourceEntry.getValue().setStarted(LocalDateTime.now());
                                        }).doOnNext((progress) -> {
                                            log.info(
                                                    "Sub-Reindex progress for {} - {}", sourceEntry.getKey(), progress);
                                            sourceEntry.getValue().setIndexed(progress);
                                        })
                                        .doOnComplete(
                                                () -> {
                                                    log.info("Sub-Reindex for {} complete", sourceEntry.getKey());
                                                    sourceEntry.getValue().setFinished(LocalDateTime.now());
                                                })
                                        .doOnError((e) -> e instanceof OngoingDataImportException, (_) -> log.warn("A" +
                                                " reindex is already in progress for {}", sourceEntry.getKey())))
                        .doOnCancel(() -> log.info("Reindex cancelled for {}", entry.getKey()))
                        .doOnComplete(() -> log.info("Reindex complete for {}", entry.getKey()))).then();
    }

    public static class ReindexBuilder {
        // delete @Builder's generated withReindexState
        private ReindexBuilder withReindexState(ReindexState reindexState) {
            this.reindexState = reindexState;

            return this;
        }

        public ReindexBuilder withCustomSharding(Map<Slice, ? extends Set<Slice>> shardMapping) {
            // TODO - isNatNetworking not respected if comes after withCustomSharding in builder
            this.reindexState = ReindexState.fromSliceMapping(shardMapping, isNatNetworking);

            return this;
        }

        public ReindexBuilder withStartTime(LocalDateTime startTime) {
            this.startTime = startTime;

            return this.withFq("timestamp:[%s TO *]" .formatted(startTime));
        }

        public ReindexBuilder withEndTime(LocalDateTime endTime) {
            this.endTime = endTime;

            return this.withFq("timestamp:[* TO %s]" .formatted(endTime));
        }
    }

    public static class PostBuilder extends ReindexBuilder {
        @Override
        public Reindex build() {
            Reindex reindex = super.build();

            if (reindex.getTimestampField() == null &&
                    (reindex.getStartTime() != null || reindex.getEndTime() != null)) {
                throw new IllegalArgumentException("A start or end time has been specified for the reindex but no " +
                        "timestamp field was specified");
            }

            return reindex;
        }
    }
}
