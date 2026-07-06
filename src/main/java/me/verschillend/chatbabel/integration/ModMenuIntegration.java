package me.verschillend.chatbabel.integration;

import me.verschillend.chatbabel.config.ChatBabelConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

/**
 * Wires ChatBabel's config screen into Mod Menu's mod list. This class is
 * declared under the "modmenu" entrypoint in fabric.mod.json and is only
 * ever instantiated if Mod Menu is actually installed - if it isn't, Fabric
 * Loader never looks for or loads this class, so the compileOnly Mod Menu
 * dependency never needs to be present at runtime.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(ChatBabelConfig.class, parent).get();
    }
}
