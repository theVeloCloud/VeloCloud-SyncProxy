package de.Snenjih.messaging;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateMessageManager {

    private final ConcurrentHashMap<UUID, UUID> lastMessaged = new ConcurrentHashMap<>();

    public void setLastMessaged(UUID sender, UUID recipient) {
        lastMessaged.put(sender, recipient);
    }

    public Optional<UUID> getLastMessaged(UUID uuid) {
        return Optional.ofNullable(lastMessaged.get(uuid));
    }

    public void cleanup(UUID uuid) {
        lastMessaged.remove(uuid);
    }
}
