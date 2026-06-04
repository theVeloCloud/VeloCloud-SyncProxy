package de.Snenjih.ban;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BanCache {

    // null = Cache-Miss, Optional.empty() = bekannt nicht gebannt, Optional.of(e) = aktiver Ban
    private final ConcurrentHashMap<String, Optional<BanEntry>> cache = new ConcurrentHashMap<>();

    /** Vollständiger Austausch beim Cache-Refresh. */
    public void replaceAll(Map<String, BanEntry> activeBans) {
        cache.clear();
        activeBans.forEach((name, entry) -> cache.put(name, Optional.of(entry)));
    }

    /**
     * Gibt den gecachten Wert zurück.
     * null = Cache-Miss (kein Eintrag vorhanden)
     * Optional.empty() = bekannt nicht gebannt
     * Optional.of(entry) = aktiver Ban
     */
    public Optional<BanEntry> get(String username) {
        return cache.get(username.toLowerCase());
    }

    public void put(String username, BanEntry entry) {
        cache.put(username.toLowerCase(), Optional.of(entry));
    }

    /** Markiert als nicht gebannt (nach Pardon oder DB-Bestätigung). */
    public void invalidate(String username) {
        cache.put(username.toLowerCase(), Optional.empty());
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }
}
