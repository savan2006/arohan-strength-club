package com.arohan.strengthclub.security;

import com.arohan.strengthclub.entity.enums.Role;
import com.arohan.strengthclub.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Current role of an account, cached for a few seconds.
 *
 * <p>The JWT carries a role claim, but trusting it alone would let a demoted or deleted account keep its
 * old powers until the token expires. Every request checks the live role through this cache instead
 * (at most one cheap query per account every few seconds). Admin changes evict the entry immediately.
 */
@Service
public class AccountStatusService {
    private static final long TTL_NANOS = 15_000_000_000L; // 15 seconds
    private static final int MAX_ENTRIES = 5_000;

    private record Entry(Optional<Role> role, long expiresAt) {}

    private final UserRepository users;
    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();

    public AccountStatusService(UserRepository users) { this.users = users; }

    /** The account's role right now, or empty when the account no longer exists. */
    public Optional<Role> currentRole(UUID userId) {
        Entry entry = cache.get(userId);
        if (entry == null || System.nanoTime() >= entry.expiresAt()) {
            if (cache.size() >= MAX_ENTRIES) cache.clear();
            entry = new Entry(users.findRoleById(userId), System.nanoTime() + TTL_NANOS);
            cache.put(userId, entry);
        }
        return entry.role();
    }

    public void evict(UUID userId) { cache.remove(userId); }
}
