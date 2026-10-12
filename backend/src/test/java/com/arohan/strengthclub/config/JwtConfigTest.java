package com.arohan.strengthclub.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {
    private final JwtConfig config = new JwtConfig();

    @Test
    void refusesTheShippedPlaceholderSecret() {
        assertThrows(IllegalStateException.class,
                () -> config.jwtSecretKey("replace-with-a-unique-random-secret-at-least-32-characters-long"));
    }

    @Test
    void refusesASecretShorterThanThirtyTwoBytes() {
        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("short-test-secret"));
    }

    @Test
    void acceptsAConfiguredSecretOfAtLeastThirtyTwoBytes() {
        assertDoesNotThrow(() -> config.jwtSecretKey("random-test-secret-for-unit-testing-0123456789"));
    }
}
