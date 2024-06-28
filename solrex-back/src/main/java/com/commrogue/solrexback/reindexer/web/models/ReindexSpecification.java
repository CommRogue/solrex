package com.commrogue.solrexback.reindexer.web.models;

import com.commrogue.solrexback.common.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Schema(description = "Reindex configuration")
@Data
public class ReindexSpecification {
    @NonNull
    private final Collection srcCollection;
    @NonNull
    private final Collection dstCollection;
    @NotNull
    private final LocalDateTime startDate;
    @NotNull
    private final LocalDateTime endDate;
    private final Integer stagingAmount;
}

