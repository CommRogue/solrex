/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class ReindexStageSpecification {
    @Singular
    private final List<String> fqs;
    // TODO - we used boxed types here to allow for null values, and therefore be able to check if the value was set.
    // Lombok's developers instead used two primitive values - param$value, and param$set - to check if the value was
    // set.
    // What is the better approach here?
    private final Boolean shouldCommitOverride;

    @Schema(example = "2000")
    private final Integer rowsPerBatchOverride;
}
