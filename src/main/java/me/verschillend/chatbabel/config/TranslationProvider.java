package me.verschillend.chatbabel.config;

/** Which free, keyless translation backend to use. */
public enum TranslationProvider {
    /** The unofficial translate.googleapis.com "gtx" client endpoint. No key, no account, no cost. */
    GOOGLE_UNOFFICIAL,
    /** Open-source LibreTranslate REST API. Free if you self-host; the public instance is rate limited. */
    LIBRETRANSLATE
}
