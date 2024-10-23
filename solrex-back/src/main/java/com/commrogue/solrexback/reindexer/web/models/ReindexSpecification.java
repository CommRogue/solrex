/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import com.commrogue.solrexback.common.Collection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Schema(description = "Reindex configuration", example = "")
@Builder(setterPrefix = "with")
@Data
public class ReindexSpecification {

    @NonNull private Collection srcCollection;

    @NonNull private Collection dstCollection;

    private String timestampField;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer stagingAmount;
    private String srcDiRequestHandler;
    private String dstDiRequestHandler;
    private Boolean isNatNetworking;
    private Boolean shouldCommit;
}
