package com.commrogue.solrexback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SolrexBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolrexBackApplication.class, args);
    }

}
