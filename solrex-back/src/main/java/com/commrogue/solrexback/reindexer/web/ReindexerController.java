/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.common.web.jobmanager.JobManager;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reindex")
public class ReindexerController {

    private final ReindexerService reindexerService;
    private final JobManager jobManager;

    @Operation(
        operationId = "reindex",
        summary = "Reindex from source to target collections",
        tags = { "Reindex" },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Started reindexing successfully",
                content = {
                    @Content(
                        mediaType = "text/plain",
                        schema = @Schema(implementation = String.class)
                    ),
                }
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Bad request format",
                content = {
                    @Content(
                        mediaType = "text/plain",
                        schema = @Schema(implementation = String.class)
                    ),
                }
            ),
        }
    )
    @RequestMapping(method = RequestMethod.POST, value = "/")
    public Mono<String> reindex(
        @RequestBody ReindexSpecification reindexSpecification
    ) {
        if (
            !reindexSpecification
                .getEndDate()
                .isAfter(reindexSpecification.getStartDate())
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Specified end date must be after start date"
            );
        }

        UUID reindexUUID = this.reindexerService.reindex(reindexSpecification);

        return Mono.just(reindexUUID.toString());
    }

    @Operation(tags = { "Reindex" })
    @RequestMapping(method = RequestMethod.GET, value = "/status/{uuid}")
    public Mono<String> checkStatus(@PathVariable String uuid) {
        return Mono.just(
            this.jobManager.getJob(UUID.fromString(uuid)).getState().toString()
        );
    }

    @Operation(tags = { "Reindex" })
    @RequestMapping(method = RequestMethod.GET, value = "/abort/{uuid}")
    public Mono<String> abort(@PathVariable String uuid) {
        this.jobManager.getJob(UUID.fromString(uuid)).terminate();

        return Mono.just("Aborted %s".formatted(uuid));
    }
}
