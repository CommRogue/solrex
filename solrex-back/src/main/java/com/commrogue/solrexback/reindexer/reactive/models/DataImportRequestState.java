package com.commrogue.solrexback.reindexer.reactive.models;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class DataImportRequestState {
    LocalDateTime started;
    LocalDateTime finished;
    long indexed;

    public Double getIndexingRate() {
        return (indexed / (double)Duration.between(started, finished != null ? finished : LocalDateTime.now()).toMillis()) * 1000;
    }
}
