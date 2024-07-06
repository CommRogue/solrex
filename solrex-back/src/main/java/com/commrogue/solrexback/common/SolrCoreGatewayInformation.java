package com.commrogue.solrexback.common;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames = false, exclude = {"internalAddress"})
public class SolrCoreGatewayInformation {
    private final String internalAddress;
    private final String externalAddress;
}
