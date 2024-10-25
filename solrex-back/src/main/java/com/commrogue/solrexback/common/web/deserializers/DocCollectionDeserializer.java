/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.solr.common.cloud.DocCollection;

public class DocCollectionDeserializer extends JsonDeserializer<DocCollection> {
    @Override
    public DocCollection deserialize(JsonParser p, DeserializationContext ctxt) {
        return null;
    }
}
