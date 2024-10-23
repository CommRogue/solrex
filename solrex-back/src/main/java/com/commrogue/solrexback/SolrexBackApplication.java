/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SolrexBackApplication {

    public static void main(String[] args) {
        //        SpringApplication.run(SolrexBackApplication.class, args);
        System.out.println(
            Stream.<String>empty().collect(Collectors.joining())
        );
    }
}
