/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.common.web.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocConfiguration {

    // TODO - change contact information
    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Solrex API")
                        .description(
                                "Solrex is an administration toolkit and interface for managing large-scale Apache Solr™ SolrCloud clusters.")
                        .contact(new Contact().email("apiteam@swagger.io"))
                        .license(
                                new License().name("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html"))
                        .version("1.0.0"));
    }
}
