package me.verschillend.chatbabel.pipeline;

import java.util.Locale;
import java.util.Map;

public final class LanguageNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("af", "Afrikaans"), Map.entry("ar", "Arabic"), Map.entry("bg", "Bulgarian"),
            Map.entry("bn", "Bengali"), Map.entry("ca", "Catalan"), Map.entry("cs", "Czech"),
            Map.entry("cy", "Welsh"), Map.entry("da", "Danish"), Map.entry("de", "German"),
            Map.entry("el", "Greek"), Map.entry("en", "English"), Map.entry("eo", "Esperanto"),
            Map.entry("es", "Spanish"), Map.entry("et", "Estonian"), Map.entry("fa", "Persian"),
            Map.entry("fi", "Finnish"), Map.entry("fr", "French"), Map.entry("ga", "Irish"),
            Map.entry("he", "Hebrew"), Map.entry("hi", "Hindi"), Map.entry("hr", "Croatian"),
            Map.entry("hu", "Hungarian"), Map.entry("id", "Indonesian"), Map.entry("is", "Icelandic"),
            Map.entry("it", "Italian"), Map.entry("ja", "Japanese"), Map.entry("ko", "Korean"),
            Map.entry("lt", "Lithuanian"), Map.entry("lv", "Latvian"), Map.entry("ms", "Malay"),
            Map.entry("nl", "Dutch"), Map.entry("no", "Norwegian"), Map.entry("pl", "Polish"),
            Map.entry("pt", "Portuguese"), Map.entry("ro", "Romanian"), Map.entry("ru", "Russian"),
            Map.entry("sk", "Slovak"), Map.entry("sl", "Slovenian"), Map.entry("sr", "Serbian"),
            Map.entry("sv", "Swedish"), Map.entry("sw", "Swahili"), Map.entry("th", "Thai"),
            Map.entry("tr", "Turkish"), Map.entry("uk", "Ukrainian"), Map.entry("ur", "Urdu"),
            Map.entry("vi", "Vietnamese"), Map.entry("zh", "Chinese"), Map.entry("zh-cn", "Chinese"),
            Map.entry("zh-tw", "Chinese (Traditional)")
    );

    private LanguageNames() {
    }

    public static String displayName(String code) {
        if (code == null) {
            return "Unknown";
        }
        String lookup = code.toLowerCase(Locale.ROOT);
        return NAMES.getOrDefault(lookup, code);
    }
}
