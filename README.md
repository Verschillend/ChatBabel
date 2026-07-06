# ChatBabel

A Fabric mod for Minecraft **1.21.11** (Yarn `1.21.11+build.4`, Fabric Loader `0.18.4`) that automatically
detects the language of incoming chat messages and translates them for you — without ever needing an API key
or paying for anything.

Instead of just replacing chat with the translated text, you choose (in-game, via [Cloth Config](https://modrinth.com/mod/cloth-config))
between two display modes:

- **Show Translated (hover for original)** — chat shows the translated line; hover it to see exactly what was typed.
- **Show Original (hover for translated)** — chat shows exactly what was typed; hover it to see the translation.

## Features

- 🌍 **Automatic language detection + translation** of chat messages, using a free, keyless translation backend
  (see [Translation backend](#translation-backend) below).
- 🖱️ **Hover tooltips** showing whichever of original/translated text isn't the primary display, plus (optionally)
  the detected source language on its own line.
- 🧑 **Username-aware** — cross-checks words in a message against the current online player list, so a player
  named e.g. "Hola" doesn't get their name mistranslated, and a message that's *just* someone's name is left alone.
  A message that mixes a username with actual foreign-language text still gets translated correctly.
- 🔤 **Chat acronym/slang expansion** (`rn`, `wdym`, `smth`, `idk`, `brb`, `imo`, and ~50 others) before translation,
  so the translator understands abbreviations correctly instead of trying to translate them literally.
- 🔒 **Caesar cipher (ROT-N) auto-decoding** — optionally detects and decodes simple letter-shift ciphers in chat
  before translating/displaying them, with the shift amount noted on hover.
- 👻 **Obfuscated text reveal** — hover over `§k` magic/scrambled text (in same-language messages) to see what it
  actually says.
- 🚫 **Per-language ignore list** — tell ChatBabel to never touch specific languages even if they aren't your native one.
- 📊 **HUD translation** — optionally translate the scoreboard sidebar title, boss bar names, titles, subtitles,
  and the action bar. These are always shown as direct translations (no hover), matching how those HUD elements work.
- ⚙️ **Fully configurable in-game** via Cloth Config — no config file editing required (though you still can,
  it's a normal JSON file under `.minecraft/config/chatbabel.json`).

## Opening the config in-game

Any of these work:

- Press the **ChatBabel** keybind (unbound by default — set one in `Options > Controls > ChatBabel`).
- Run the `/chatbabel` client-side command.
- If you have [Mod Menu](https://modrinth.com/mod/modmenu) installed, open it from there like any other mod.

## Translation backend

By default ChatBabel uses the same unauthenticated endpoint `translate.google.com`'s own web page calls
(`translate.googleapis.com/translate_a/single?client=gtx`). It requires **no API key, no Google account, and no
billing** — which is exactly why so many free/open-source translation tools use it. Being unofficial and
undocumented, though, it isn't guaranteed to stay up or unlimited forever.

If it ever stops working for you, switch **Translation Provider** to **LibreTranslate** in the config.
[LibreTranslate](https://github.com/LibreTranslate/LibreTranslate) is fully open-source and can be self-hosted for
free with no rate limits and no key — just point **LibreTranslate URL** at your own instance. The public
`libretranslate.com` demo instance also works out of the box, but is rate-limited and may ask for its own API key
under heavy use.

Both options are completely free; you will never be asked to pay or sign up for anything to use this mod.

## Known limitations (read before reporting a "bug")

- **Scoreboard translation covers the sidebar title only**, not individual score lines. Since the 1.20.3 scoreboard
  rework, each line's text is assembled from team prefixes/suffixes and player/score-holder names deep inside
  `InGameHud`'s rendering code rather than through one stable method, which was too fragile to ship confidently.
  The title (e.g. "☠ Kills ☠") is translated; per-player line text currently is not.
- **Boss bar / scoreboard title mixins are "best effort."** They live in `chatbabel.mixins.optional.json` with
  `"required": false`, meaning if a future mappings build renames their target methods, ChatBabel silently skips
  that one feature (logging a warning) instead of crashing. Chat translation, and title/subtitle/action bar
  translation, are unaffected either way.
- **Rich formatting inside a translated chat message is simplified.** Minecraft chat messages can be a tree of
  differently-styled/clickable text runs (e.g. an embedded link). Translation operates on the plain-text content,
  so a translated message keeps its overall color/style but loses any per-word formatting or click actions the
  original had. Obfuscated-text-reveal, by contrast, fully preserves per-run styling — it just doesn't run
  side-by-side with translation on the same message (see the Javadoc on `ObfuscationRevealer` for why).
- **Translation happens synchronously, with a short timeout**, not asynchronously in the background. Repeated/cached
  phrases resolve instantly; a brand-new phrase can cause a small (bounded, ~1-2s worst case) one-off hitch while
  it's fetched. This keeps the implementation simple and robust rather than needing to reconstruct Minecraft's
  chat-decoration pipeline to patch a message in after the fact. If this bothers you on very high-traffic servers,
  that's the first place to optimize (see `ChatMessageProcessor`).
- **Caesar-cipher decoding always scores against English** common words, regardless of your native language setting,
  since Caesar-shift chat memes are essentially always in English regardless of who's reading them.

## Project layout

```
src/main/java/com/chatbabel/
├── ChatBabel.java                 shared constants/logger
├── ChatBabelClient.java           client entrypoint: wires chat events, keybind, /chatbabel command
├── config/                        Cloth Config (AutoConfig) data class + enums
├── translation/                   TranslationService + Google(unofficial)/LibreTranslate implementations + cache
├── pipeline/                      username masking, acronym expansion, Caesar cipher, obfuscation reveal,
│                                   and the two orchestrators (chat vs. HUD)
├── mixin/client/                  InGameHud (title/subtitle/action bar), ScoreboardObjective, BossBar mixins
└── integration/                   optional Mod Menu config screen hookup
```

## Building / opening in IntelliJ IDEA

1. Clone/unzip this project.
2. **Gradle Wrapper**: this repo intentionally does *not* commit the binary `gradle-wrapper.jar` (it's a compiled
   artifact, not source). Before opening the project, generate it once:
   - If you have Gradle installed system-wide: run `gradle wrapper --gradle-version 8.14` in the project root.
   - Otherwise, open the folder in IntelliJ anyway — IntelliJ's Gradle integration can use its own bundled Gradle
     distribution to import the project even without a wrapper present (`Settings > Build Tools > Gradle >
     Gradle JVM/Distribution`), and you can generate the wrapper afterwards from the Gradle tool window
     (`Tasks > build setup > wrapper`).
3. **File > Open...** and select the project's root folder (the one with `build.gradle`).
4. Let IntelliJ import the Gradle project (it will download Minecraft, Yarn mappings, Fabric API, Cloth Config, etc.
   the first time — this can take a few minutes).
5. Run the `genSources` / `genClientSources` Gradle task if you want fully-named/decompiled Minecraft sources for
   browsing (`Gradle tool window > chatbabel > Tasks > fabric > genClientSources`).
6. Use the **Run/Debug Configurations** Loom generates automatically (`Minecraft Client`) to launch the game with
   the mod loaded, or run `./gradlew runClient` from a terminal once the wrapper exists.
7. Build a distributable jar with `./gradlew build`; it appears under `build/libs/`.

### If a mapping name doesn't match

Since 1.21.11 is a recent Minecraft version, always double check exact method names against **your** resolved
Yarn mappings before assuming a compile error is a typo — right-click a Minecraft class in IntelliJ and choose
"Go to declaration", or run the `genClientSources` task, to see the real decompiled signatures. The mixins in
`chatbabel.mixins.optional.json` are written defensively (`required: false`) for exactly this reason.

## Config reference

All options live in `.minecraft/config/chatbabel.json` and are editable in-game (see above). Key ones:

| Option | Default | Notes |
|---|---|---|
| `modEnabled` | `true` | Master on/off switch |
| `nativeLanguage` | `en` | ISO 639-1 code to translate INTO |
| `displayMode` | Show Translated / hover original | The two modes described above |
| `translationProvider` | Google (unofficial) | or LibreTranslate |
| `libreTranslateUrl` | `https://libretranslate.com/translate` | only used if provider = LibreTranslate |
| `showSourceLanguageOnHover` | `true` | adds a "Translated from: X" hover line |
| `translateChatMessages` | `true` | master switch for chat translation specifically |
| `expandAcronyms` | `true` | rn/wdym/smth/idk/etc. expansion before translating |
| `detectPlayerUsernames` | `true` | avoid mistranslating names |
| `caesarCipherDecoding` | `false` | ROT-N auto-decode |
| `revealObfuscatedText` | `true` | hover `§k` text to reveal it |
| `ignoredLanguages` | `[]` | list of ISO codes to never translate |
| `translateScoreboard` / `translateBossBar` / `translateTitle` / `translateSubtitle` / `translateActionBar` | `false` each | direct translation, no hover |

## License

MIT — see [LICENSE](LICENSE).
