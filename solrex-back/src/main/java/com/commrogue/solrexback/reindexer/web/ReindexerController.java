/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.common.web.jobmanager.JobManager;
import com.commrogue.solrexback.common.web.jobmanager.StatefulJob;
import com.commrogue.solrexback.reindexer.reactive.BaseReindexJob;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
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

    private final JobManager jobManager;
    /**
     * Simplified schema version of POST /reindex.
     *
     * @param reindexSpecification The simplified reindex specification containing details for the reindexing process.
     * @return A Mono containing the UUID of the reindex job.
     * @throws ResponseStatusException if the end date is not after the start date in the specification.
     */
    @Operation(
            operationId = "simplified_reindex",
            summary =
                    "Reindex from source to target collections. This is a simplified version of the full reindex endpoint, with only the required fields. ",
            tags = {"Reindex"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Started reindexing successfully",
                        content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)),
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request format",
                        content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)),
                        }),
            })
    @RequestMapping(method = RequestMethod.POST, value = "/simple")
    public Mono<String> simplifiedSchemaReindex(@RequestBody BaseReindexJob reindexSpecification) {
        return reindex(ReindexJob.builder()
                .withSrcCollection(reindexSpecification.getSrcCollection())
                .withDstCollection(reindexSpecification.getDstCollection())
                .withStartDate(reindexSpecification.getStartDate())
                .withEndDate(reindexSpecification.getEndDate())
                .build());
    }

    /**
     * Initiates reindexing from a source to a target collection based on the provided reindex specification.
     * Validates the start and end dates in the specification.
     * Returns a Mono containing the UUID of the reindex job.
     *
     * @param reindexJob The reindex specification containing details for the reindexing process.
     * @return A Mono containing the UUID of the reindex job.
     * @throws ResponseStatusException if the end date is not after the start date in the specification.
     */
    @Operation(
            operationId = "reindex",
            summary = "Reindex from source to target collections",
            tags = {"Reindex"},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Started reindexing successfully",
                        content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)),
                        }),
                @ApiResponse(
                        responseCode = "400",
                        description = "Bad request format",
                        content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)),
                        }),
            })
    @RequestMapping(method = RequestMethod.POST, value = "/")
    public Mono<String> reindex(@RequestBody ReindexJob reindexJob) {
        if ((reindexJob.getStartDate() != null && reindexJob.getEndDate() != null)
                && !reindexJob.getEndDate().isAfter(reindexJob.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified end date must be after start date");
        }

        UUID reindexUUID = jobManager.registerJob(reindexJob);
        jobManager.getJob(reindexUUID).ifPresent(StatefulJob::start);

        return Mono.just(reindexUUID.toString());
    }
}
