package com.example.skullbeacon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configurações salvas em config/skullbeacon.json */
public final class SkullBeaconConfig {
    public static final int MIN_HIDE_DISTANCE = 1;
    public static final int MAX_HIDE_DISTANCE = 20;
    public static final int MIN_SKULL_SIZE = 50;
    public static final int MAX_SKULL_SIZE = 200;

    /** Onde mostrar a distância até a caveira. */
    public enum DistanceMode { OFF, HUD, SKULL }

    public boolean enabled = true;
    public boolean showDeathMessage = true;
    public DistanceMode distanceMode = DistanceMode.HUD;
    public int hideDistance = 3;
    public boolean bobbing = false;
    public boolean seeThroughWalls = true;
    public int skullSize = 100; // porcentagem

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SkullBeaconConfig instance = new SkullBeaconConfig();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("skullbeacon.json");
    }

    public static SkullBeaconConfig get() {
        return instance;
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                SkullBeaconConfig loaded = GSON.fromJson(r, SkullBeaconConfig.class);
                if (loaded != null) {
                    loaded.hideDistance = Math.max(MIN_HIDE_DISTANCE,
                            Math.min(MAX_HIDE_DISTANCE, loaded.hideDistance));
                    loaded.skullSize = Math.max(MIN_SKULL_SIZE,
                            Math.min(MAX_SKULL_SIZE, loaded.skullSize));
                    if (loaded.distanceMode == null) loaded.distanceMode = DistanceMode.HUD;
                    instance = loaded;
                }
            } catch (IOException | RuntimeException ignored) {
                // arquivo inválido: mantém os valores padrão
            }
        }
        save();
    }

    public static void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException ignored) {
            // sem permissão de escrita: ignora
        }
    }
}
