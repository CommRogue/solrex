/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import com.commrogue.solrexback.common.Collection;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Schema(description = "Reindex configuration")
@Builder(setterPrefix = "with")
@Data
public class ReindexSpecification {
    @RequiredArgsConstructor
    @Getter(onMethod_ = @__(@JsonValue))
    public enum StageOrdering {
        PRIORITIZE_STAGES("stages"),
        PRIORITIZE_TIME_SPLITS("timeSplits");
        private final String alias;
    }

    @NonNull private final Collection srcCollection;

    @NonNull private final Collection dstCollection;

    private final String timestampField;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final Integer timeRangeSplitAmount;
    private final String srcDiRequestHandler;
    private final String dstDiRequestHandler;
    private final Boolean isNatNetworking;
    private final Boolean shouldCommit;
    private final List<ReindexStageSpecification> stages;
    private final StageOrdering stageOrdering;
}
