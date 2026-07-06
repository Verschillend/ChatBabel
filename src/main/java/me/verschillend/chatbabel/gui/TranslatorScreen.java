package me.verschillend.chatbabel.gui;

import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.config.Language;
import me.verschillend.chatbabel.config.TranslationProvider;
import me.verschillend.chatbabel.translation.GoogleTranslateService;
import me.verschillend.chatbabel.translation.LibreTranslateService;
import me.verschillend.chatbabel.translation.TranslationException;
import me.verschillend.chatbabel.translation.TranslationResult;
import me.verschillend.chatbabel.translation.TranslationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslatorScreen extends Screen {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chatbabel-translator-gui");
        t.setDaemon(true);
        return t;
    });
    private final GoogleTranslateService googleService = new GoogleTranslateService();
    private final LibreTranslateService libreService =
            new LibreTranslateService(() -> ChatBabelConfigHolder.getConfig().libreTranslateUrl);

    private TextFieldWidget leftField;
    private TextFieldWidget rightField;
    private net.minecraft.client.gui.widget.ButtonWidget leftLangButton;
    private net.minecraft.client.gui.widget.ButtonWidget rightLangButton;
    private Language leftLanguage = Language.ENGLISH;
    private Language rightLanguage = ChatBabelConfigHolder.getConfig().nativeLanguage;

    private volatile boolean programmaticUpdate = false;
    private long lastLeftEditMillis = 0;
    private long lastRightEditMillis = 0;
    private boolean leftDirty = false;
    private boolean rightDirty = false;

    private static final long DEBOUNCE_MILLIS = 500;

    public TranslatorScreen() {
        super(Text.literal("ChatBabel Translator"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 200;

        leftLangButton = net.minecraft.client.gui.widget.ButtonWidget
                .builder(Text.literal(leftLanguage.name()), button -> {
                    leftLanguage = nextLanguage(leftLanguage);
                    button.setMessage(Text.literal(leftLanguage.name()));
                    leftDirty = true;
                    lastLeftEditMillis = System.currentTimeMillis();
                })
                .dimensions(centerX - fieldWidth - 10, 40, fieldWidth, 20)
                .build();
        addDrawableChild(leftLangButton);

        rightLangButton = net.minecraft.client.gui.widget.ButtonWidget
                .builder(Text.literal(rightLanguage.name()), button -> {
                    rightLanguage = nextLanguage(rightLanguage);
                    button.setMessage(Text.literal(rightLanguage.name()));
                    rightDirty = true;
                    lastRightEditMillis = System.currentTimeMillis();
                })
                .dimensions(centerX + 10, 40, fieldWidth, 20)
                .build();
        addDrawableChild(rightLangButton);

        leftField = new TextFieldWidget(this.textRenderer, centerX - fieldWidth - 10, 70, fieldWidth, 100, Text.literal("Left"));
        leftField.setMaxLength(500);
        leftField.setChangedListener(text -> {
            if (programmaticUpdate) return;
            lastLeftEditMillis = System.currentTimeMillis();
            leftDirty = true;
        });
        addDrawableChild(leftField);

        rightField = new TextFieldWidget(this.textRenderer, centerX + 10, 70, fieldWidth, 100, Text.literal("Right"));
        rightField.setMaxLength(500);
        rightField.setChangedListener(text -> {
            if (programmaticUpdate) return;
            lastRightEditMillis = System.currentTimeMillis();
            rightDirty = true;
        });
        addDrawableChild(rightField);
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (leftDirty && now - lastLeftEditMillis > DEBOUNCE_MILLIS) {
            leftDirty = false;
            translate(leftField.getText(), leftLanguage, rightLanguage, rightField);
        }
        if (rightDirty && now - lastRightEditMillis > DEBOUNCE_MILLIS) {
            rightDirty = false;
            translate(rightField.getText(), rightLanguage, leftLanguage, leftField);
        }
    }

    private void translate(String text, Language from, Language to, TextFieldWidget target) {
        if (text == null || text.isBlank()) {
            return;
        }
        TranslationService service = ChatBabelConfigHolder.getConfig().translationProvider == TranslationProvider.LIBRETRANSLATE
                ? libreService : googleService;
        executor.submit(() -> {
            try {
                TranslationResult result = service.translate(text, to.code());
                MinecraftClient.getInstance().execute(() -> {
                    programmaticUpdate = true;
                    target.setText(result.translatedText());
                    programmaticUpdate = false;
                });
            } catch (TranslationException ignored) {
                // silently skip - user can just keep typing
            }
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010); // plain translucent overlay, no blur
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static Language nextLanguage(Language current) {
        Language[] values = Language.values();
        int next = (current.ordinal() + 1) % values.length;
        return values[next];
    }
}