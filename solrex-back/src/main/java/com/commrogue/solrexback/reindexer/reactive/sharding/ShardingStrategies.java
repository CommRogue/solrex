/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer.reactive.sharding;
// TODO - do this dynamically without enum

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShardingStrategies {
    LINEAR(LinearSharding::getShardMapping),
    BALANCED(NonLinearAutomaticSharding::getShardMapping);

    private final ShardingStrategy shardingStrategy;
}
