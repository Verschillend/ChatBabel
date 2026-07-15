package me.verschillend.chatbabel.pipeline;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class NeverTranslateMatcher {

    private static final Pattern COLOR_CODE = Pattern.compile("[&§][0-9a-fk-orA-FK-OR]");

    private NeverTranslateMatcher() {
    }

    public static boolean matches(String rawMessage, List<String> suffixPatterns) {
        if (rawMessage == null || suffixPatterns == null || suffixPatterns.isEmpty()) {
            return false;
        }
        String strippedMessage = strip(rawMessage).toLowerCase(Locale.ROOT);
        for (String pattern : suffixPatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String strippedPattern = strip(pattern).toLowerCase(Locale.ROOT);
            if (!strippedPattern.isEmpty() && strippedMessage.endsWith(strippedPattern)) {
                return true;
            }
        }
        return false;
    }

    private static String strip(String text) {
        return COLOR_CODE.matcher(text).replaceAll("");
    }
}