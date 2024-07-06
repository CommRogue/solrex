package com.commrogue.solrexback.reindexer;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReindexJobTest {
    @Mock
    Collection testCollection;

    @Test
    void getStages_unevenlyDivisible() {
        ReindexJob reindexJob = new ReindexJob(LocalDateTime.of(2024, 1, 1, 0, 0, 0), LocalDateTime.of(2024, 1, 2, 0, 0, 0), testCollection, testCollection, 5, "/dih", true);

        var s = reindexJob.generateStages();
        assertEquals(5, s.size());
    }
}