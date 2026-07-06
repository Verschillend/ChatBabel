package me.verschillend.chatbabel.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

public class LibreTranslateService implements TranslationService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final Supplier<String> endpointSupplier;

    public LibreTranslateService(Supplier<String> endpointSupplier) {
        this.endpointSupplier = endpointSupplier;
    }

    @Override
    public TranslationResult translate(String text, String targetLang) throws TranslationException {
        if (text == null || text.isBlank()) {
            return new TranslationResult(text == null ? "" : text, "auto");
        }

        JsonObject body = new JsonObject();
        body.addProperty("q", text);
        body.addProperty("source", "auto");
        body.addProperty("target", targetLang);
        body.addProperty("format", "text");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointSupplier.get()))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new TranslationException("LibreTranslate returned HTTP " + response.statusCode() + ": " + response.body());
            }
            return parseResponse(response.body());
        } catch (IOException e) {
            throw new TranslationException("Failed to reach LibreTranslate: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TranslationException("Translation request interrupted", e);
        }
    }

    private TranslationResult parseResponse(String body) throws TranslationException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            String translated = root.has("translatedText") ? root.get("translatedText").getAsString() : "";

            String detected = "auto";
            if (root.has("detectedLanguage") && root.get("detectedLanguage").isJsonObject()) {
                JsonObject detectedObj = root.getAsJsonObject("detectedLanguage");
                if (detectedObj.has("language")) {
                    detected = detectedObj.get("language").getAsString();
                }
            }
            return new TranslationResult(translated, detected);
        } catch (Exception e) {
            throw new TranslationException("Could not parse LibreTranslate response", e);
        }
    }
}
