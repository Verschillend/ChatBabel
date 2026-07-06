package me.verschillend.chatbabel.translation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class TranslationCache {

    private static final long MAX_BYTES = 1024L * 1024L; // 1MB
    private static final int EVICT_BATCH = 100;

    private final LinkedHashMap<String, TranslationResult> cache = new LinkedHashMap<>(64, 0.75f, true);
    private long approximateBytes = 0;

    public synchronized TranslationResult get(String sourceText, String targetLang) {
        return cache.get(key(sourceText, targetLang));
    }

    public synchronized void put(String sourceText, String targetLang, TranslationResult result) {
        String k = key(sourceText, targetLang);
        TranslationResult previous = cache.put(k, result);
        approximateBytes += estimateSize(k, result);
        if (previous != null) {
            approximateBytes -= estimateSize(k, previous);
        }
        if (approximateBytes > MAX_BYTES) {
            evictOldest();
        }
    }

    public synchronized void clear() {
        cache.clear();
        approximateBytes = 0;
    }

    private void evictOldest() {
        Iterator<Map.Entry<String, TranslationResult>> it = cache.entrySet().iterator();
        int removed = 0;
        while (it.hasNext() && removed < EVICT_BATCH) {
            Map.Entry<String, TranslationResult> entry = it.next();
            approximateBytes -= estimateSize(entry.getKey(), entry.getValue());
            it.remove();
            removed++;
        }
    }

    private static long estimateSize(String key, TranslationResult result) {
        long size = key.length() * 2L;
        if (result != null) {
            if (result.translatedText() != null) size += result.translatedText().length() * 2L;
            if (result.detectedLanguage() != null) size += result.detectedLanguage().length() * 2L;
        }
        return size;
    }

    private static String key(String sourceText, String targetLang) {
        return targetLang + '\u0000' + sourceText;
    }
}