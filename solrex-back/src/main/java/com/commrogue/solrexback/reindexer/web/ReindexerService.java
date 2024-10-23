/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.common.web.jobmanager.JobManager;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReindexerService {

    private final JobManager jobManager;

    public UUID reindex(ReindexSpecification reindexSpecification) {
        UUID jobId = jobManager.registerJob(
            new ReindexJob(reindexSpecification)
        );
        jobManager.getJob(jobId).start();

        return jobId;
    }
}
