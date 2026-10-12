package com.arohan.strengthclub.controller;

import com.arohan.strengthclub.dto.SyncDtos.SyncStatusResponse;
import com.arohan.strengthclub.security.CurrentUserService;
import com.arohan.strengthclub.service.SyncEventService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Checks whether another browser tab or session has changed the user's data. */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final CurrentUserService currentUsers;
    private final SyncEventService syncEvents;

    public SyncController(CurrentUserService currentUsers, SyncEventService syncEvents) {
        this.currentUsers = currentUsers;
        this.syncEvents = syncEvents;
    }

    @GetMapping("/status")
    public SyncStatusResponse status(
            Authentication authentication,
            @RequestParam(defaultValue = "0") long clientVersion,
            @RequestHeader(name = "X-Tab-Session-Id", required = false) String tabId) {
        // The user id comes from the verified token: polling must not cost a user lookup.
        return syncEvents.status(currentUsers.requireUserId(authentication), Math.max(0, clientVersion), tabId);
    }
}
