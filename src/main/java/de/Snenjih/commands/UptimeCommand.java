package de.Snenjih.commands;

import com.velocitypowered.api.command.SimpleCommand;
import de.Snenjih.config.ConfigManager;
import de.Snenjih.util.Messages;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UptimeCommand implements SimpleCommand {

    private final ConfigManager config;
    private final Instant startTime;

    public UptimeCommand(ConfigManager config, Instant startTime) {
        this.config = config;
        this.startTime = startTime;
    }

    @Override
    public void execute(Invocation invocation) {
        Duration uptime = Duration.between(startTime, Instant.now());
        long days  = uptime.toDays();
        long hours = uptime.toHoursPart();
        long mins  = uptime.toMinutesPart();
        long secs  = uptime.toSecondsPart();

        String formatted = days + "d " + hours + "h " + mins + "m " + secs + "s";
        invocation.source().sendMessage(Messages.prefix(config).append(
                Messages.legacy("§7Proxy Uptime: §b" + formatted)));
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}
