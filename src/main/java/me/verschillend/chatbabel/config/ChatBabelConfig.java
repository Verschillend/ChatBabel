package me.verschillend.chatbabel.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "chatbabel")
public class ChatBabelConfig implements ConfigData {

    // ----------------------------------------------------------------
    // General
    // ----------------------------------------------------------------

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean modEnabled = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public me.verschillend.chatbabel.config.Language nativeLanguage = Language.ENGLISH;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public DisplayMode displayMode = DisplayMode.SHOW_ORIGINAL_HOVER_TRANSLATED;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public TranslationProvider translationProvider = TranslationProvider.GOOGLE_UNOFFICIAL;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public String libreTranslateUrl = "https://libretranslate.com/translate";

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean showSourceLanguageOnHover = true;

    // ----------------------------------------------------------------
    // Chat
    // ----------------------------------------------------------------

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public boolean translateChatMessages = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public boolean expandAcronyms = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public boolean detectPlayerUsernames = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public boolean caesarCipherDecoding = false;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public boolean revealObfuscatedText = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public List<String> ignoredLanguages = new ArrayList<>();

    // ----------------------------------------------------------------
    // HUD (scoreboard / boss bar / title / subtitle / action bar)
    // Each is direct-translation only, no hover, by design (matches the spec).
    // ----------------------------------------------------------------

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.Tooltip
    public boolean translateScoreboard = false;

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.Tooltip
    public boolean translateBossBar = false;

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.Tooltip
    public boolean translateTitle = false;

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.Tooltip
    public boolean translateSubtitle = false;

    @ConfigEntry.Category("hud")
    @ConfigEntry.Gui.Tooltip
    public boolean translateActionBar = false;
}
