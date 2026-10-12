package com.arohan.strengthclub.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers signed-out tokens (by id) until they would have expired anyway. Per server instance. */
@Service
public class RevokedTokenService {
    private final ConcurrentHashMap<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank()) return;
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        revoked.put(tokenId, expiresAt == null ? now.plusSeconds(86_400) : expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        if (tokenId == null) return false;
        Instant expiresAt = revoked.get(tokenId);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }
}
