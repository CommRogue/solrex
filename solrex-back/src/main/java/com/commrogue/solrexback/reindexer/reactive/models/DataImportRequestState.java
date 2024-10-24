/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.models;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DataImportRequestState {
    LocalDateTime started;
    LocalDateTime finished;
    long indexed;

    public Double getIndexingRate() {
        return (indexed
                        / (double) Duration.between(started, finished != null ? finished : LocalDateTime.now())
                                .toMillis())
                * 1000;
    }
}
