package me.verschillend.chatbabel.mixin.client;

import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.pipeline.HudTextTranslator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(InGameHud.class)
public abstract class InGameHudTextMixin {

    @ModifyVariable(method = "setTitle", at = @At("HEAD"), argsOnly = true)
    private Text chatbabel$translateTitle(Text title) {
        HudTextTranslator.INSTANCE.translateAsync(title, ChatBabelConfigHolder.getConfig().translateTitle,
                translated -> MinecraftClient.getInstance().inGameHud.setTitle(translated));
        return title;
    }

    @ModifyVariable(method = "setSubtitle", at = @At("HEAD"), argsOnly = true)
    private Text chatbabel$translateSubtitle(Text subtitle) {
        HudTextTranslator.INSTANCE.translateAsync(subtitle, ChatBabelConfigHolder.getConfig().translateSubtitle,
                translated -> MinecraftClient.getInstance().inGameHud.setSubtitle(translated));
        return subtitle;
    }

    @ModifyVariable(method = "setOverlayMessage", at = @At("HEAD"), argsOnly = true)
    private Text chatbabel$translateActionBar(Text message) {
        HudTextTranslator.INSTANCE.translateAsync(message, ChatBabelConfigHolder.getConfig().translateActionBar,
                translated -> MinecraftClient.getInstance().inGameHud.setOverlayMessage(translated, false));
        return message;
    }
}
