package com.commrogue.solrexback.reindexer.models.web;

import com.commrogue.solrexback.reindexer.ReindexJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reindex")
public class ReindexerController {
    private final ReindexerService reindexerService;

    @Operation(
            operationId = "reindex",
            summary = "Reindex from source to target collections",
            tags = {"Reindex"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Started reindexing successfully", content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
                    }),
                    @ApiResponse(responseCode = "400", description = "Bad request format", content = {
                            @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))
                    })
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/",
            produces = {"text/plain"},
            consumes = {"application/json", "application/xml", "application/x-www-form-urlencoded"}
    )
    public Mono<String> reindex(@RequestBody @Valid ReindexSpecification reindexSpecification) {
        if (!reindexSpecification.getEndDate().isAfter(reindexSpecification.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specified end date must be after start date");
        }

        ReindexJob reindexJob = reindexerService.reindex(reindexSpecification);
        reindexJob.run().subscribeOn(Schedulers.boundedElastic()).subscribe();

        return Mono.just("sd");
    }
}
