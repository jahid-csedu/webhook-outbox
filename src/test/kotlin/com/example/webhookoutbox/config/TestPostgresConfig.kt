package com.example.webhookoutbox.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@TestConfiguration
@Testcontainers
class TestPostgresConfig {

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:16.10").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }
}
