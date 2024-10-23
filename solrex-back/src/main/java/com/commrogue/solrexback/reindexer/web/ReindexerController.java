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
    /**
     * Initiates reindexing from a source to a target collection based on the provided reindex specification.
     * Validates the start and end dates in the specification.
     * Returns a Mono containing the UUID of the reindex job.
     *
     * @param reindexSpecification The reindex specification containing details for the reindexing process.
     * @return A Mono containing the UUID of the reindex job.
     * @throws ResponseStatusException if the end date is not after the start date in the specification.
     */
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

    /**
     * Retrieves the status of a reindex job identified by the provided UUID.
     *
     * @param uuid The UUID of the reindex job to check the status for.
     * @return A Mono containing the string representation of the current state of the reindex job.
     */
    @Operation(tags = { "Reindex" })
    @RequestMapping(method = RequestMethod.GET, value = "/status/{uuid}")
    public Mono<String> checkStatus(@PathVariable String uuid) {
        return Mono.just(
            this.jobManager.getJob(UUID.fromString(uuid)).getState().toString()
        );
    }

    /*
     * Endpoint to initiate reindexing from a source to a target collection based on the provided reindex specification.
     * Validates the start and end dates in the specification.
     * Returns a Mono containing the UUID of the reindex job.
     *
     * @param reindexSpecification The reindex specification containing details for the reindexing process.
     * @return A Mono containing the UUID of the reindex job.
     * @throws ResponseStatusException if the end date is not after the start date in the specification.
     */
    @Operation(tags = { "Reindex" })
    @RequestMapping(method = RequestMethod.GET, value = "/abort/{uuid}")
    public Mono<String> abort(@PathVariable String uuid) {
        this.jobManager.getJob(UUID.fromString(uuid)).terminate();

        return Mono.just("Aborted %s".formatted(uuid));
    }
}
