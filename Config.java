package ru.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    public static int menuKey = -1; // -1 = не назначена

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visuals.json");

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            Map<String, Object> m = GSON.fromJson(Files.readString(FILE),
                    new TypeToken<Map<String, Object>>() { }.getType());
            if (m == null) return;
            Object mk = m.get("menuKey");
            if (mk instanceof Number n) menuKey = n.intValue();
            for (Feature f : Features.ALL) {
                if (f.enabled != null && m.containsKey(f.enabled.id)) {
                    f.enabled.load(m.get(f.enabled.id));
                    f.anim = f.enabled.value ? 1f : 0f;
                }
                for (Setting s : f.settings) {
                    if (s.raw() != null && m.containsKey(s.id)) s.load(m.get(s.id));
                }
            }
            syncAnims();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Подгоняет значения анимаций под загруженные настройки, чтобы меню не «ехало» при первом открытии. */
    public static void syncAnims() {
        for (Feature f : Features.ALL) {
            if (f.enabled != null) f.anim = f.enabled.value ? 1f : 0f;
            for (Setting s : f.settings) {
                if (s instanceof Setting.Bool b) b.anim = b.value ? 1f : 0f;
                else if (s instanceof Setting.Num n) n.anim = (float) ((n.value - n.min) / (n.max - n.min));
                else if (s instanceof Setting.Color c) c.anim = c.index;
            }
        }
    }

    public static void save() {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("menuKey", menuKey);
            for (Feature f : Features.ALL) {
                if (f.enabled != null) m.put(f.enabled.id, f.enabled.raw());
                for (Setting s : f.settings) {
                    Object raw = s.raw();
                    if (raw != null) m.put(s.id, raw);
                }
            }
            Files.writeString(FILE, GSON.toJson(m));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
