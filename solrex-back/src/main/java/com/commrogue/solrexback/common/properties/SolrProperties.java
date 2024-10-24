/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.properties;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solr")
@Data
public class SolrProperties {
    private Map<String, String> envs;
}
