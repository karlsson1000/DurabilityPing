package org.karlssonsmp.durabilityping.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::createConfigScreen;
    }

    public static Screen createConfigScreen(Screen parent) {
        DurabilityPingConfig config = DurabilityPingConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.durability-ping.config"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory settings = builder.getOrCreateCategory(Component.translatable("category.durability-ping.settings"));

        settings.addEntry(entryBuilder.startDoubleField(Component.translatable("option.durability-ping.threshold"), config.threshold)
                .setDefaultValue(0.10)
                .setMin(0.01)
                .setMax(1.00)
                .setTooltip(Component.translatable("tooltip.durability-ping.threshold"))
                .setSaveConsumer(newValue -> config.threshold = newValue)
                .build());

        settings.addEntry(entryBuilder.startBooleanToggle(Component.translatable("option.durability-ping.enable_sound"), config.enableSound)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("tooltip.durability-ping.enable_sound"))
                .setSaveConsumer(newValue -> config.enableSound = newValue)
                .build());

        return builder.build();
    }
}