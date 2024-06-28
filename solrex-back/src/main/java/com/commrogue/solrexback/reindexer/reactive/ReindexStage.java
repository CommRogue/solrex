package com.commrogue.solrexback.reindexer.reactive;

import lombok.Data;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ReindexStage {
    private final LocalDateTime startTime;

    private final LocalDateTime endTime;

    private final ShardMapping shardMapping;


    public Mono<Integer> run() {
        return Mono.empty();
    }
}
