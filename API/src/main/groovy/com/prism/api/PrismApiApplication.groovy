//
// This is the main class for the Prism application.
//
package com.prism.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrismApiApplication {

   static void main(String[] args) {
      runApplication<PrismApiApplication>(*args)
   }

}
