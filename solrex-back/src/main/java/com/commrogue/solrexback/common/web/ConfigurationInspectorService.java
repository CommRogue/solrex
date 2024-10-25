/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web;

import com.commrogue.solrexback.common.properties.SolrProperties;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/configuration")
public class ConfigurationInspectorService {
    private final SolrProperties solrProperties;

    @Operation(
            operationId = "get_envs",
            summary = "Get the available envionment aliases.",
            tags = {"Configuration"})
    @RequestMapping(method = RequestMethod.GET, value = "/envs")
    public Mono<Map<String, String>> getEnvAlises() {
        return Mono.just(this.solrProperties.getEnvs());
    }
}
