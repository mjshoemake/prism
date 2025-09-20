package com.prism.api

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ApiApplication {

	static void main(String[] args) {
		println "Starting...."
		SpringApplication.run(ApiApplication, args)
		println "Starting.... done."
	}

}
