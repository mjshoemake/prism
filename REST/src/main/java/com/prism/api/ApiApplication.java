package com.prism.api;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.prism.common.logging.LogCategory;

@SpringBootApplication
public class ApiApplication {

    private static final Logger log = LogCategory.APPLICATION.logger(ApiApplication.class);

    public static void main(String[] args) {
        log.info("Starting....");
        SpringApplication.run(ApiApplication.class, args);
        log.info("Starting.... done.");
    }
}
