package me.verschillend.chatbabel.translation;

public class TranslationException extends Exception {
    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
