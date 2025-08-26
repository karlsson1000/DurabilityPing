package org.karlssonsmp.durabilityping.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigScreen implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::createConfigScreen;
    }

    public static Screen createConfigScreen(Screen parent) {
        DurabilityPingConfig config = DurabilityPingConfig.getInstance();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.durability-ping.config"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory settings = builder.getOrCreateCategory(Text.translatable("category.durability-ping.settings"));

        settings.addEntry(entryBuilder.startDoubleField(Text.translatable("option.durability-ping.threshold"), config.threshold)
                .setDefaultValue(0.10)
                .setMin(0.01)
                .setMax(1.00)
                .setTooltip(Text.translatable("tooltip.durability-ping.threshold"))
                .setSaveConsumer(newValue -> config.threshold = newValue)
                .build());

        settings.addEntry(entryBuilder.startBooleanToggle(Text.translatable("option.durability-ping.enable_sound"), config.enableSound)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("tooltip.durability-ping.enable_sound"))
                .setSaveConsumer(newValue -> config.enableSound = newValue)
                .build());

        return builder.build();
    }
}