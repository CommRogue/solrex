package com.commrogue.solrexback.reindexer.reactive;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder(setterPrefix = "with")
@Getter
@Slf4j
public class Reindex {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private String diRequestHandler;
    @Singular
    private final List<String> fqs;
    private final ReindexState reindexState;
    private final boolean isNatNetworking;

    public Mono<Void> getSubscribable() {
        return Flux.fromIterable(reindexState.entrySet())
                .flatMap((entry) -> Flux.fromIterable(entry.getValue().entrySet()).concatMap(
                        (sourceEntry) -> DataImportRequest.builder(entry.getKey(),
                                        sourceEntry.getKey()).withFqs(fqs).build()
                                .getSubscribable().doOnNext((progress) -> log.info(
                                        "Sub-Reindex progress for {} - {}", sourceEntry.getKey(), progress))
                                .doOnComplete(
                                        () -> log.info("Sub-Reindex for {} complete", sourceEntry.getKey()))).doOnComplete(() -> log.info("Reindex complete for {}", entry.getKey()))).then();


//        return this.shardMapping.entrySet().stream().reduce(Flux.<Void>empty(), (stageMono, entry) ->
//                stageMono.mergeWith(
//                        entry.getValue().entrySet().stream().reduce(Flux.<Void>empty(), (destShardMono, sourceEntry) ->
//                                destShardMono.concatWith(Mono.defer(
//                                                () -> Mono.fromCompletionStage(entry.getKey().query(dataImportRequest)))
//                                        .then(observeShardReindex(Mono.defer(() -> Mono.fromCompletionStage(
//                                                entry.getKey().query(dataImportRequest)))).doOnNext(
//                                                sourceEntry::setValue).then())
//                                ), Flux::concatWith).then()
//                ), Flux::mergeWith).then();
//    }
    }

    public static class ReindexBuilder {
        // delete @Builder's generated withReindexState
        private ReindexBuilder withReindexState(ReindexState reindexState) {
            this.reindexState = reindexState;

            return this;
        }

        private String getLeaderUrlForShardSlice(Slice shardSlice) {
            var coreUrl = shardSlice.getLeader().getCoreUrl();

            return isNatNetworking ? coreUrl.replaceFirst("192\\.168\\.\\d+\\.\\d+", "localhost") : coreUrl;
        }

        public ReindexBuilder withLinearSharding(DocCollection sourceCollection,
                                                 DocCollection destinationCollection) {
            this.reindexState = new ReindexState(destinationCollection.getActiveSlices().stream().collect(
                    Collectors.toMap(
                            (this::getLeaderUrlForShardSlice),
                            (slice -> Map.of(getLeaderUrlForShardSlice(sourceCollection.getSlice(slice.getName())), 0))
                    )));

            return this;
        }
    }
}
