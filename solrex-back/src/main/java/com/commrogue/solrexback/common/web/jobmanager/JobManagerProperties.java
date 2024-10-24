/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.jobmanager;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job-manager")
@Data
public class JobManagerProperties {
    private int maxCachedJobs;
}
