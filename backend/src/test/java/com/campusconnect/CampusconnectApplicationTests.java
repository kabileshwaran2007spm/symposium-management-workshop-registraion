package com.campusconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the application context starts: security wiring, repositories and the
 * index configuration all resolve.
 */
@SpringBootTest
@ActiveProfiles("test")
class CampusconnectApplicationTests {

    @Test
    void contextLoads() {
        // Fails the build if any bean cannot be created.
    }
}
