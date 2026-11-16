package ru.visuals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class Feature {
    public final String id, name, category;
    public final List<Setting> settings = new ArrayList<>();
    public final Setting.Bool enabled; // null, если у функции нет главного переключателя
    public float anim;
    private int infoCounter = 0;

    public Feature(String id, String name, String category, boolean toggle, boolean def) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.enabled = toggle ? new Setting.Bool(id + ".enabled", "enabled", def) : null;
        this.anim = def ? 1f : 0f;
    }

    public boolean on() {
        return enabled == null || enabled.value;
    }

    private <T extends Setting> T add(T s) {
        settings.add(s);
        return s;
    }

    public Setting.Bool bool(String sid, String n, boolean def) {
        return add(new Setting.Bool(id + "." + sid, n, def));
    }

    public Setting.Num num(String sid, String n, double min, double max, double step, double def, String unit) {
        return add(new Setting.Num(id + "." + sid, n, min, max, step, def, unit));
    }

    public Setting.Mode mode(String sid, String n, int def, String... options) {
        return add(new Setting.Mode(id + "." + sid, n, def, options));
    }

    public Setting.Color color(String sid, String n, int def) {
        return add(new Setting.Color(id + "." + sid, n, def));
    }

    public Setting.Action action(String n, Runnable r) {
        return add(new Setting.Action(id + ".act." + (infoCounter++), n, r));
    }

    public Setting.Info info(Supplier<String> t) {
        return add(new Setting.Info(id + ".info." + (infoCounter++), t));
    }

    public boolean matches(String q) {
        if (name.toLowerCase(Locale.ROOT).contains(q)) return true;
        for (Setting s : settings) {
            if (s.name.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }
}
