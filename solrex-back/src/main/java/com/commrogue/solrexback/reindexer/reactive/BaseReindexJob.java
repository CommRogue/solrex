/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive;

import static com.commrogue.solrexback.common.web.deserializers.DocCollectionDeserializerComponent.*;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.apache.solr.common.cloud.DocCollection;

@Schema(description = "Simplified Reindex configuration")
@SuperBuilder(setterPrefix = "with")
@RequiredArgsConstructor
@Getter
@Jacksonized
public class BaseReindexJob {
    @NonNull @JsonDeserialize(using = DocCollectionDeserializer.class)
    @Schema(example = "{\"env\":\"env9\", \"collection\":\"qualifiedName\"}")
    private final DocCollection srcCollection;

    @NonNull @JsonDeserialize(using = DocCollectionDeserializer.class)
    @Schema(example = "{\"env\":\"env9\", \"collection\":\"qualifiedName\"}")
    private final DocCollection dstCollection;

    @Schema(example = "2024-01-01T00:00:00Z")
    private final LocalDateTime startDate;

    @Schema(example = "2024-01-01T00:00:00Z")
    private final LocalDateTime endDate;
}
