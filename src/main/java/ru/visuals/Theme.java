package ru.visuals;

import java.util.Arrays;

public final class Theme {
    private Theme() { }

    public record P(String name, int bg, int accent, int text, int sub, int card, int line, boolean light) { }

    public static final P[] LIST = {
            new P("Тёмная", 0x0F0F18, 0x9B5CFF, 0xFFF2F2F8, 0xFF8E8EA3, 0x14FFFFFF, 0x22FFFFFF, false),
            new P("Океан", 0x08151F, 0x3FA9FF, 0xFFEAF4FF, 0xFF7F9BB3, 0x14FFFFFF, 0x22FFFFFF, false),
            new P("Закат", 0x1A0E12, 0xFF7A59, 0xFFFFF0EC, 0xFFB08F88, 0x14FFFFFF, 0x22FFFFFF, false),
            new P("Мята", 0x0A1613, 0x3DDC97, 0xFFEAFFF5, 0xFF7FAE9C, 0x14FFFFFF, 0x22FFFFFF, false),
            new P("Роза", 0x180C15, 0xFF5CA8, 0xFFFFEEF6, 0xFFB08AA3, 0x14FFFFFF, 0x22FFFFFF, false),
            new P("Светлая", 0xEEF0F8, 0x6A5CFF, 0xFF1B1B28, 0xFF6A6A80, 0x12000000, 0x26000000, true),
            new P("Моно", 0x121212, 0xE6E6E6, 0xFFF5F5F5, 0xFF909090, 0x14FFFFFF, 0x22FFFFFF, false),
    };

    public static final String[] NAMES = Arrays.stream(LIST).map(P::name).toArray(String[]::new);

    public static final int[] SWATCHES = {
            0x9B5CFF, 0x3FA9FF, 0x3DDC97, 0xFFC857, 0xFF8A4C, 0xFF5C7A, 0xFF5CC8, 0xE8E8F0
    };

    public static P cur() {
        int i = Features.T_THEME.index;
        return LIST[Math.max(0, Math.min(LIST.length - 1, i))];
    }

    /** RGB-цвет акцента (без альфы). */
    public static int accent() {
        int i = Features.T_ACCENT.index;
        if (i <= 0) return cur().accent();
        return SWATCHES[Math.min(i, SWATCHES.length) - 1];
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Цвет из палитры настройки (индекс 0 = акцент темы). */
    public static int pick(Setting.Color c) {
        int i = c.index;
        if (i <= 0) return accent();
        return SWATCHES[Math.min(i, SWATCHES.length) - 1];
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6);
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (((int) ((r + m) * 255)) << 16) | (((int) ((g + m) * 255)) << 8) | ((int) ((b + m) * 255));
    }

    public static int rainbow(float time, float offset) {
        return hsv(time * 0.15f + offset, 0.65f, 1f);
    }
}
