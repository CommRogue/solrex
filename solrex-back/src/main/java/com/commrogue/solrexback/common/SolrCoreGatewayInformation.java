package com.commrogue.solrexback.common;

import lombok.Data;

@Data
public class SolrCoreGatewayInformation {
    private final String internalAddress;
    private final String externalAddress;
    
    @Override
    public String toString() {
        return externalAddress; 
    }
}
