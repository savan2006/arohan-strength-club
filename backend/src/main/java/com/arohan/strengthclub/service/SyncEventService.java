package com.arohan.strengthclub.service;

import com.arohan.strengthclub.dto.SyncDtos.SyncStatusResponse;
import com.arohan.strengthclub.entity.SyncEvent;
import com.arohan.strengthclub.repository.SyncEventRepository;
import com.arohan.strengthclub.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stores cross-instance change events in PostgreSQL; no in-memory singleton version is required. */
@Service
public class SyncEventService {
    private static final Logger log = LoggerFactory.getLogger(SyncEventService.class);
    private static final Pattern MEMBER_PATH = Pattern.compile("/api/trainer/members/([0-9a-fA-F-]{36})(?:/|$)");
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final SyncEventRepository events;
    private final UserRepository users;
    // Change markers are advisory. Writing them on a background thread keeps the extra INSERT
    // (a full database round trip) off the critical path of every mutation response.
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "sync-event-writer");
        thread.setDaemon(true);
        return thread;
    });

    public SyncEventService(SyncEventRepository events, UserRepository users) {
        this.events = events;
        this.users = users;
    }

    @PreDestroy
    void shutdown() {
        writer.shutdown();
    }

    /** Called after each request; only cheap in-memory work happens on the request thread. */
    public void captureSuccessfulMutation(HttpServletRequest request, int status, Exception failure) {
        if (failure != null || status < 200 || status >= 400) return;
        String method = request.getMethod();
        if (!MUTATING.contains(method)) return;
        String path = request.getRequestURI();
        if (path.startsWith("/api/sync/") || path.startsWith("/api/auth/") || path.startsWith("/api/health")) return;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) return;

        UUID actorId = actorId(authentication);
        String actorEmail = authentication.getName();
        String tabHeader = request.getHeader("X-Tab-Session-Id");
        String tabId = tabHeader == null || tabHeader.isBlank() ? null : tabHeader.substring(0, Math.min(80, tabHeader.length()));

        try {
            writer.execute(() -> persist(method, path, actorId, actorEmail, tabId));
        } catch (RuntimeException ex) {
            log.debug("Sync marker skipped for {} {}: {}", method, path, ex.getMessage());
        }
    }

    private void persist(String method, String path, UUID tokenActorId, String actorEmail, String tabId) {
        try {
            UUID actorId = tokenActorId != null ? tokenActorId
                    : users.findByEmailIgnoreCase(actorEmail).map(u -> u.getId()).orElse(null);
            if (actorId == null) return;

            boolean global = path.startsWith("/api/admin/") || path.startsWith("/api/membership-plans/")
                    || path.startsWith("/api/gym/");
            UUID targetUserId = actorId;
            Matcher trainerMember = MEMBER_PATH.matcher(path);
            if (trainerMember.find()) {
                try { targetUserId = UUID.fromString(trainerMember.group(1)); }
                catch (IllegalArgumentException ignored) { targetUserId = actorId; }
            }

            SyncEvent event = new SyncEvent();
            event.setActorUserId(actorId);
            event.setTargetUserId(global ? null : targetUserId);
            event.setCategory(global ? "GLOBAL" : categoryFor(path));
            event.setInitiatorTabId(tabId);
            events.save(event);
        } catch (Exception ex) {
            // Sync is advisory and must never turn a successfully saved business operation into a failure.
            log.warn("Could not record cross-session change marker for {} {}: {}", method, path, ex.getMessage());
        }
    }

    private static UUID actorId(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String uid = jwtAuth.getToken().getClaimAsString("uid");
            if (uid != null) {
                try { return UUID.fromString(uid); } catch (IllegalArgumentException ignored) { /* fall back to email */ }
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public SyncStatusResponse status(UUID userId, long clientVersion, String tabId) {
        long latestVersion = events.findTopByOrderByIdDesc().map(SyncEvent::getId).orElse(0L);
        if (clientVersion <= 0 || clientVersion >= latestVersion) {
            return new SyncStatusResponse(false, latestVersion, List.of());
        }
        List<String> changes = events.findByIdGreaterThanOrderByIdAsc(clientVersion).stream()
                .filter(event -> !sameTab(event, tabId))
                .filter(event -> "GLOBAL".equals(event.getCategory())
                        || userId.equals(event.getTargetUserId())
                        || userId.equals(event.getActorUserId()))
                .map(SyncEvent::getCategory)
                .distinct().toList();
        return new SyncStatusResponse(!changes.isEmpty(), latestVersion, changes);
    }

    /** Old event markers are housekeeping data; retain one month for clients that return after being offline. */
    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void deleteExpiredEvents() {
        long removed = events.deleteByCreatedAtBefore(Instant.now().minus(30, ChronoUnit.DAYS));
        if (removed > 0) log.info("Removed {} expired sync markers", removed);
    }

    private boolean sameTab(SyncEvent event, String currentTabId) {
        return currentTabId != null && !currentTabId.isBlank() && currentTabId.equals(event.getInitiatorTabId());
    }

    private String categoryFor(String path) {
        if (path.contains("weekly-schedule") || path.contains("workout-plan") || path.contains("workout-source")) return "WORKOUT_PLAN";
        if (path.contains("membership")) return "MEMBERSHIP";
        if (path.contains("attendance")) return "ATTENDANCE";
        if (path.contains("trainer-preference") || path.contains("assign-plan")) return "TRAINER_ASSIGNMENT";
        if (path.startsWith("/api/trainer/")) return "TRAINER_WORKSPACE";
        return "PROFILE";
    }
}
