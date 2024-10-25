/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@RestController("/jobs")
public class JobManager {
    private final JobManagerProperties jobManagerProperties;

    private Map<UUID, StatefulJob> jobs;

    @PostConstruct
    public void init() {
        this.jobs = Caffeine.newBuilder()
                .maximumSize(jobManagerProperties.getMaxCachedJobs())
                .<UUID, StatefulJob>build()
                .asMap();
    }

    public UUID registerJob(Job job) {
        UUID uuid = UUID.randomUUID();
        this.jobs.put(uuid, new StatefulJob(job));

        return uuid;
    }

    private StatefulJob getJobByString(String uuidString) {
        return Optional.ofNullable(this.jobs.get(UUID.fromString(uuidString)))
                .orElseThrow(() -> new IllegalArgumentException("No job with UUID %s found.".formatted(uuidString)));
    }

    public Optional<StatefulJob> getJob(UUID uuid) {
        return Optional.ofNullable(jobs.get(uuid));
    }

    /**
     * Retrieves the status of a job.
     *
     * @param uuid The UUID of the reindex job to check the status for.
     * @return A Mono containing the string representation of the current state of the reindex job.
     */
    @Operation(tags = {"Jobs"})
    @RequestMapping(method = RequestMethod.GET, value = "/status/{uuid}")
    public Mono<String> checkStatus(@PathVariable String uuid) {
        return Mono.just(getJobByString(uuid).getSummary().orElse("No job summary available."));
    }

    /**
     * Aborts the reindex job identified by the provided UUID.
     *
     * @param uuid The UUID of the reindex job to abort.
     * @return A Mono containing a message indicating the job has been aborted.
     */
    @Operation(tags = {"Jobs"})
    @RequestMapping(method = RequestMethod.GET, value = "/abort/{uuid}")
    public Mono<String> abort(@PathVariable String uuid, @RequestParam String reason) {
        StatefulJob job = getJobByString(uuid);

        if (reason != null) {
            job.stop(reason);
        } else {
            job.stop();
        }

        return Mono.just("Aborted %s".formatted(uuid));
    }

    @Operation(tags = {"Jobs"})
    @RequestMapping(method = RequestMethod.GET, value = "/all")
    public Mono<String> getAll() {
        return Mono.just(
                this.jobs.isEmpty()
                        ? "No jobs are currently running"
                        : this.jobs.values().stream()
                                .map(job -> job.getSummary().orElse("No job summary available."))
                                .collect(Collectors.joining("\n")));
    }
}
