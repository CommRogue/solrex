/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class ReindexStageSpecification {
    @Singular
    private final List<String> fqs;

    private final Boolean shouldCommitOverride;
    private final Integer rowsPerBatchOverride;
}
