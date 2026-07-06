package me.verschillend.chatbabel.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GoogleTranslateService implements TranslationService {

    private static final String ENDPOINT = "https://translate.googleapis.com/translate_a/single";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public TranslationResult translate(String text, String targetLang) throws TranslationException {
        if (text == null || text.isBlank()) {
            return new TranslationResult(text == null ? "" : text, "auto");
        }

        String query = "client=gtx&sl=auto&tl=" + urlEncode(mapLangCode(targetLang)) + "&dt=t&q=" + urlEncode(text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + "?" + query))
                .timeout(Duration.ofSeconds(4))
                .header("User-Agent", "Mozilla/5.0 (compatible; ChatBabelMinecraftMod/1.0)")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new TranslationException("Google Translate (unofficial) returned HTTP " + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (IOException e) {
            throw new TranslationException("Failed to reach Google Translate: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation request interrupted", e);
        }
    }

    private TranslationResult parseResponse(String body) throws TranslationException {
        try {
            JsonArray root = JsonParser.parseString(body).getAsJsonArray();
            JsonArray chunks = root.get(0).getAsJsonArray();

            StringBuilder translated = new StringBuilder();
            for (JsonElement chunkEl : chunks) {
                if (!chunkEl.isJsonArray()) {
                    continue;
                }
                JsonArray chunk = chunkEl.getAsJsonArray();
                if (chunk.size() > 0 && !chunk.get(0).isJsonNull()) {
                    translated.append(chunk.get(0).getAsString());
                }
            }

            String detected = "auto";
            if (root.size() > 2 && root.get(2) != null && !root.get(2).isJsonNull() && root.get(2).isJsonPrimitive()) {
                detected = root.get(2).getAsString();
            }

            return new TranslationResult(translated.toString(), detected);
        } catch (Exception e) {
            throw new TranslationException("Could not parse Google Translate response", e);
        }
    }

    private static String mapLangCode(String code) {
        if (code == null || code.isBlank()) {
            return "en";
        }
        return switch (code.toLowerCase(java.util.Locale.ROOT)) {
            case "zh" -> "zh-CN";
            case "he" -> "iw";
            default -> code;
        };
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
