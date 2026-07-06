package me.verschillend.chatbabel.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;


public final class ChatBabelConfigHolder {

    public static final String CONFIG_NAME = "chatbabel";

    private static ConfigHolder<ChatBabelConfig> holder;

    private ChatBabelConfigHolder() {
    }

    public static void init() {
        holder = AutoConfig.register(ChatBabelConfig.class, GsonConfigSerializer::new);
    }

    public static ChatBabelConfig getConfig() {
        return holder.getConfig();
    }

    public static ConfigHolder<ChatBabelConfig> getHolder() {
        return holder;
    }
}
