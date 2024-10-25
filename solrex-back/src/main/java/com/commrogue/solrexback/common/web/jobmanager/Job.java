/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import reactor.core.publisher.Mono;

public interface Job {
    Mono<String> start();

    default void cleanup() {}

    @Schema(hidden = true)
    default Optional<String> getTitle() {
        return Optional.empty();
    }

    @Schema(hidden = true)
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    @Schema(hidden = true)
    default Optional<String> getStateDescription() {
        return Optional.empty();
    }
}
