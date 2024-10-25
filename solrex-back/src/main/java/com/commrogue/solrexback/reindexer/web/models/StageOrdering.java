/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.web.models;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter(onMethod_ = @__(@JsonValue))
public enum StageOrdering {
    PRIORITIZE_STAGES("stages"),
    PRIORITIZE_TIME_SPLITS("timeSplits");
    private final String alias;
}
