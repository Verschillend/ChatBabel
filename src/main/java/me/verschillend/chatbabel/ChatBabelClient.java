package me.verschillend.chatbabel;

import me.verschillend.chatbabel.config.ChatBabelConfig;
import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.pipeline.ChatMessageProcessor;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ChatBabelClient implements ClientModInitializer {

    private static KeyBinding openConfigKeyBinding;
    private final ChatMessageProcessor processor = new ChatMessageProcessor();
    private static KeyBinding openTranslatorKeyBinding;
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("chatbabel", "main"));

    @Override
    public void onInitializeClient() {
        ChatBabelConfigHolder.init();

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            processor.processAsync(message, processed ->
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(processed));
            return false;
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) {
                return true;
            }
            processor.processAsync(message, translated ->
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(translated));
            return false;
        });

        openConfigKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatbabel.openconfig",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                CATEGORY
        ));

        openTranslatorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatbabel.opentranslator",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKeyBinding.wasPressed()) {
                openConfigScreen(client);
            }
            while (openTranslatorKeyBinding.wasPressed()) {
                client.setScreen(new me.verschillend.chatbabel.gui.TranslatorScreen());
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("chatbabel")
                        .executes(context -> {
                            openConfigScreen(MinecraftClient.getInstance());
                            return 1;
                        })));

        ChatBabel.LOGGER.info("ChatBabel initialized.");
    }

    @SuppressWarnings("deprecation")
    private static void openConfigScreen(MinecraftClient client) {
        client.setScreen(AutoConfig.getConfigScreen(ChatBabelConfig.class, client.currentScreen).get());
    }
}
