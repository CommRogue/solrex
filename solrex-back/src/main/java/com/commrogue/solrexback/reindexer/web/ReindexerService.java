package com.commrogue.solrexback.reindexer.web;

import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import com.commrogue.solrexback.reindexer.web.models.ReindexSpecification;
import org.springframework.stereotype.Service;

@Service
public class ReindexerService {
    public ReindexJob reindex(ReindexSpecification reindexSpecification) {
        return new ReindexJob(reindexSpecification);
    }

}
