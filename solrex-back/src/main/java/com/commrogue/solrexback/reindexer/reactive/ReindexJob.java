package com.commrogue.solrexback.reindexer.reactive;


import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import io.ino.solrs.JavaAsyncSolrClient;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Slice;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.commrogue.solrexback.reindexer.reactive.Helpers.getCloudSolrClientFromZk;

@RequiredArgsConstructor
@Valid
public class ReindexJob {
    @NonNull
    private final ReindexSpecification reindexSpecification;
    @Getter
    private final AtomicReference<ReindexStage> currentStage = new AtomicReference<>();

    public ReindexJob(LocalDateTime startDate, LocalDateTime endDate, Collection srcCollection, Collection dstCollection, int stagingAmount) {
        this.reindexSpecification = new ReindexSpecification(srcCollection, dstCollection, startDate, endDate, stagingAmount);
    }

    @Getter(lazy = true)
    private final Queue<ReindexStage> stages = generateStages();

    @Getter(lazy = true)
    private final ShardMapping shardMapping = mapShards();

    public Queue<ReindexStage> generateStages() {
        Duration stageDuration = Duration.between(reindexSpecification.getStartDate(), reindexSpecification.getEndDate()).dividedBy(reindexSpecification.getStagingAmount());

        return IntStream.range(0, reindexSpecification.getStagingAmount()).mapToObj(
                        (stageIndex) -> new ReindexStage(reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex)),
                                reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1)), this.getShardMapping()))
                .collect(Collectors.toCollection(() -> new PriorityQueue<>(Comparator.comparing(ReindexStage::getStartTime))));
    }

    public ShardMapping mapShards() {
        // TODO - add exception handling for unknown collections
        // TODO - getClusterState() is blocking
        DocCollection sourceCollection = getCloudSolrClientFromZk(reindexSpecification.getSrcCollection().getZkConnectionString()).getClusterState().getCollection(reindexSpecification.getSrcCollection().getCollectionName());
        DocCollection destinationCollection = getCloudSolrClientFromZk(reindexSpecification.getDstCollection().getZkConnectionString()).getClusterState().getCollection(reindexSpecification.getSrcCollection().getCollectionName());

        return new ShardMapping(destinationCollection.getActiveSlices().stream().collect(Collectors.<Slice, JavaAsyncSolrClient, Map<JavaAsyncSolrClient, Integer>>toMap(
                (slice -> JavaAsyncSolrClient.create(slice.getLeader().getCoreUrl())),
                (slice -> Map.of(JavaAsyncSolrClient.create(sourceCollection.getSlice(slice.getName()).getLeader().getCoreUrl()), 0))
        )));
    }

    public Mono<Void> run() {
        return Flux.<ReindexStage>generate((sink) -> {
            ReindexStage stage = this.getStages().poll();
            this.currentStage.set(stage);
            if (stage == null) {
                this.currentStage.set(null);
                sink.complete();
            } else {
                sink.next(stage);
            }
        }).concatMap(ReindexStage::run).then();
    }
}
