package de.Snenjih.ban;

import java.time.Instant;

public final class BanEntry {

    private final long id;
    private final String uuid;
    private final String username;
    private final String ip;
    private final String type;
    private final String reason;
    private final String bannerName;
    private final String bannerUuid;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean active;

    public BanEntry(long id, String uuid, String username, String ip,
                    String type, String reason, String bannerName, String bannerUuid,
                    Instant createdAt, Instant expiresAt, boolean active) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.ip = ip;
        this.type = type;
        this.reason = reason;
        this.bannerName = bannerName;
        this.bannerUuid = bannerUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public long getId()           { return id; }
    public String getUuid()       { return uuid; }
    public String getUsername()   { return username; }
    public String getIp()         { return ip; }
    public String getType()       { return type; }
    public String getReason()     { return reason; }
    public String getBannerName() { return bannerName; }
    public String getBannerUuid() { return bannerUuid; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive()     { return active; }

    public boolean isCurrentlyBanned() {
        if (!active || !"BAN".equals(type)) return false;
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    public boolean isPermanent() { return expiresAt == null; }
}
