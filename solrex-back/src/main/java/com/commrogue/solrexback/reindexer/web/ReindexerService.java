package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.common.jobmanager.JobManager;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReindexerService {
    private final JobManager jobManager;

    public UUID reindex(ReindexSpecification reindexSpecification) {
        UUID jobId = jobManager.registerJob(new ReindexJob(reindexSpecification));
        jobManager.getJob(jobId).start();

        return jobId;
    }
}
