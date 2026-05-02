package com.trojanmarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TrojanMarketApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test: verifies the Spring Boot application context boots with the
        // test profile (H2 + JPA-managed schema, Flyway disabled, mocked email).
    }
}
