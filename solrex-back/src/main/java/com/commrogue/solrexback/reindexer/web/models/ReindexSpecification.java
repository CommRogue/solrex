/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Schema(description = "Reindex configuration")
@SuperBuilder(setterPrefix = "with")
@Getter
public class ReindexSpecification extends BaseReindexSpecification {
    @RequiredArgsConstructor
    @Getter(onMethod_ = @__(@JsonValue))
    public enum StageOrdering {
        PRIORITIZE_STAGES("stages"),
        PRIORITIZE_TIME_SPLITS("timeSplits");
        private final String alias;
    }

    @Schema(
            description =
                    "Target timestamp field in imported documents. Required if startDate or endDate are specified.")
    private final String timestampField;

    private final Integer timeRangeSplitAmount;
    private final String srcDiRequestHandler;
    private final String dstDiRequestHandler;
    private final Boolean isNatNetworking;
    private final Boolean shouldCommit;
    private final List<ReindexStageSpecification> stages;
    private final StageOrdering stageOrdering;
}
