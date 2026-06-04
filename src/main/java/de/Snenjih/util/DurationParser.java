package de.Snenjih.util;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private DurationParser() {}

    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])");

    /**
     * Parst eine Dauer wie "1d12h30m", "1d 12h", "perma", "permanent".
     * Gibt Optional.empty() für permanente Bans zurück.
     */
    public static Optional<Instant> parse(String input) {
        if (input == null) throw new IllegalArgumentException("Input darf nicht null sein");
        String s = input.trim().toLowerCase();
        if (s.equals("perma") || s.equals("permanent") || s.equals("perm")) {
            return Optional.empty();
        }
        // Leerzeichen entfernen damit "1d 12h" wie "1d12h" behandelt wird
        s = s.replace(" ", "");
        Matcher m = TOKEN.matcher(s);
        long totalSeconds = 0;
        boolean found = false;
        while (m.find()) {
            long value = Long.parseLong(m.group(1));
            if (value == 0) continue;
            found = true;
            totalSeconds += switch (m.group(2)) {
                case "s" -> value;
                case "m" -> value * 60;
                case "h" -> value * 3600;
                case "d" -> value * 86400;
                case "w" -> value * 604800;
                default  -> 0;
            };
        }
        if (!found) throw new IllegalArgumentException("Keine gültige Dauer: " + input);
        return Optional.of(Instant.now().plusSeconds(totalSeconds));
    }

    /**
     * Parst Dauer aus einem Args-Array ab fromIndex.
     * Konsumiert greedy Tokens (z.B. "1d", "12h") bis ein Wort kein Token ist → Beginn des Grundes.
     */
    public static ParseResult parseFromArgs(String[] args, int fromIndex) {
        long totalSeconds = 0;
        boolean permanent = false;
        int reasonStart = fromIndex;

        for (int i = fromIndex; i < args.length; i++) {
            String token = args[i].trim().toLowerCase();
            if (token.equals("perma") || token.equals("permanent") || token.equals("perm")) {
                permanent = true;
                reasonStart = i + 1;
                continue;
            }
            // Prüfen ob das Argument nur aus Duration-Tokens besteht
            Matcher m = TOKEN.matcher(token.replace(" ", ""));
            boolean allTokens = m.find();
            if (!allTokens) {
                reasonStart = i;
                break;
            }
            // Komplett gescannter Token
            m.reset();
            while (m.find()) {
                long value = Long.parseLong(m.group(1));
                if (value == 0) continue;
                totalSeconds += switch (m.group(2)) {
                    case "s" -> value;
                    case "m" -> value * 60;
                    case "h" -> value * 3600;
                    case "d" -> value * 86400;
                    case "w" -> value * 604800;
                    default  -> 0;
                };
            }
            reasonStart = i + 1;
        }

        Optional<Instant> expiresAt;
        if (permanent) {
            expiresAt = Optional.empty();
        } else if (totalSeconds > 0) {
            expiresAt = Optional.of(Instant.now().plusSeconds(totalSeconds));
        } else {
            throw new IllegalArgumentException("Keine gültige Dauer angegeben");
        }
        return new ParseResult(expiresAt, reasonStart);
    }

    /**
     * Gibt eine lesbare Restzeit zurück: "3 Tage 2 Stunden 14 Minuten" oder "Permanent".
     */
    public static String humanize(Instant expiresAt) {
        if (expiresAt == null) return "Permanent";
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        if (remaining <= 0) return "Abgelaufen";

        long weeks   = remaining / 604800; remaining %= 604800;
        long days    = remaining / 86400;  remaining %= 86400;
        long hours   = remaining / 3600;   remaining %= 3600;
        long minutes = remaining / 60;
        long seconds = remaining % 60;

        StringBuilder sb = new StringBuilder();
        if (weeks   > 0) sb.append(weeks)   .append(weeks   == 1 ? " Woche "   : " Wochen ");
        if (days    > 0) sb.append(days)    .append(days    == 1 ? " Tag "     : " Tage ");
        if (hours   > 0) sb.append(hours)   .append(hours   == 1 ? " Stunde "  : " Stunden ");
        if (minutes > 0) sb.append(minutes) .append(minutes == 1 ? " Minute "  : " Minuten ");
        if (sb.isEmpty()) sb.append(seconds).append(seconds == 1 ? " Sekunde"  : " Sekunden");

        return sb.toString().trim();
    }

    public record ParseResult(Optional<Instant> expiresAt, int reasonStartIndex) {}
}
