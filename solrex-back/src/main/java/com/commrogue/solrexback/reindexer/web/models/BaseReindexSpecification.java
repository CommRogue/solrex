/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import com.commrogue.solrexback.common.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Schema(description = "Simplified Reindex configuration")
@SuperBuilder(setterPrefix = "with")
@Getter
public class BaseReindexSpecification {
    @NonNull private final Collection srcCollection;

    @NonNull private final Collection dstCollection;

    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
}
