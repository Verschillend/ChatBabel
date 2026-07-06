package me.verschillend.chatbabel.translation;

/**
 * @param translatedText     the translated text
 * @param detectedLanguage   ISO 639-1 (ish) code of the detected source language, or "auto" if unknown
 */
public record TranslationResult(String translatedText, String detectedLanguage) {
}
