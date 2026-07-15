package me.verschillend.chatbabel.pipeline;

import me.verschillend.chatbabel.ChatBabel;
import me.verschillend.chatbabel.config.ChatBabelConfig;
import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.config.DisplayMode;
import me.verschillend.chatbabel.config.TranslationProvider;
import me.verschillend.chatbabel.translation.GoogleTranslateService;
import me.verschillend.chatbabel.translation.LibreTranslateService;
import me.verschillend.chatbabel.translation.TranslationCache;
import me.verschillend.chatbabel.translation.TranslationException;
import me.verschillend.chatbabel.translation.TranslationResult;
import me.verschillend.chatbabel.translation.TranslationService;
import me.verschillend.chatbabel.pipeline.NeverTranslateMatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ChatMessageProcessor {

    private final TranslationCache cache = new TranslationCache();
    private final GoogleTranslateService googleService = new GoogleTranslateService();
    private final LibreTranslateService libreService =
            new LibreTranslateService(() -> ChatBabelConfigHolder.getConfig().libreTranslateUrl);
    private final java.util.concurrent.ExecutorService translationExecutor =
            java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "chatbabel-translator");
                t.setDaemon(true);
                return t;
            });
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "chatbabel-chat-translator");
        t.setDaemon(true);
        return t;
    });
    private final java.util.Set<String> inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public Text process(Text original) {
        ChatBabelConfig config = ChatBabelConfigHolder.getConfig();
        if (!config.modEnabled) {
            return original;
        }

        if (NeverTranslateMatcher.matches(original.getString(), config.neverTranslateIfEndsWith)) {
            return original;
        }

        Text afterTranslation = config.translateChatMessages ? runTranslationPipeline(original, config) : original;

        afterTranslation = config.translateChatMessages ? runTranslationPipeline(original, config) : original;

        if (config.revealObfuscatedText && afterTranslation == original && ObfuscationRevealer.containsObfuscatedRun(original)) {
            return ObfuscationRevealer.reveal(original);
        }
        return afterTranslation;
    }

    private static boolean isVanillaSystemMessage(Text message) {
        if (!(message.getContent() instanceof net.minecraft.text.TranslatableTextContent translatable)) {
            return false;
        }
        String key = translatable.getKey();
        return key.startsWith("death.")
                || key.equals("multiplayer.player.joined")
                || key.equals("multiplayer.player.joined.renamed")
                || key.equals("multiplayer.player.left")
                || key.startsWith("chat.type.advancement.");
    }

    private Text runTranslationPipeline(Text original, ChatBabelConfig config) {
        String rawMessage = original.getString();
        if (rawMessage == null || rawMessage.isBlank()) {
            return original;
        }

        Set<String> onlineUsernames = config.detectPlayerUsernames ? UsernameMasker.getOnlineUsernames() : Set.of();

        if (config.detectPlayerUsernames && UsernameMasker.isJustAUsername(rawMessage, onlineUsernames)) {
            return original;
        }

        UsernameMasker.MaskResult masked = config.detectPlayerUsernames
                ? UsernameMasker.mask(rawMessage, onlineUsernames)
                : new UsernameMasker.MaskResult(rawMessage, java.util.Map.of());

        String workingText = masked.maskedText();

        boolean caesarApplied = false;
        int caesarShift = 0;
        if (config.caesarCipherDecoding) {
            Optional<CaesarCipher.Result> decoded = CaesarCipher.tryDecode(workingText);
            if (decoded.isPresent()) {
                workingText = decoded.get().decodedText();
                caesarApplied = true;
                caesarShift = decoded.get().shift();
            }
        }

        String decodedUnmasked = UsernameMasker.unmask(workingText, masked.placeholders());

        String translationInput = config.expandAcronyms ? AcronymExpander.expand(workingText) : workingText;

        TranslationResult result = translate(translationInput, config);
        if (result == null) {
            if (caesarApplied) {
                return buildResult(config, original, decodedUnmasked, false, null, true, caesarShift);
            }
            return original;
        }

        String detectedLang = normalize(result.detectedLanguage());
        String nativeLang = normalize(config.nativeLanguage.code());

        boolean ignored = detectedLang != null && config.ignoredLanguages.stream()
                .anyMatch(name -> matchesIgnored(name, detectedLang));
        if (ignored) {
            return caesarApplied
                    ? buildResult(config, original, decodedUnmasked, false, null, true, caesarShift)
                    : original;
        }

        boolean sameLanguage = detectedLang == null || detectedLang.equals(nativeLang);

        if (sameLanguage && !caesarApplied) {
            return original;
        }

        String translatedUnmasked = UsernameMasker.unmask(result.translatedText(), masked.placeholders());
        boolean didTranslate = !sameLanguage;
        String processedText = didTranslate ? translatedUnmasked : decodedUnmasked;

        return buildResult(config, original, processedText, didTranslate, detectedLang, caesarApplied, caesarShift);
    }

    private Text buildResult(ChatBabelConfig config, Text original, String processedText,
                             boolean didTranslate, String detectedLang, boolean caesarApplied, int caesarShift) {

        MutableText infoLines = null;
        if (didTranslate && config.showSourceLanguageOnHover && detectedLang != null) {
            MutableText line = Text.literal("\nTranslated from: " + LanguageNames.displayName(detectedLang))
                    .formatted(Formatting.GRAY, Formatting.ITALIC);
            infoLines = infoLines == null ? line : infoLines.append(line);
        }
        if (caesarApplied) {
            MutableText line = Text.literal("\nCaesar cipher decoded (shift " + caesarShift + ")")
                    .formatted(Formatting.GRAY, Formatting.ITALIC);
            infoLines = infoLines == null ? line : infoLines.append(line);
        }

        if (config.displayMode == DisplayMode.SHOW_TRANSLATED_HOVER_ORIGINAL) {
            MutableText primary = Text.literal(processedText);

            MutableText hover = original.copy();
            if (infoLines != null) {
                hover = hover.append(infoLines);
            }
            final MutableText hoverFinal = hover;
            return primary.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverFinal)));
        } else {
            MutableText primary = original.copy();

            MutableText hover = Text.literal(processedText);
            if (infoLines != null) {
                hover = hover.append(infoLines);
            }
            final MutableText hoverFinal = hover;
            return primary.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverFinal)));
        }
    }

    private static MutableText mergeExtraIntoEverySegment(Text text, Text extra) {
        java.util.List<Segment> segments = new java.util.ArrayList<>();
        text.visit((style, asString) -> {
            if (!asString.isEmpty()) segments.add(new Segment(asString, style));
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);

        MutableText result = null;
        for (Segment segment : segments) {
            net.minecraft.text.Style style = segment.style();
            HoverEvent existing = style.getHoverEvent();
            HoverEvent merged;
            if (existing instanceof HoverEvent.ShowText showText) {
                merged = new HoverEvent.ShowText(showText.value().copy().append("\n").append(extra));
            } else if (existing == null) {
                merged = new HoverEvent.ShowText(extra);
            } else {
                merged = existing;
            }
            MutableText part = Text.literal(segment.text()).setStyle(style.withHoverEvent(merged));
            result = (result == null) ? part : result.append(part);
        }
        return result != null ? result : text.copy();
    }

    private static HoverEvent firstHoverEventOf(Text mergedOriginal, Text fallback) {
        HoverEvent[] found = new HoverEvent[1];
        mergedOriginal.visit((style, s) -> {
            if (found[0] == null && style.getHoverEvent() != null) found[0] = style.getHoverEvent();
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return found[0] != null ? found[0] : new HoverEvent.ShowText(fallback);
    }

    private record Segment(String text, net.minecraft.text.Style style) {}

    private TranslationResult translate(String text, ChatBabelConfig config) {
        String targetLang = config.nativeLanguage.code();
        TranslationResult cached = cache.get(text, targetLang);
        if (cached != null) {
            return cached;
        }
        TranslationService service = config.translationProvider == TranslationProvider.LIBRETRANSLATE
                ? libreService : googleService;
        try {
            TranslationResult result = service.translate(text, targetLang);
            cache.put(text, targetLang, result);
            return result;
        } catch (TranslationException e) {
            ChatBabel.LOGGER.warn("ChatBabel: translation failed ({}): {}", config.translationProvider, e.getMessage());
            return null;
        }
    }

    private static String normalize(String langCode) {
        if (langCode == null || langCode.isBlank() || langCode.equalsIgnoreCase("auto")) {
            return null;
        }
        String lower = langCode.toLowerCase(Locale.ROOT);
        int dash = lower.indexOf('-');
        return dash > 0 ? lower.substring(0, dash) : lower;
    }

    public void processAsync(Text original, java.util.function.Consumer<Text> displayCallback) {
        executor.submit(() -> {
            Text result = process(original);
            MinecraftClient.getInstance().execute(() -> displayCallback.accept(result));
        });
    }

    private static boolean matchesIgnored(String configuredName, String detectedCode) {
        try {
            me.verschillend.chatbabel.config.Language lang = me.verschillend.chatbabel.config.Language.valueOf(configuredName.trim().toUpperCase(Locale.ROOT));
            return normalize(lang.code()).equals(detectedCode);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
