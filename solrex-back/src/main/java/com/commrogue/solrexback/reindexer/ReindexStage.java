package com.commrogue.solrexback.reindexer;

import com.commrogue.solrexback.common.jobmanager.StatefulJob;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReindexStage {
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;

}
