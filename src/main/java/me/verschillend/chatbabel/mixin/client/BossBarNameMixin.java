package me.verschillend.chatbabel.mixin.client;

import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.pipeline.HudTextTranslator;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BossBar.class)
public abstract class BossBarNameMixin {

    @ModifyVariable(method = "setName", at = @At("HEAD"), argsOnly = true)
    private Text chatbabel$translateBossBarName(Text name) {
        if (!(((Object) this) instanceof ClientBossBar)) {
            return name;
        }
        HudTextTranslator.INSTANCE.translateAsync(name, ChatBabelConfigHolder.getConfig().translateBossBar,
                translated -> ((BossBar) (Object) this).setName(translated));
        return name;
    }
}
