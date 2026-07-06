package me.verschillend.chatbabel.config;

/**
 * Controls which text is shown directly in chat, and which text is only
 * revealed when the player hovers over the message.
 */
public enum DisplayMode {
    /** Chat shows the translated text; hovering reveals the original. */
    SHOW_TRANSLATED_HOVER_ORIGINAL,
    /** Chat shows exactly what was typed; hovering reveals the translation. */
    SHOW_ORIGINAL_HOVER_TRANSLATED
}
