package me.verschillend.chatbabel.pipeline;

import me.verschillend.chatbabel.ChatBabel;
import me.verschillend.chatbabel.config.ChatBabelConfig;
import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.config.TranslationProvider;
import me.verschillend.chatbabel.translation.GoogleTranslateService;
import me.verschillend.chatbabel.translation.LibreTranslateService;
import me.verschillend.chatbabel.translation.TranslationCache;
import me.verschillend.chatbabel.translation.TranslationException;
import me.verschillend.chatbabel.translation.TranslationResult;
import me.verschillend.chatbabel.translation.TranslationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public final class HudTextTranslator {

    public static final HudTextTranslator INSTANCE = new HudTextTranslator();

    private final TranslationCache cache = new TranslationCache();
    private final GoogleTranslateService googleService = new GoogleTranslateService();
    private final LibreTranslateService libreService =
            new LibreTranslateService(() -> ChatBabelConfigHolder.getConfig().libreTranslateUrl);
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "chatbabel-hud-translator");
        t.setDaemon(true);
        return t;
    });
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> APPLYING = ThreadLocal.withInitial(() -> false);

    private HudTextTranslator() {
    }

    public void translateAsync(Text original, boolean featureFlag, Consumer<Text> onTranslated) {
        if (Boolean.TRUE.equals(APPLYING.get())) {
            return;
        }
        if (original == null) {
            return;
        }
        ChatBabelConfig config = ChatBabelConfigHolder.getConfig();
        if (!config.modEnabled || !featureFlag) {
            return;
        }
        String raw = original.getString();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String targetLang = config.nativeLanguage.code();

        TranslationResult cached = cache.get(raw, targetLang);
        if (cached != null) {
            Text built = applyIfDifferentLanguage(original, cached, config);
            if (built != null) {
                APPLYING.set(true);
                try {
                    onTranslated.accept(built);
                } finally {
                    APPLYING.set(false);
                }
            }
            return;
        }

        String key = targetLang + '\u0000' + raw;
        if (!inFlight.add(key)) {
            return;
        }

        TranslationService service = config.translationProvider == TranslationProvider.LIBRETRANSLATE
                ? libreService
                : googleService;

        executor.submit(() -> {
            try {
                TranslationResult result = service.translate(raw, targetLang);
                cache.put(raw, targetLang, result);
                Text built = applyIfDifferentLanguage(original, result, config);
                if (built != null) {
                    MinecraftClient.getInstance().execute(() -> {
                        APPLYING.set(true);
                        try {
                            onTranslated.accept(built);
                        } finally {
                            APPLYING.set(false);
                        }
                    });
                }
            } catch (TranslationException e) {
                ChatBabel.LOGGER.debug("ChatBabel: HUD translation failed: {}", e.getMessage());
            } finally {
                inFlight.remove(key);
            }
        });
    }

    private Text applyIfDifferentLanguage(Text original, TranslationResult result, ChatBabelConfig config) {
        String detected = normalize(result.detectedLanguage());
        String native_ = normalize(config.nativeLanguage.code());
        if (detected == null || detected.equals(native_)) {
            return null;
        }
        if (config.ignoredLanguages.stream().anyMatch(name -> matchesIgnored(name, detected))) {
            return null;
        }
        return Text.literal(result.translatedText()).setStyle(original.getStyle());
    }

    private static boolean matchesIgnored(String configuredName, String detectedCode) {
        try {
            me.verschillend.chatbabel.config.Language lang = me.verschillend.chatbabel.config.Language.valueOf(configuredName.trim().toUpperCase(Locale.ROOT));
            return normalize(lang.code()).equals(detectedCode);
        } catch (IllegalArgumentException e) {
            return false;
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
}