/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.common.web.jobmanager.JobManager;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReindexerService {
    private final Function<String, CloudSolrClient> cloudSolrClientFactory;

    private final JobManager jobManager;

    public UUID reindex(ReindexSpecification reindexSpecification) {
        UUID jobId = jobManager.registerJob(new ReindexJob(reindexSpecification, cloudSolrClientFactory));
        jobManager.getJob(jobId).start();

        return jobId;
    }
}
