# setup.ps1 — run from C:\Work\prism\API with:
#   powershell -ExecutionPolicy Bypass -File .\setup.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $root

# 1. Create directory structure
$dirs = @(
  "src\main\groovy\com\prism\api\domain",
  "src\main\groovy\com\prism\api\repository",
  "src\main\groovy\com\prism\api\service",
  "src\main\groovy\com\prism\api\web",
  "src\main\resources"
)
foreach ($d in $dirs) { New-Item -ItemType Directory -Path $d -Force | Out-Null }

# 2. pom.xml
@"
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.prism.api</groupId>
  <artifactId>prism-api</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>jar</packaging>

  <properties>
    <java.version>17</java.version>
    <groovy.version>4.0.14</groovy.version>
    <spring.boot.version>3.1.0</spring.boot.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <version>\${spring.boot.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
      <version>\${spring.boot.version}</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.6.0</version>
    </dependency>
    <dependency>
      <groupId>org.codehaus.groovy</groupId>
      <artifactId>groovy</artifactId>
      <version>\${groovy.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>\${spring.boot.version}</version>
      </plugin>
      <plugin>
        <groupId>org.codehaus.gmavenplus</groupId>
        <artifactId>gmavenplus-plugin</artifactId>
        <version>1.16.0</version>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>compileTests</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
"@ | Out-File -FilePath "pom.xml" -Encoding utf8

# 3. application.properties
@"
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=none
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
"@ | Out-File -FilePath "src\main\resources\application.properties" -Encoding utf8

# 4. Domain — Person.groovy
@"
package com.prism.api.domain

import jakarta.persistence.*

@Entity
@Table(name = \"person\")
class Person {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = \"first_name\")
    String firstName

    @Column(name = \"last_name\")
    String lastName
}
"@ | Out-File -FilePath "src\main\groovy\com\prism\api\domain\Person.groovy" -Encoding utf8

# 5. Repository — PersonRepository.groovy
@"
package com.prism.api.repository

import com.prism.api.domain.Person
import org.springframework.data.jpa.repository.JpaRepository

interface PersonRepository extends JpaRepository<Person, Long> { }
"@ | Out-File -FilePath "src\main\groovy\com\prism\api\repository\PersonRepository.groovy" -Encoding utf8

# 6. Service — PersonService.groovy
@"
package com.prism.api.service

import com.prism.api.domain.Person
import com.prism.api.repository.PersonRepository
import org.springframework.stereotype.Service

@Service
class PersonService {
    private final PersonRepository repo

    PersonService(PersonRepository repo) {
        this.repo = repo
    }

    List<Person> findAll() {
        repo.findAll()
    }

    Person findById(Long id) {
        repo.findById(id).orElseThrow { new RuntimeException(\"Person not found\") }
    }
}
"@ | Out-File -FilePath "src\main\groovy\com\prism\api\service\PersonService.groovy" -Encoding utf8

# 7. Controller — PersonController.groovy
@"
package com.prism.api.web

import com.prism.api.domain.Person
import com.prism.api.service.PersonService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(\"/api/people\")
class PersonController {
    private final PersonService service

    PersonController(PersonService service) {
        this.service = service
    }

    @GetMapping
    List<Person> getAll() {
        service.findAll()
    }

    @GetMapping(\"/{id}\")
    Person getOne(@PathVariable Long id) {
        service.findById(id)
    }
}
"@ | Out-File -FilePath "src\main\groovy\com\prism\api\web\PersonController.groovy" -Encoding utf8

# 8. Application Entry — PrismApiApplication.groovy
@"
package com.prism.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrismApiApplication

static void main(String[] args) {
    runApplication<PrismApiApplication>(*args)
}
"@ | Out-File -FilePath "src\main\groovy\com\prism\api\PrismApiApplication.groovy" -Encoding utf8

Write-Host "Scaffold complete. Run 'mvn clean package' and then 'mvn spring-boot:run'."