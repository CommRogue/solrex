/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@JsonSubTypes(
    {
//        @JsonSubTypes.Type(Collection.ExplicitTargetCollection.class),
        @JsonSubTypes.Type(Collection.ImplicitTargetCollection.class),
    }
)
public class Collection {

    private final String collectionName;
    private final String zkConnectionString;

//    public static class ExplicitTargetCollection extends Collection {
//
//        public ExplicitTargetCollection(
//            String collectionName,
//            String zkConnectionString
//        ) {
//            super(collectionName);
//            this.zkConnectionString = zkConnectionString;
//        }
//    }

    public static class ImplicitTargetCollection extends Collection {

        private final String env;

        public ImplicitTargetCollection(
            String collectionName,
            String env
        ) {
            super(collectionName);
            this.env = envAlias;
        }
    }
}
