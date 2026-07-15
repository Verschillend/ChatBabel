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

import org.lwjgl.glfw.GLFW;

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

    private volatile boolean programmaticUpdate = false;
    private long lastLeftEditMillis = 0;
    private long lastRightEditMillis = 0;
    private boolean leftDirty = false;
    private boolean rightDirty = false;

    private boolean prevMouseDown = false;

    private static final long DEBOUNCE_MILLIS = 500;

    public TranslatorScreen() {
        super(Text.literal("ChatBabel Translator"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int fieldWidth = 200;

        leftDropdown.x = centerX - fieldWidth - 10;
        leftDropdown.y = 40;
        leftDropdown.width = fieldWidth;
        leftDropdown.height = 20;

        rightDropdown.x = centerX + 10;
        rightDropdown.y = 40;
        rightDropdown.width = fieldWidth;
        rightDropdown.height = 20;

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
            translate(leftField.getText(), leftDropdown.selected, rightDropdown.selected, rightField);
        }
        if (rightDirty && now - lastRightEditMillis > DEBOUNCE_MILLIS) {
            rightDirty = false;
            translate(rightField.getText(), rightDropdown.selected, leftDropdown.selected, leftField);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !prevMouseDown) {
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            GLFW.glfwGetCursorPos(handle, xpos, ypos);
            double scaleX = (double) client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double scaleY = (double) client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            handleClick((int) (xpos[0] * scaleX), (int) (ypos[0] * scaleY));
        }
        prevMouseDown = down;
    }

    private void handleClick(int mx, int my) {
        if (leftDropdown.open && tryPickFromList(leftDropdown, mx, my)) {
            leftDirty = true;
            lastLeftEditMillis = System.currentTimeMillis();
            return;
        }
        if (rightDropdown.open && tryPickFromList(rightDropdown, mx, my)) {
            rightDirty = true;
            lastRightEditMillis = System.currentTimeMillis();
            return;
        }
        if (LanguageDropdown.contains(mx, my, leftDropdown.x, leftDropdown.y, leftDropdown.width, leftDropdown.height)) {
            leftDropdown.open = !leftDropdown.open;
            rightDropdown.open = false;
            return;
        }
        if (LanguageDropdown.contains(mx, my, rightDropdown.x, rightDropdown.y, rightDropdown.width, rightDropdown.height)) {
            rightDropdown.open = !rightDropdown.open;
            leftDropdown.open = false;
            return;
        }
        leftDropdown.open = false;
        rightDropdown.open = false;
    }

    private boolean tryPickFromList(LanguageDropdown dropdown, int mx, int my) {
        Language[] values = Language.values();
        boolean scrollable = values.length > LanguageDropdown.VISIBLE_ROWS;
        int listY = dropdown.y + dropdown.height;
        int arrowH = scrollable ? 10 : 0;
        int rowsShown = Math.min(LanguageDropdown.VISIBLE_ROWS, values.length);
        int lh = arrowH * 2 + rowsShown * LanguageDropdown.ROW_HEIGHT;

        if (!LanguageDropdown.contains(mx, my, dropdown.x, listY, dropdown.width, lh)) {
            return false;
        }

        int relativeY = my - listY;
        if (scrollable && relativeY < arrowH) {
            dropdown.scrollOffset = Math.max(0, dropdown.scrollOffset - 1);
            return true;
        }
        int afterUp = scrollable ? arrowH : 0;
        if (scrollable && relativeY >= afterUp + rowsShown * LanguageDropdown.ROW_HEIGHT) {
            int maxScroll = Math.max(0, values.length - LanguageDropdown.VISIBLE_ROWS);
            dropdown.scrollOffset = Math.min(maxScroll, dropdown.scrollOffset + 1);
            return true;
        }

        int row = (relativeY - afterUp) / LanguageDropdown.ROW_HEIGHT;
        int index = dropdown.scrollOffset + row;
        if (index >= 0 && index < values.length) {
            dropdown.selected = values[index];
            dropdown.open = false;
            return true;
        }
        return false;
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
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
        leftDropdown.render(context, this.textRenderer, mouseX, mouseY);
        rightDropdown.render(context, this.textRenderer, mouseX, mouseY);
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

    private final LanguageDropdown leftDropdown = new LanguageDropdown(Language.ENGLISH);
    private final LanguageDropdown rightDropdown = new LanguageDropdown(ChatBabelConfigHolder.getConfig().nativeLanguage);

    private static final class LanguageDropdown {
        static final int ROW_HEIGHT = 18;
        static final int VISIBLE_ROWS = 8;

        int x, y, width, height;
        boolean open = false;
        int scrollOffset = 0;
        Language selected;

        LanguageDropdown(Language initial) {
            this.selected = initial;
        }

        int listHeight() {
            return Math.min(VISIBLE_ROWS, Language.values().length) * ROW_HEIGHT;
        }

        void render(net.minecraft.client.gui.DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int mouseX, int mouseY) {
            boolean buttonHovered = contains(mouseX, mouseY, x, y, width, height);
            context.fill(x, y, x + width, y + height, buttonHovered ? 0xFF5A5A5A : 0xFF3A3A3A);
            drawBoxBorder(context, x, y, width, height);
            context.drawText(textRenderer, Text.literal(displayName(selected)), x + 6, y + (height - 8) / 2, 0xFFFFFFFF, true);
            context.drawText(textRenderer, Text.literal(open ? "^" : "v"), x + width - 14, y + (height - 8) / 2, 0xFFFFFFFF, true);

            if (!open) return;

            Language[] values = Language.values();
            boolean scrollable = values.length > VISIBLE_ROWS;
            int listY = y + height;
            int rowsShown = Math.min(VISIBLE_ROWS, values.length);
            int arrowH = scrollable ? 10 : 0;
            int lh = arrowH * 2 + rowsShown * ROW_HEIGHT;

            context.fill(x, listY, x + width, listY + lh, 0xE0101010);

            int cursorY = listY;
            if (scrollable) {
                boolean upHovered = contains(mouseX, mouseY, x, cursorY, width, arrowH);
                context.fill(x, cursorY, x + width, cursorY + arrowH, upHovered ? 0xFF505050 : 0xFF303030);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("^"), x + width / 2, cursorY + 1, 0xFFFFFFFF);
                cursorY += arrowH;
            }

            for (int i = 0; i < rowsShown; i++) {
                int index = scrollOffset + i;
                if (index >= values.length) break;
                int rowY = cursorY + i * ROW_HEIGHT;
                if (contains(mouseX, mouseY, x, rowY, width, ROW_HEIGHT)) {
                    context.fill(x, rowY, x + width, rowY + ROW_HEIGHT, 0xFF4A4A4A);
                }
                context.drawText(textRenderer, Text.literal(displayName(values[index])), x + 6, rowY + (ROW_HEIGHT - 8) / 2, 0xFFFFFFFF, true);
            }

            if (scrollable) {
                int downY = cursorY + rowsShown * ROW_HEIGHT;
                boolean downHovered = contains(mouseX, mouseY, x, downY, width, arrowH);
                context.fill(x, downY, x + width, downY + arrowH, downHovered ? 0xFF505050 : 0xFF303030);
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("v"), x + width / 2, downY + 1, 0xFFFFFFFF);
            }

            drawBoxBorder(context, x, listY, width, lh);
        }

        private static void drawBoxBorder(net.minecraft.client.gui.DrawContext context, int bx, int by, int bw, int bh) {
            context.fill(bx, by, bx + bw, by + 1, 0xFFAAAAAA);
            context.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFFAAAAAA);
            context.fill(bx, by, bx + 1, by + bh, 0xFFAAAAAA);
            context.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFFAAAAAA);
        }

        static boolean contains(int px, int py, int x, int y, int w, int h) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }

        static String displayName(Language lang) {
            String[] parts = lang.name().toLowerCase(java.util.Locale.ROOT).split("_");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            return sb.toString();
        }
    }
}