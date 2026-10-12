package com.arohan.strengthclub.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {
    @Test
    void blocksAfterRepeatedFailuresAndResetsOnSuccess() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 7; i++) attempts.recordFailure("Member@Example.com");
        assertFalse(attempts.isBlocked("member@example.com"));

        attempts.recordFailure("member@example.com");
        assertTrue(attempts.isBlocked("MEMBER@example.com"));

        attempts.recordSuccess("member@example.com");
        assertFalse(attempts.isBlocked("member@example.com"));
    }

    @Test
    void otherAccountsAreNotAffected() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 8; i++) attempts.recordFailure("a@example.com");
        assertTrue(attempts.isBlocked("a@example.com"));
        assertFalse(attempts.isBlocked("b@example.com"));
    }
}
