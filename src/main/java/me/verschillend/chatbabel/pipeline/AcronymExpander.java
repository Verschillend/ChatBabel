package me.verschillend.chatbabel.pipeline;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AcronymExpander {

    // Word-boundary matched, case-insensitive. Keep additions lower-case.
    private static final Map<String, String> ACRONYMS = Map.ofEntries(
            Map.entry("rn", "right now"),
            Map.entry("lwk", "lowkey"),
            Map.entry("wdym", "what do you mean"),
            Map.entry("smth", "something"),
            Map.entry("idk", "I don't know"),
            Map.entry("idc", "I don't care"),
            Map.entry("imo", "in my opinion"),
            Map.entry("imho", "in my humble opinion"),
            Map.entry("tbh", "to be honest"),
            Map.entry("ngl", "not gonna lie"),
            Map.entry("fr", "for real"),
            Map.entry("nvm", "never mind"),
            Map.entry("brb", "be right back"),
            Map.entry("gtg", "got to go"),
            Map.entry("g2g", "got to go"),
            Map.entry("afk", "away from keyboard"),
            Map.entry("btw", "by the way"),
            Map.entry("lol", "laughing out loud"),
            Map.entry("lmao", "laughing my ass off"),
            Map.entry("rofl", "rolling on the floor laughing"),
            Map.entry("omg", "oh my god"),
            Map.entry("wyd", "what are you doing"),
            Map.entry("wbu", "what about you"),
            Map.entry("hbu", "how about you"),
            Map.entry("ily", "I love you"),
            Map.entry("jk", "just kidding"),
            Map.entry("atm", "at the moment"),
            Map.entry("asap", "as soon as possible"),
            Map.entry("ikr", "I know right"),
            Map.entry("tysm", "thank you so much"),
            Map.entry("ty", "thank you"),
            Map.entry("np", "no problem"),
            Map.entry("yw", "you're welcome"),
            Map.entry("gg", "good game"),
            Map.entry("gl", "good luck"),
            Map.entry("hf", "have fun"),
            Map.entry("wp", "well played"),
            Map.entry("afaik", "as far as I know"),
            Map.entry("iirc", "if I recall correctly"),
            Map.entry("smh", "shaking my head"),
            Map.entry("tbf", "to be fair"),
            Map.entry("rq", "real quick"),
            Map.entry("rlly", "really"),
            Map.entry("prob", "probably"),
            Map.entry("probs", "probably"),
            Map.entry("def", "definitely"),
            Map.entry("obv", "obviously"),
            Map.entry("cuz", "because"),
            Map.entry("bc", "because"),
            Map.entry("b4", "before"),
            Map.entry("thx", "thanks"),
            Map.entry("pls", "please"),
            Map.entry("plz", "please"),
            Map.entry("u", "you"),
            Map.entry("ur", "your"),
            Map.entry("k", "ok"),
            Map.entry("wb", "welcome back")
    );

    private static final Pattern TOKEN_PATTERN;

    static {
        String alternation = ACRONYMS.keySet().stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        TOKEN_PATTERN = Pattern.compile("\\b(" + alternation + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    private AcronymExpander() {
    }

    public static String expand(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            String matched = matcher.group();
            String expansion = ACRONYMS.get(matched.toLowerCase(Locale.ROOT));
            if (expansion == null) {
                continue;
            }
            result.append(text, last, matcher.start());
            if (Character.isUpperCase(matched.charAt(0))) {
                result.append(Character.toUpperCase(expansion.charAt(0))).append(expansion.substring(1));
            } else {
                result.append(expansion);
            }
            last = matcher.end();
        }
        result.append(text.substring(last));
        return result.toString();
    }
}
