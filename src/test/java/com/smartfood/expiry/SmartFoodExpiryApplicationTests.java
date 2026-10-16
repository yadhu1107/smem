package com.smartfood.expiry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmartFoodExpiryApplicationTests {

    @Test
    void contextLoads() {
        // Verifies complete Spring Boot application context, JPA beans, and security filter chain load successfully
    }
}
