package ru.visuals;

import java.util.Locale;
import java.util.function.Supplier;

public abstract class Setting {
    public final String id;
    public final String name;
    public float anim = 0f;
    public Supplier<Boolean> visible = () -> true;

    protected Setting(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Setting showIf(Supplier<Boolean> v) {
        this.visible = v;
        return this;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public Object raw() { return null; }
    public void load(Object o) { }
    public void reset() { }

    // ---------------- Bool ----------------
    public static class Bool extends Setting {
        public boolean value;
        private final boolean def;

        public Bool(String id, String name, boolean def) {
            super(id, name);
            this.value = def;
            this.def = def;
            this.anim = def ? 1f : 0f;
        }

        @Override public Object raw() { return value; }
        @Override public void load(Object o) { if (o instanceof Boolean b) value = b; }
        @Override public void reset() { value = def; }
    }

    // ---------------- Num ----------------
    public static class Num extends Setting {
        public double value;
        public final double min, max, step, def;
        public final String unit;

        public Num(String id, String name, double min, double max, double step, double def, String unit) {
            super(id, name);
            this.min = min;
            this.max = max;
            this.step = step;
            this.def = def;
            this.unit = unit;
            this.value = def;
            this.anim = (float) ((def - min) / (max - min));
        }

        public double get() { return value; }
        public float getf() { return (float) value; }
        public int geti() { return (int) Math.round(value); }

        public void set(double v) {
            v = Math.round(v / step) * step;
            value = Math.max(min, Math.min(max, v));
        }

        public String text() {
            String s;
            if (step >= 1) s = String.valueOf(Math.round(value));
            else if (step >= 0.1) s = String.format(Locale.ROOT, "%.1f", value);
            else s = String.format(Locale.ROOT, "%.2f", value);
            return s + unit;
        }

        @Override public Object raw() { return value; }
        @Override public void load(Object o) { if (o instanceof Number n) set(n.doubleValue()); }
        @Override public void reset() { value = def; }
    }

    // ---------------- Mode ----------------
    public static class Mode extends Setting {
        public int index;
        public final String[] options;
        private final int def;

        public Mode(String id, String name, int def, String... options) {
            super(id, name);
            this.options = options;
            this.def = def;
            this.index = def;
        }

        public void cycle(int dir) {
            index = Math.floorMod(index + dir, options.length);
        }

        @Override public Object raw() { return index; }
        @Override public void load(Object o) {
            if (o instanceof Number n) index = Math.max(0, Math.min(options.length - 1, n.intValue()));
        }
        @Override public void reset() { index = def; }
    }

    // ---------------- Color (индекс 0 = из темы) ----------------
    public static class Color extends Setting {
        public int index;
        private final int def;

        public Color(String id, String name, int def) {
            super(id, name);
            this.def = def;
            this.index = def;
            this.anim = def;
        }

        @Override public Object raw() { return index; }
        @Override public void load(Object o) {
            if (o instanceof Number n) index = Math.max(0, Math.min(Theme.SWATCHES.length, n.intValue()));
        }
        @Override public void reset() { index = def; }
    }

    // ---------------- Action ----------------
    public static class Action extends Setting {
        public final Runnable run;

        public Action(String id, String name, Runnable run) {
            super(id, name);
            this.run = run;
        }
    }

    // ---------------- Info ----------------
    public static class Info extends Setting {
        public final Supplier<String> text;

        public Info(String id, Supplier<String> text) {
            super(id, "");
            this.text = text;
        }
    }
}
