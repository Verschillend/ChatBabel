package me.verschillend.chatbabel.config;

public enum Language {
    ENGLISH("en"), SPANISH("es"), FRENCH("fr"), GERMAN("de"), ITALIAN("it"),
    PORTUGUESE("pt"), DUTCH("nl"), RUSSIAN("ru"), UKRAINIAN("uk"), POLISH("pl"),
    CZECH("cs"), SLOVAK("sk"), SLOVENIAN("sl"), CROATIAN("hr"), SERBIAN("sr"),
    BULGARIAN("bg"), ROMANIAN("ro"), HUNGARIAN("hu"), GREEK("el"), TURKISH("tr"),
    SWEDISH("sv"), NORWEGIAN("no"), DANISH("da"), FINNISH("fi"), ICELANDIC("is"),
    IRISH("ga"), WELSH("cy"), CATALAN("ca"), ESTONIAN("et"), LATVIAN("lv"),
    LITHUANIAN("lt"), ARABIC("ar"), HEBREW("he"), PERSIAN("fa"), HINDI("hi"),
    BENGALI("bn"), URDU("ur"), THAI("th"), VIETNAMESE("vi"), INDONESIAN("id"),
    MALAY("ms"), JAPANESE("ja"), KOREAN("ko"), CHINESE_SIMPLIFIED("zh-CN"),
    CHINESE_TRADITIONAL("zh-TW"), SWAHILI("sw"), AFRIKAANS("af"), ESPERANTO("eo"),
    LATIN("la");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}