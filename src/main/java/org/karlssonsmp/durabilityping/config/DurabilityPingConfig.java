package org.karlssonsmp.durabilityping.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DurabilityPingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "durability-ping.json");

    private static DurabilityPingConfig INSTANCE;

    public double threshold = 0.10; // 10% default
    public boolean enableSound = true;

    public static DurabilityPingConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static DurabilityPingConfig load() {
        if (!CONFIG_FILE.exists()) {
            DurabilityPingConfig config = new DurabilityPingConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, DurabilityPingConfig.class);
        } catch (IOException e) {
            return new DurabilityPingConfig();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}