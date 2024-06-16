package com.commrogue.solrexback.reindexer;


import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.models.web.ReindexSpecification;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Valid
public class ReindexJob {
    @NonNull
    private final ReindexSpecification reindexSpecification;
    private final AtomicInteger currentStageProgress = new AtomicInteger(0);

    @Getter(lazy = true)
    private final Queue<ReindexStage> stages = generateStages();

    private Queue<ReindexStage> generateStages() {
        Duration stageDuration = Duration.between(reindexSpecification.getStartDate(), reindexSpecification.getEndDate()).dividedBy(reindexSpecification.getStagingAmount());

        return IntStream.range(0, reindexSpecification.getStagingAmount()).mapToObj(
                        (stageIndex) -> new ReindexStage(reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex)),
                                reindexSpecification.getStartDate().plus(stageDuration.multipliedBy(stageIndex + 1))))
                .collect(Collectors.toCollection(PriorityQueue::new));
    }

    public Mono<Void> run() {
        return Flux.<ReindexStage>generate((sink) -> {
            ReindexStage stage = this.getStages().poll();
            if (stage == null) {
                sink.complete();
            } else {
                sink.next(stage);
            }
        }).concatMap(stage -> stage.run(this)).then();
    }
}
