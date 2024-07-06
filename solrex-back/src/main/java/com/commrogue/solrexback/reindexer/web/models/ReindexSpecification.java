package com.commrogue.solrexback.reindexer.web.models;

import com.commrogue.solrexback.common.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Schema(description = "Reindex configuration")
@Builder(setterPrefix = "with")
@Data
public class ReindexSpecification {
    @NonNull
    private Collection srcCollection;
    @NonNull
    private Collection dstCollection;
    private String timestampField;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer stagingAmount;
    private String diRequestHandler;
    private boolean isNatNetworking;
}

