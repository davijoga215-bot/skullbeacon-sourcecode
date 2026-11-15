package com.example.skullbeacon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Guarda a última caveira de cada mundo/servidor em config/skullbeacon-skulls.json */
final class SkullStore {
    static final class Entry {
        String dimension;
        double x;
        double y;
        double z;
    }

    private static final class Data {
        Map<String, Entry> skulls = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data = read();

    private SkullStore() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("skullbeacon-skulls.json");
    }

    private static Data read() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d != null && d.skulls != null) return d;
            } catch (IOException | RuntimeException ignored) {
                // arquivo inválido: começa vazio
            }
        }
        return new Data();
    }

    private static void write() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(data, w);
            }
        } catch (IOException ignored) {
            // sem permissão de escrita: ignora
        }
    }

    static Entry get(String key) {
        return data.skulls.get(key);
    }

    static void put(String key, String dimension, Vec3d pos) {
        Entry e = new Entry();
        e.dimension = dimension;
        e.x = pos.x;
        e.y = pos.y;
        e.z = pos.z;
        data.skulls.put(key, e);
        write();
    }

    static void remove(String key) {
        if (data.skulls.remove(key) != null) write();
    }
}
