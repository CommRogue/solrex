package com.commrogue.solrexback.reindexer.models.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "Reindex configuration")
@Data
public class ReindexSpecification {
    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;
    private Integer stagingAmount;
}

