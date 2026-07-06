package me.verschillend.chatbabel.mixin.client;

import me.verschillend.chatbabel.config.ChatBabelConfigHolder;
import me.verschillend.chatbabel.pipeline.HudTextTranslator;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ScoreboardObjective.class)
public abstract class ScoreboardObjectiveMixin {

    @ModifyVariable(method = "setDisplayName", at = @At("HEAD"), argsOnly = true)
    private Text chatbabel$translateObjectiveTitle(Text displayName) {
        HudTextTranslator.INSTANCE.translateAsync(displayName, ChatBabelConfigHolder.getConfig().translateScoreboard,
                translated -> ((ScoreboardObjective) (Object) this).setDisplayName(translated));
        return displayName;
    }
}