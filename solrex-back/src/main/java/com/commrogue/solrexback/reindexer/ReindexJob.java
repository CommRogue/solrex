package com.commrogue.solrexback.reindexer;


import com.commrogue.solrexback.common.Collection;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Valid
public class ReindexJob {
    @NonNull
    private final LocalDateTime start;
    @NonNull
    private final LocalDateTime end;
    @NonNull
    private final Collection srcCollection;
    @NonNull
    private final Collection dstCollection;
    private final int numStages;

    @Getter(lazy = true)
    private final LinkedHashSet<ReindexStage> stages = generateStages();

    private LinkedHashSet<ReindexStage> generateStages() {
        Duration stageDuration = Duration.between(start, end).dividedBy(numStages);

        return IntStream.range(0, numStages).mapToObj(
                        (stageIndex) -> new ReindexStage(start.plus(stageDuration.multipliedBy(stageIndex)),
                                start.plus(stageDuration.multipliedBy(stageIndex + 1))))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
