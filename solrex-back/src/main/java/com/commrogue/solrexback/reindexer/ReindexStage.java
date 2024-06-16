package com.commrogue.solrexback.reindexer;

import com.commrogue.solrexback.common.jobmanager.StatefulJob;
import lombok.Data;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class ReindexStage {
    private final LocalDateTime startTime;;
    private final LocalDateTime endTime;

    public Mono<Integer> run(ReindexJob ownerJob) {
        return Mono.just(1).doOnNext((x) -> System.out.println("Before delay element " + x)).delayElement(Duration.of(1, ChronoUnit.SECONDS)).doOnNext((x) -> System.out.println("After delay element " + x));
    }
}
