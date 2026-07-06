package me.verschillend.chatbabel.pipeline;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CaesarCipher {

    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");

    private static final double MIN_IMPROVEMENT_RATIO = 2.5;
    private static final int MIN_ABSOLUTE_SCORE = 2;

    private CaesarCipher() {
    }

    public static Optional<Result> tryDecode(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        // Only bother if there's meaningful alphabetic content to shift.
        long letters = text.chars().filter(Character::isLetter).count();
        if (letters < 4) {
            return Optional.empty();
        }

        int originalScore = score(text);
        int bestShift = 0;
        int bestScore = originalScore;
        String bestText = text;

        for (int shift = 1; shift <= 25; shift++) {
            String candidate = shift(text, shift);
            int candidateScore = score(candidate);
            if (candidateScore > bestScore) {
                bestScore = candidateScore;
                bestShift = shift;
                bestText = candidate;
            }
        }

        if (bestShift == 0) {
            return Optional.empty();
        }
        if (bestScore < MIN_ABSOLUTE_SCORE) {
            return Optional.empty();
        }
        if (originalScore > 0 && bestScore < originalScore * MIN_IMPROVEMENT_RATIO) {
            return Optional.empty();
        }

        return Optional.of(new Result(bestText, bestShift));
    }

    private static String shift(String text, int shift) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                sb.append((char) ('a' + (c - 'a' + shift) % 26));
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) ('A' + (c - 'A' + shift) % 26));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int score(String text) {
        Matcher matcher = WORD_PATTERN.matcher(text);
        int matches = 0;
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (COMMON_WORDS.contains(word)) {
                matches++;
            }
        }
        return matches;
    }

    public record Result(String decodedText, int shift) {
    }

    // A small but high-frequency English word list, enough to reliably
    // distinguish "real sentence" from "random shifted gibberish" without
    // shipping a full dictionary.
    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
            "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
            "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
            "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
            "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
            "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
            "is", "are", "was", "were", "am", "been", "being", "hello", "hi", "hey",
            "yes", "thanks", "sorry", "help", "here", "player", "game", "server", "chat",
            "love", "hate", "kill", "died", "why", "where", "again", "please", "welcome"
    );
}
