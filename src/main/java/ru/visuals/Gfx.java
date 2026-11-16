package ru.visuals;

import net.minecraft.client.gui.DrawContext;

/** Рисование скруглённых прямоугольников и эффекта «жидкого стекла». */
public final class Gfx {
    private Gfx() { }

    public static int mul(int argb, float f) {
        f = Math.max(0f, Math.min(1f, f));
        int a = (int) (((argb >>> 24) & 0xFF) * f);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int argb(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int inset(int r, int row) {
        double d = r - row - 0.5;
        double x = Math.sqrt(Math.max(0, (double) r * r - d * d));
        return (int) Math.round(r - x);
    }

    public static void roundRect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        roundGradient(c, x1, y1, x2, y2, r, color, color);
    }

    public static void roundGradient(DrawContext c, int x1, int y1, int x2, int y2, int r, int top, int bottom) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (top == bottom) {
            if (r == 0) {
                c.fill(x1, y1, x2, y2, top);
                return;
            }
            for (int row = 0; row < r; row++) {
                int ins = inset(r, row);
                c.fill(x1 + ins, y1 + row, x2 - ins, y1 + row + 1, top);
                c.fill(x1 + ins, y2 - row - 1, x2 - ins, y2 - row, top);
            }
            if (h > 2 * r) c.fill(x1, y1 + r, x2, y2 - r, top);
            return;
        }
        for (int row = 0; row < h; row++) {
            int ins = 0;
            if (row < r) ins = inset(r, row);
            else if (row >= h - r) ins = inset(r, h - 1 - row);
            if (x2 - ins <= x1 + ins) continue;
            int col = lerp(top, bottom, (row + 0.5f) / h);
            c.fill(x1 + ins, y1 + row, x2 - ins, y1 + row + 1, col);
        }
    }

    /** Контур в 1 пиксель со скруглёнными углами. */
    public static void stroke(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        int w = x2 - x1, h = y2 - y1;
        if (w < 2 || h < 2) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x1 + r, y1, x2 - r, y1 + 1, color);
        c.fill(x1 + r, y2 - 1, x2 - r, y2, color);
        c.fill(x1, y1 + r, x1 + 1, y2 - r, color);
        c.fill(x2 - 1, y1 + r, x2, y2 - r, color);
        if (r == 0) return;
        int steps = Math.max(6, r * 4);
        for (int k = 0; k < 4; k++) {
            boolean left = (k == 0 || k == 3);
            boolean top = (k < 2);
            int cx = left ? x1 + r : x2 - r;
            int cy = top ? y1 + r : y2 - r;
            double sx = left ? -1 : 1;
            double sy = top ? -1 : 1;
            int lx = Integer.MIN_VALUE, ly = Integer.MIN_VALUE;
            for (int i = 0; i <= steps; i++) {
                double a = Math.PI / 2 * i / steps;
                int px = (int) Math.floor(cx + sx * Math.cos(a) * (r - 0.5));
                int py = (int) Math.floor(cy + sy * Math.sin(a) * (r - 0.5));
                if (px != lx || py != ly) {
                    c.fill(px, py, px + 1, py + 1, color);
                    lx = px;
                    ly = py;
                }
            }
        }
    }

    /**
     * «Жидкое стекло»: полупрозрачная заливка с градиентом, верхний свет,
     * тонкая светлая обводка, блик по верхней кромке и бегущий отсвет.
     */
    public static void glass(DrawContext c, int x1, int y1, int x2, int y2, int r, float opacity, float ga, boolean big) {
        Theme.P th = Theme.cur();
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        int a = (int) (255 * Math.max(0f, Math.min(1f, opacity)) * Math.max(0f, Math.min(1f, ga)));
        int topRgb = Theme.mix(th.bg(), 0xFFFFFF, th.light() ? 0.55f : 0.14f);
        roundGradient(c, x1, y1, x2, y2, r, argb(topRgb, a), argb(th.bg(), a));

        if (Features.T_GLOW.value) {
            int gh = Math.max(4, h * (big ? 40 : 55) / 100);
            int ga2 = (int) ((big ? 0x2C : 0x24) * ga);
            roundGradient(c, x1 + 1, y1 + 1, x2 - 1, y1 + 1 + gh, Math.max(0, r - 1),
                    argb(0xFFFFFF, ga2), argb(0xFFFFFF, 0));
        }

        if (Features.T_BORDER.value) {
            float b = Features.T_BORDER_STR.getf() / 100f;
            int base = th.light() ? 0x000000 : 0xFFFFFF;
            int ba = (int) ((th.light() ? 0x40 : 0x78) * b * ga);
            stroke(c, x1, y1, x2, y2, r, argb(base, ba));
            // блик по верхней кромке: яркий слева, затухает вправо
            int seg = Math.max(2, (w - 2 * r) / 12);
            for (int k = 0; k < 6; k++) {
                int sa = (int) (Math.min(255, ba * (2.2f - k * 0.38f)) * 1f);
                if (sa <= 0) break;
                int sx1 = x1 + r + k * seg;
                int sx2 = Math.min(x2 - r, sx1 + seg);
                if (sx2 > sx1) c.fill(sx1, y1, sx2, y1 + 1, argb(0xFFFFFF, Math.min(255, sa)));
            }
        }

        if (big && Features.T_SHEEN.value) {
            float time = (System.nanoTime() / 1_000_000_000f);
            float t = (time * 0.12f) % 1.7f - 0.35f;
            int bx = x1 + (int) (t * w);
            float str = Features.T_SHEEN_STR.getf() / 100f;
            int strips = 12, sw = 5;
            for (int k = 0; k < strips; k++) {
                int sx1 = bx + k * sw;
                int sx2 = sx1 + sw;
                sx1 = Math.max(sx1, x1 + r);
                sx2 = Math.min(sx2, x2 - r);
                if (sx2 <= sx1) continue;
                float prof = 1f - Math.abs(k - (strips - 1) / 2f) / (strips / 2f);
                int sa = (int) (0x1E * str * prof * ga);
                if (sa > 0) c.fill(sx1, y1 + 1, sx2, y2 - 1, argb(0xFFFFFF, sa));
            }
        }
    }
}
