/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface StatefulJob {
    State getState();

    void terminate();

    void start();

    default Optional<String> getTitle() {
        return Optional.empty();
    }

    default Optional<String> getDescription() {
        return Optional.empty();
    }

    default Optional<String> getStateDescription() {
        return Optional.empty();
    }

    default Optional<String> getSummary() {
        String summary = Stream.of(
            Optional.of(getState()).map(s -> "State: " + s.name()),
            getTitle().map(t -> "Title: " + t),
            getDescription().map(d -> "Description: " + d),
            getStateDescription().map(sd -> "State description: " + sd)
        )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.joining("\n"));

        return !summary.isBlank() ? Optional.empty() : Optional.of(summary);
    }

    enum State {
        RUNNING,
        FINISHED,
        TERMINATED,
        AWAITING,
    }
}
