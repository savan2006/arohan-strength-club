package com.arohan.strengthclub.dto;

import java.util.List;

public final class SyncDtos {
    private SyncDtos() {}
    public record SyncStatusResponse(boolean hasChanges, long latestVersion, List<String> changes) {}
}
