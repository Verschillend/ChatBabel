package me.verschillend.chatbabel.translation;

public interface TranslationService {

    /**
     * @param text       the text to translate
     * @param targetLang ISO 639-1 code of the language to translate INTO
     * @return the translated text plus the detected source language
     */
    TranslationResult translate(String text, String targetLang) throws TranslationException;
}
