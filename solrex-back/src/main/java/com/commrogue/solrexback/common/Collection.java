package com.commrogue.solrexback.common;

import lombok.Data;

@Data
public class Collection {
    private final String zkConnectionString;
    private final String collectionName;
}
