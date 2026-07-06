package me.verschillend.chatbabel.pipeline;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UsernameMasker {

    // Minecraft usernames are 3-16 chars of [A-Za-z0-9_], but we go down to 2
    // to be lenient with older/bot accounts.
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9_]{2,16}");
    private static final Set<String> FAKE_PLAYER_STOPLIST = Set.of(
            "stats", "stat", "level", "strength", "defense", "speed", "intelligence",
            "crit", "health", "mana", "coins", "purse", "bank", "skill", "skills",
            "slayer", "rank", "guild", "party", "friends", "online", "offline",
            "menu", "shop", "info", "help", "back", "close", "loading"
    );
    private static final Pattern VALID_USERNAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");

    private UsernameMasker() {
    }

    public static Set<String> getOnlineUsernames() {
        Set<String> names = new HashSet<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return names;
        }
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                String name = entry.getProfile().name();
                if (name != null
                        && VALID_USERNAME.matcher(name).matches()
                        && !FAKE_PLAYER_STOPLIST.contains(name.toLowerCase(Locale.ROOT))) {
                    names.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        return names;
    }

    public static boolean isJustAUsername(String message, Set<String> onlineUsernames) {
        if (message == null) {
            return false;
        }
        String trimmed = message.trim().toLowerCase(Locale.ROOT);
        return onlineUsernames.contains(trimmed);
    }

    public static MaskResult mask(String message, Set<String> onlineUsernames) {
        if (message == null || onlineUsernames.isEmpty()) {
            return new MaskResult(message, Map.of());
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        Matcher matcher = TOKEN_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();
        int last = 0;
        int index = 0;

        while (matcher.find()) {
            String token = matcher.group();
            if (onlineUsernames.contains(token.toLowerCase(Locale.ROOT))) {
                String placeholder = "cbnametoken" + (index++);
                placeholders.put(placeholder, token);
                result.append(message, last, matcher.start());
                result.append(placeholder);
                last = matcher.end();
            }
        }
        result.append(message.substring(last));
        return new MaskResult(result.toString(), placeholders);
    }
    public static String unmask(String text, Map<String, String> placeholders) {
        if (text == null || placeholders.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replaceAll("(?i)" + Pattern.quote(entry.getKey()), Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    public record MaskResult(String maskedText, Map<String, String> placeholders) {
    }
}
