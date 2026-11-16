package ru.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MenuScreen extends Screen {
    private static final int SB_W = 104;
    private static final int HEADER = 20;

    private static final class Hit {
        final int type, x1, y1, x2, y2, idx;
        final Object ref;
        int tx1, tx2;

        Hit(int type, int x1, int y1, int x2, int y2, Object ref, int idx) {
            this.type = type;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.ref = ref;
            this.idx = idx;
        }
    }

    private int px, py, pw, ph;
    private int cat = 0;
    private float selY = -1f, contentAnim = 0f, scroll = 0f, scrollTarget = 0f, open = 0f;
    private boolean closing = false;
    private String query = "";
    private boolean searchFocus = false;
    private long last = System.nanoTime();
    private int totalH = 0, viewH = 0;
    private final List<Hit> hits = new ArrayList<>();
    private Hit dragging = null;

    private DrawContext g;
    private float ga = 1f;
    private float tfCx, tfCy, tfSc = 1f, tfOy = 0f;

    public MenuScreen() {
        super(Text.literal("Visuals"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------------- анимация ----------------

    private float ap(float cur, float target, float k, float dt) {
        if (!Features.T_ANIM.value) return target;
        float f = 1f - (float) Math.exp(-k * Features.T_SPEED.getf() * dt);
        float v = cur + (target - cur) * f;
        return Math.abs(target - v) < 0.002f ? target : v;
    }

    // ---------------- рисование-обёртки ----------------

    private void rr(int x1, int y1, int x2, int y2, int r, int col) {
        Gfx.roundRect(g, x1, y1, x2, y2, r, Gfx.mul(col, ga));
    }

    private void st(int x1, int y1, int x2, int y2, int r, int col) {
        Gfx.stroke(g, x1, y1, x2, y2, r, Gfx.mul(col, ga));
    }

    private void tx(String s, int x, int y, int col) {
        int c = Gfx.mul(col, ga);
        if (((c >>> 24) & 255) < 8) return; // при малой альфе текст рисуется непрозрачным
        g.drawText(textRenderer, s, x, y, c, false);
    }

    private int tw(String s) {
        return textRenderer.getWidth(s);
    }

    private String fit(String s, int maxW) {
        if (tw(s) <= maxW) return s;
        while (s.length() > 1 && tw(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private int sX(float x) {
        return Math.round(tfCx + (x - tfCx) * tfSc);
    }

    private int sY(float y) {
        return Math.round(tfCy + tfOy + (y - tfCy) * tfSc);
    }

    // ---------------- размеры строк ----------------

    private int pad() {
        return switch (Features.T_DENSITY.index) {
            case 0 -> -2;
            case 2 -> 3;
            default -> 0;
        };
    }

    private int base(Setting s) {
        if (s instanceof Setting.Num) return 24;
        if (s instanceof Setting.Color) return 28;
        if (s instanceof Setting.Action) return 18;
        if (s instanceof Setting.Info) return 14;
        return 16;
    }

    private int rowH(Setting s) {
        return Math.max(12, base(s) + pad());
    }

    private int cardHeight(Feature f) {
        int h = HEADER;
        for (Setting s : f.settings) {
            if (s.isVisible()) h += rowH(s);
        }
        return h + 6;
    }

    private Hit hit(int type, int x1, int y1, int x2, int y2, Object ref, int idx, int vt, int vb) {
        int a = Math.max(y1, vt), b = Math.min(y2, vb);
        if (a >= b) return null;
        Hit h = new Hit(type, x1, a, x2, b, ref, idx);
        hits.add(h);
        return h;
    }

    // ---------------- render ----------------

    @Override
    public void render(DrawContext ctx, int mx, int my, RenderTickCounter tickCounter) {
        float delta = tickCounter.getTickDelta(true);
        g = ctx;
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1_000_000_000f);
        last = now;

        open = ap(open, closing ? 0f : 1f, 16f, dt);
        if (closing && open <= 0.02f) {
            this.client.setScreen(null);
            return;
        }

        int sizeIdx = Features.T_SIZE.index;
        int bw = sizeIdx == 0 ? 380 : sizeIdx == 1 ? 440 : 520;
        int bh = sizeIdx == 0 ? 240 : sizeIdx == 1 ? 280 : 330;
        pw = Math.min(bw, width - 12);
        ph = Math.min(bh, height - 12);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        if (Features.T_BLUR.value) {
            super.renderBackground(ctx, mx, my, delta);
        } else {
            ctx.fill(0, 0, width, height, Gfx.mul(0x88000000, open));
        }

        int appear = Features.T_APPEAR.index;
        tfCx = px + pw / 2f;
        tfCy = py + ph / 2f;
        tfSc = appear == 0 ? 0.9f + 0.1f * open : 1f;
        tfOy = appear == 1 ? (1f - open) * 24f : 0f;

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(tfCx, tfCy + tfOy, 0f);
        ms.scale(tfSc, tfSc, 1f);
        ms.translate(-tfCx, -tfCy, 0f);

        ga = open;
        hits.clear();
        Theme.P th = Theme.cur();
        int acc = Theme.accent();
        int R = Features.T_RADIUS.geti();

        Gfx.glass(ctx, px, py, px + pw, py + ph, R, Features.T_OPACITY.getf() / 100f, ga, true);

        drawSidebar(th, acc, R, mx, my, dt);

        ctx.fill(px + SB_W, py + 8, px + SB_W + 1, py + ph - 8, Gfx.mul(th.line(), ga));

        drawContent(th, acc, mx, my, dt);

        ms.pop();
    }

    private void drawSidebar(Theme.P th, int acc, int R, int mx, int my, float dt) {
        int lx = px + 10, ly = py + 10;
        rr(lx, ly, lx + 20, ly + 14, Math.min(R, 5), 0xFF000000 | acc);
        tx("КК", lx + 4, ly + 3, 0xFFFFFFFF);
        tx("Visuals", lx + 26, ly + 3, th.text());

        // поиск
        int sx1 = px + 8, sy1 = py + 30, sx2 = px + SB_W - 8, sy2 = sy1 + 16;
        rr(sx1, sy1, sx2, sy2, 6, th.card());
        if (searchFocus) st(sx1, sy1, sx2, sy2, 6, Gfx.argb(acc, 0xC0));
        boolean blink = ((int) (System.nanoTime() / 500_000_000L)) % 2 == 0;
        if (query.isEmpty() && !searchFocus) {
            tx("Поиск...", sx1 + 6, sy1 + 4, th.sub());
        } else {
            tx(fit(query + (searchFocus && blink ? "|" : ""), sx2 - sx1 - 10), sx1 + 6, sy1 + 4, th.text());
        }
        hits.add(new Hit(7, sx1, sy1, sx2, sy2, null, 0));

        // категории
        int cy0 = py + 54;
        float targetSel = cy0 + cat * 20;
        if (selY < 0) selY = targetSel;
        selY = ap(selY, targetSel, 18f, dt);
        int sel = Math.round(selY);
        rr(px + 6, sel, px + SB_W - 6, sel + 18, 6, Gfx.argb(acc, 0x38));
        rr(px + 6, sel + 4, px + 8, sel + 14, 1, 0xFF000000 | acc);
        for (int i = 0; i < Features.CATS.length; i++) {
            int y = cy0 + i * 20;
            boolean active = i == cat && query.isEmpty();
            tx(Features.CATS[i], px + 16, y + 5, active || i == cat ? th.text() : th.sub());
            hits.add(new Hit(6, px + 6, y, px + SB_W - 6, y + 18, null, i));
        }

        // низ
        String name = this.client != null && this.client.player != null ? this.client.player.getName().getString() : "";
        tx(fit(name, SB_W - 20), px + 10, py + ph - 26, th.text());
        tx(fit("Кнопка: " + VisualsClient.menuKeyName(), SB_W - 20), px + 10, py + ph - 15, th.sub());
    }

    private void drawContent(Theme.P th, int acc, int mx, int my, float dt) {
        int cx0 = px + SB_W + 10, cx1 = px + pw - 10;
        int viewTop = py + 30, viewBottom = py + ph - 8;

        // хлебные крошки
        String crumb = query.isEmpty() ? Features.CATS[cat] : "Поиск";
        tx("КК Visuals", cx0, py + 10, th.sub());
        int cw = tw("КК Visuals");
        tx("›", cx0 + cw + 5, py + 10, th.sub());
        tx(crumb, cx0 + cw + 15, py + 10, th.text());
        g.fill(cx0, py + 24, cx1, py + 25, Gfx.mul(th.line(), ga));

        // список функций
        List<Feature> list = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Feature f : Features.ALL) {
            if (q.isEmpty() ? f.category.equals(Features.CATS[cat]) : f.matches(q)) list.add(f);
        }

        contentAnim = ap(contentAnim, 1f, 12f, dt);
        float baseGa = ga;
        ga = baseGa * contentAnim;
        float slide = (1f - contentAnim) * 8f;

        int cols = (cx1 - cx0) >= 280 ? 2 : 1;
        int gap = 8;
        int colW = ((cx1 - cx0) - gap * (cols - 1)) / cols;
        int[] colY = new int[cols];

        viewH = viewBottom - viewTop;
        scrollTarget = Math.max(0f, Math.min(scrollTarget, Math.max(0, totalH - viewH)));
        scroll = ap(scroll, scrollTarget, 18f, dt);

        g.enableScissor(sX(cx0 - 4), sY(viewTop), sX(cx1 + 4), sY(viewBottom));
        for (Feature f : list) {
            int h = cardHeight(f);
            int c = 0;
            for (int i = 1; i < cols; i++) {
                if (colY[i] < colY[c]) c = i;
            }
            int x = cx0 + c * (colW + gap);
            int y = viewTop - Math.round(scroll) + colY[c] + Math.round(slide);
            if (y + h > viewTop - 2 && y < viewBottom + 2) {
                drawCard(f, x, y, colW, h, viewTop, viewBottom, mx, my, dt);
            }
            colY[c] += h + gap;
        }
        g.disableScissor();

        int maxY = 0;
        for (int v : colY) maxY = Math.max(maxY, v);
        totalH = Math.max(0, maxY - gap);

        if (list.isEmpty()) {
            String s = "Ничего не найдено";
            tx(s, (cx0 + cx1) / 2 - tw(s) / 2, (viewTop + viewBottom) / 2 - 4, th.sub());
        }

        ga = baseGa;
        if (totalH > viewH) {
            int bh = Math.max(16, viewH * viewH / totalH);
            float frac = scroll / (float) Math.max(1, totalH - viewH);
            int by = viewTop + Math.round((viewH - bh) * Math.max(0f, Math.min(1f, frac)));
            rr(px + pw - 7, by, px + pw - 5, by + bh, 1, Gfx.argb(th.light() ? 0x000000 : 0xFFFFFF, 0x60));
        }
    }

    private void sw(int sx, int sy, float a) {
        Theme.P th = Theme.cur();
        int off = th.light() ? 0x40000000 : 0x40FFFFFF;
        int on = 0xFF000000 | Theme.accent();
        rr(sx, sy, sx + 20, sy + 10, 5, Gfx.lerp(off, on, a));
        int kx = sx + 1 + Math.round(a * 10);
        rr(kx, sy + 1, kx + 8, sy + 9, 4, 0xFFFFFFFF);
    }

    private void drawCard(Feature f, int x, int y, int w, int h, int vt, int vb, int mx, int my, float dt) {
        Theme.P th = Theme.cur();
        int R2 = Math.max(2, Math.min(Features.T_RADIUS.geti(), 8));
        rr(x, y, x + w, y + h, R2, th.card());
        st(x, y, x + w, y + h, R2, th.line());

        tx(f.name, x + 8, y + 6, th.text());
        if (f.enabled != null) {
            f.anim = ap(f.anim, f.enabled.value ? 1f : 0f, 16f, dt);
            sw(x + w - 8 - 20, y + 5, f.anim);
            hit(0, x, y, x + w, y + HEADER, f, 0, vt, vb);
        }
        rr(x + 6, y + HEADER - 2, x + w - 6, y + HEADER - 1, 0, th.line());

        float saved = ga;
        if (f.enabled != null) ga = saved * (0.5f + 0.5f * f.anim);
        int ry = y + HEADER;
        for (Setting s : f.settings) {
            if (!s.isVisible()) continue;
            ry += drawRow(s, x, ry, w, vt, vb, mx, my, dt);
        }
        ga = saved;
    }

    private int drawRow(Setting s, int x, int y, int w, int vt, int vb, int mx, int my, float dt) {
        int h = rowH(s);
        if (y + h < vt - 2 || y > vb + 2) {
            // вне экрана, но анимации держим актуальными
            return h;
        }
        Theme.P th = Theme.cur();
        int acc = 0xFF000000 | Theme.accent();
        int base = base(s);
        int b = y + (h - base) / 2;

        if (s instanceof Setting.Bool bo) {
            tx(bo.name, x + 8, b + 4, th.text());
            bo.anim = ap(bo.anim, bo.value ? 1f : 0f, 16f, dt);
            sw(x + w - 8 - 20, b + 3, bo.anim);
            hit(1, x, y, x + w, y + h, bo, 0, vt, vb);
        } else if (s instanceof Setting.Num n) {
            tx(n.name, x + 8, b + 3, th.text());
            String v = n.text();
            tx(v, x + w - 8 - tw(v), b + 3, th.sub());
            int t1 = x + 8, t2 = x + w - 8, ty = b + 15;
            float frac = (float) ((n.value - n.min) / (n.max - n.min));
            n.anim = ap(n.anim, frac, 22f, dt);
            rr(t1, ty, t2, ty + 3, 1, th.light() ? 0x30000000 : 0x30FFFFFF);
            int fx = t1 + Math.round((t2 - t1) * n.anim);
            if (fx > t1) rr(t1, ty, fx, ty + 3, 1, acc);
            rr(fx - 3, ty - 2, fx + 3, ty + 5, 3, 0xFFFFFFFF);
            Hit hh = hit(2, x, y, x + w, y + h, n, 0, vt, vb);
            if (hh != null) {
                hh.tx1 = t1;
                hh.tx2 = t2;
            }
        } else if (s instanceof Setting.Mode m) {
            tx(m.name, x + 8, b + 4, th.text());
            String v = fit(m.options[m.index], Math.max(30, w - 16 - tw(m.name) - 16));
            int pwid = tw(v) + 10;
            int rx = x + w - 8;
            rr(rx - pwid, b + 2, rx, b + 14, 5, Gfx.argb(Theme.accent(), 0x38));
            tx(v, rx - pwid + 5, b + 4, th.text());
            hit(3, rx - pwid - 2, y, x + w, y + h, m, 0, vt, vb);
        } else if (s instanceof Setting.Color c) {
            tx(c.name, x + 8, b + 3, th.text());
            if (c.index == 0) {
                String a = "авто";
                tx(a, x + w - 8 - tw(a), b + 3, th.sub());
            }
            int n = Theme.SWATCHES.length + 1;
            c.anim = ap(c.anim, c.index, 16f, dt);
            int sy = b + 15;
            for (int i = 0; i < n; i++) {
                int sx = x + 8 + i * 13;
                int col = i == 0 ? (0xFF000000 | th.accent()) : (0xFF000000 | Theme.SWATCHES[i - 1]);
                rr(sx, sy, sx + 9, sy + 9, 4, col);
                if (i == 0) st(sx, sy, sx + 9, sy + 9, 4, 0x90FFFFFF);
                hit(4, sx - 2, sy - 2, sx + 11, sy + 11, c, i, vt, vb);
            }
            int rx = x + 8 + Math.round(c.anim * 13);
            st(rx - 2, sy - 2, rx + 11, sy + 11, 6, th.text());
        } else if (s instanceof Setting.Action a) {
            int bx1 = x + 8, bx2 = x + w - 8, by1 = b + 1, by2 = b + 17;
            boolean hov = mx >= bx1 && mx <= bx2 && my >= Math.max(by1, vt) && my <= Math.min(by2, vb);
            rr(bx1, by1, bx2, by2, 6, Gfx.argb(Theme.accent(), hov ? 0x66 : 0x34));
            tx(a.name, x + (w - tw(a.name)) / 2, b + 5, th.text());
            hit(5, bx1, by1, bx2, by2, a, 0, vt, vb);
        } else if (s instanceof Setting.Info inf) {
            tx(fit(inf.text.get(), w - 16), x + 8, b + 3, th.sub());
        }
        return h;
    }

    // ---------------- ввод ----------------

    private void setSlider(Hit h, double mx) {
        Setting.Num n = (Setting.Num) h.ref;
        double t = (mx - h.tx1) / (double) Math.max(1, h.tx2 - h.tx1);
        t = Math.max(0, Math.min(1, t));
        n.set(n.min + t * (n.max - n.min));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (closing || open < 0.9f) return true;
        boolean onSearch = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (mx < h.x1 || mx >= h.x2 || my < h.y1 || my >= h.y2) continue;
            switch (h.type) {
                case 0 -> {
                    Feature f = (Feature) h.ref;
                    f.enabled.value = !f.enabled.value;
                    Config.save();
                }
                case 1 -> {
                    Setting.Bool b = (Setting.Bool) h.ref;
                    b.value = !b.value;
                    Config.save();
                }
                case 2 -> {
                    dragging = h;
                    setSlider(h, mx);
                }
                case 3 -> {
                    ((Setting.Mode) h.ref).cycle(button == 1 ? -1 : 1);
                    Config.save();
                }
                case 4 -> {
                    ((Setting.Color) h.ref).index = h.idx;
                    Config.save();
                }
                case 5 -> ((Setting.Action) h.ref).run.run();
                case 6 -> {
                    if (h.idx != cat || !query.isEmpty()) {
                        cat = h.idx;
                        query = "";
                        contentAnim = 0f;
                        scroll = 0f;
                        scrollTarget = 0f;
                    }
                }
                case 7 -> onSearch = true;
                default -> { }
            }
            searchFocus = onSearch;
            return true;
        }
        searchFocus = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            setSlider(dragging, mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        float max = Math.max(0, totalH - viewH);
        scrollTarget = Math.max(0f, Math.min(max, scrollTarget - (float) vAmount * 22f));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocus) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!query.isEmpty()) query = "";
                else searchFocus = false;
                scrollTarget = 0f;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocus = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
                scrollTarget = 0f;
                return true;
            }
            return true;
        }
        if (keyCode == Config.menuKey) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocus && chr >= 32 && query.length() < 24) {
            query += chr;
            scrollTarget = 0f;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            searchFocus = false;
            Config.save();
        }
    }
}
