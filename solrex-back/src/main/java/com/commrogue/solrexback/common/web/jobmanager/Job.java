/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import java.util.Optional;
import reactor.core.publisher.Mono;

public interface Job {
    Mono<String> start();

    default void cleanup() {}

    default Optional<String> getTitle() {
        return Optional.empty();
    }

    default Optional<String> getDescription() {
        return Optional.empty();
    }

    default Optional<String> getStateDescription() {
        return Optional.empty();
    }
}
