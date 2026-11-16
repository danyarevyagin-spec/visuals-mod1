package ru.visuals;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Косметика на игроке: крылья, шляпы, нимб, рожки, ушки, хвост.
 * Рисуется цветными четырёхугольниками прямо на модели игрока.
 * Координаты внутри геометрии: (x вправо, up вверх, back назад от спины).
 */
public final class Cosmetics {
    private Cosmetics() { }

    public static void init() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer pr) helper.register(new CosLayer(pr));
        });
    }

    // ================= слой рендера =================

    private static final class CosLayer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        CosLayer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
            super(context);
        }

        public void render(MatrixStack matrices, VertexConsumerProvider vcp, int light,
                           PlayerEntityRenderState state, float limbAngle, float limbDistance) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            boolean head = Features.HAT.on() || Features.HALO.on() || Features.HORNS.on() || Features.EARS.on();
            boolean body = Features.WINGS.on() || Features.TAIL.on();
            if (!head && !body) return;

            if (Features.COS_WHO.index == 0) {
                double dx = state.x - mc.player.getX();
                double dy = state.y - mc.player.getY();
                double dz = state.z - mc.player.getZ();
                if (dx * dx + dy * dy + dz * dz > 4.0) return;
            }

            VertexConsumer vc = vcp.getBuffer(RenderLayer.getDebugQuads());
            float time = (System.nanoTime() / 1_000_000L % 1_000_000L) / 1000f;
            PlayerEntityModel model = getContextModel();

            if (head) {
                matrices.push();
                model.head.rotate(matrices);
                Matrix4f m = matrices.peek().getPositionMatrix();
                if (Features.HAT.on()) hat(new Mesh(vc, m, Features.CH_OPACITY.getf() / 100f), time);
                if (Features.HALO.on()) halo(new Mesh(vc, m, Features.HL_OPACITY.getf() / 100f), time);
                if (Features.HORNS.on()) horns(new Mesh(vc, m, Features.HR_OPACITY.getf() / 100f));
                if (Features.EARS.on()) ears(new Mesh(vc, m, Features.EA_OPACITY.getf() / 100f), time);
                matrices.pop();
            }
            if (body) {
                matrices.push();
                model.body.rotate(matrices);
                Matrix4f m = matrices.peek().getPositionMatrix();
                if (Features.WINGS.on()) wings(new Mesh(vc, m, Features.CW_OPACITY.getf() / 100f), time);
                if (Features.TAIL.on()) tail(new Mesh(vc, m, Features.TL_OPACITY.getf() / 100f), time);
                matrices.pop();
            }
        }
    }

    // ================= сетка =================

    private static final class Mesh {
        final VertexConsumer vc;
        final Matrix4f m;
        final float alpha;

        Mesh(VertexConsumer vc, Matrix4f m, float alpha) {
            this.vc = vc;
            this.m = m;
            this.alpha = alpha;
        }

        private static int shade(int rgb, float f) {
            int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
            int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
            int b = Math.min(255, (int) ((rgb & 255) * f));
            return (r << 16) | (g << 8) | b;
        }

        private void vert(float[] p, int rgb, float a) {
            int al = (int) (Math.max(0f, Math.min(1f, a * alpha)) * 255f);
            vc.vertex(m, p[0], -p[1], p[2]).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, al);
        }

        void quadG(float[] a, float[] b, float[] c, float[] d,
                   int c0, int c1, int c2, int c3, float a0, float a1, float a2, float a3) {
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float f = 1f;
            if (len > 1e-7f) {
                float dot = Math.abs((nx * 0.35f + ny * 0.8f + nz * 0.5f) / (len * 1.006f));
                f = 0.62f + 0.5f * Math.min(1f, dot);
            }
            vert(a, shade(c0, f), a0);
            vert(b, shade(c1, f), a1);
            vert(c, shade(c2, f), a2);
            vert(d, shade(c3, f), a3);
        }

        void quad(float[] a, float[] b, float[] c, float[] d, int col, float al) {
            quadG(a, b, c, d, col, col, col, col, al, al, al, al);
        }

        void tri(float[] a, float[] b, float[] c, int col, float al) {
            quadG(a, b, c, a, col, col, col, col, al, al, al, al);
        }

        /** Усечённый конус/цилиндр между двумя горизонтальными кольцами. */
        void ring(float cx0, float cy0, float cz0, float r0, float cx1, float cy1, float cz1, float r1,
                  int seg, int cA, int cB, float a0, float a1) {
            for (int i = 0; i < seg; i++) {
                double an0 = Math.PI * 2 * i / seg, an1 = Math.PI * 2 * (i + 1) / seg;
                float[] p0 = {cx0 + r0 * (float) Math.cos(an0), cy0, cz0 + r0 * (float) Math.sin(an0)};
                float[] p1 = {cx0 + r0 * (float) Math.cos(an1), cy0, cz0 + r0 * (float) Math.sin(an1)};
                float[] p2 = {cx1 + r1 * (float) Math.cos(an1), cy1, cz1 + r1 * (float) Math.sin(an1)};
                float[] p3 = {cx1 + r1 * (float) Math.cos(an0), cy1, cz1 + r1 * (float) Math.sin(an0)};
                int col = (i % 2 == 0) ? cA : cB;
                quadG(p0, p1, p2, p3, col, col, col, col, a0, a0, a1, a1);
            }
        }

        void disk(float cx, float cy, float cz, float r, int seg, int col, float al) {
            float[] c = {cx, cy, cz};
            for (int i = 0; i < seg; i++) {
                double an0 = Math.PI * 2 * i / seg, an1 = Math.PI * 2 * (i + 1) / seg;
                float[] p0 = {cx + r * (float) Math.cos(an0), cy, cz + r * (float) Math.sin(an0)};
                float[] p1 = {cx + r * (float) Math.cos(an1), cy, cz + r * (float) Math.sin(an1)};
                tri(c, p0, p1, col, al);
            }
        }

        void box(float cx, float cy, float cz, float hx, float hy, float hz, int col, float al) {
            float[] a = {cx - hx, cy - hy, cz - hz}, b = {cx + hx, cy - hy, cz - hz};
            float[] c = {cx + hx, cy + hy, cz - hz}, d = {cx - hx, cy + hy, cz - hz};
            float[] e = {cx - hx, cy - hy, cz + hz}, f = {cx + hx, cy - hy, cz + hz};
            float[] g = {cx + hx, cy + hy, cz + hz}, h = {cx - hx, cy + hy, cz + hz};
            quad(a, b, c, d, col, al);
            quad(f, e, h, g, col, al);
            quad(e, a, d, h, col, al);
            quad(b, f, g, c, col, al);
            quad(d, c, g, h, col, al);
            quad(e, f, b, a, col, al);
        }
    }

    private static int col(Setting.Color c, boolean rainbow, float time, float off) {
        return rainbow ? Theme.rainbow(time, off) : Theme.pick(c);
    }

    private static int mixRgb(int a, int b, float t) {
        return Theme.mix(a, b, t);
    }

    // ================= шляпы =================

    private static void hat(Mesh m, float time) {
        boolean rb = Features.CH_RAINBOW.value;
        int c1 = col(Features.CH_C1, rb, time, 0f);
        int c2 = col(Features.CH_C2, rb, time, 0.5f);
        float s = Features.CH_SIZE.getf();
        float hm = Features.CH_HEIGHT.getf();
        float base = 0.5f + Features.CH_LIFT.getf();

        switch (Features.CH_STYLE.index) {
            case 0 -> { // китайская конусная шляпа
                float R = 0.62f * s, H = 0.36f * s * hm;
                float[][] prof = {{R, 0f}, {R * 0.62f, H * 0.38f}, {R * 0.30f, H * 0.72f}, {0.035f * s, H}};
                for (int k = 0; k < 3; k++) {
                    m.ring(0, base + prof[k][1], 0, prof[k][0], 0, base + prof[k + 1][1], 0, prof[k + 1][0],
                            28, c1, c2, 1f, 1f);
                }
                m.ring(0, base - 0.025f, 0, R, 0, base, 0, R, 28, c2, c2, 1f, 1f);
                m.ring(0, base + H, 0, 0.04f * s, 0, base + H + 0.05f * s, 0, 0.012f * s, 8, c2, c2, 1f, 1f);
            }
            case 1 -> { // ведьмина
                m.ring(0, base, 0, 0.58f * s, 0, base, 0, 0.2f * s, 28, c2, c2, 1f, 1f);
                float[][] p = {{0.24f, 0f, 0f}, {0.20f, 0.25f, 0f}, {0.14f, 0.5f, 0.03f}, {0.05f, 0.75f, 0.09f}};
                for (int k = 0; k < 3; k++) {
                    m.ring(p[k][2] * s, base + p[k][1] * s * hm, 0, p[k][0] * s,
                            p[k + 1][2] * s, base + p[k + 1][1] * s * hm, 0, p[k + 1][0] * s, 20, c1, c1, 1f, 1f);
                }
                m.ring(0, base, 0, 0.255f * s, 0, base + 0.08f * s, 0, 0.235f * s, 20, c2, c2, 1f, 1f);
            }
            case 2 -> { // корона
                m.ring(0, base, 0, 0.29f * s, 0, base + 0.1f * s, 0, 0.29f * s, 24, c1, c2, 1f, 1f);
                for (int k = 0; k < 5; k++) {
                    double an = Math.PI * 2 * k / 5;
                    float sx = 0.29f * s * (float) Math.cos(an), sz = 0.29f * s * (float) Math.sin(an);
                    m.ring(sx, base + 0.1f * s, sz, 0.06f * s, sx, base + 0.3f * s * hm, sz, 0.008f * s, 6, c1, c2, 1f, 1f);
                }
            }
            case 3 -> { // цилиндр
                float H = 0.42f * s * hm;
                m.ring(0, base, 0, 0.42f * s, 0, base, 0, 0.29f * s, 28, c2, c2, 1f, 1f);
                m.ring(0, base, 0, 0.29f * s, 0, base + H, 0, 0.27f * s, 24, c1, c1, 1f, 1f);
                m.disk(0, base + H, 0, 0.27f * s, 24, c1, 1f);
                m.ring(0, base + 0.02f * s, 0, 0.292f * s, 0, base + 0.1f * s, 0, 0.292f * s, 24, c2, c2, 1f, 1f);
            }
            default -> { // колпак
                float[][] p = {{0.30f, 0f}, {0.22f, 0.2f}, {0.13f, 0.4f}, {0.05f, 0.55f}};
                for (int k = 0; k < 3; k++) {
                    int c = (k % 2 == 0) ? c1 : c2;
                    m.ring(0, base + p[k][1] * s * hm, 0, p[k][0] * s, 0, base + p[k + 1][1] * s * hm, 0, p[k + 1][0] * s,
                            20, c, c, 1f, 1f);
                }
                float top = base + 0.55f * s * hm;
                m.ring(0, top, 0, 0.06f * s, 0, top + 0.06f * s, 0, 0.02f * s, 8, c2, c2, 1f, 1f);
            }
        }
    }

    private static void halo(Mesh m, float time) {
        int c = col(Features.HL_C1, Features.HL_RAINBOW.value, time, 0f);
        float s = Features.HL_SIZE.getf();
        float up = 0.5f + Features.HL_LIFT.getf() + (float) Math.sin(time * 2f) * Features.HL_BOB.getf();
        float R = 0.32f * s, r = 0.25f * s, t = 0.015f;
        m.ring(0, up + t, 0, R, 0, up + t, 0, r, 32, c, c, 1f, 1f);
        m.ring(0, up - t, 0, R, 0, up - t, 0, r, 32, c, c, 1f, 1f);
        m.ring(0, up + t, 0, R, 0, up - t, 0, R, 32, c, c, 1f, 1f);
        m.ring(0, up + t, 0, r, 0, up - t, 0, r, 32, c, c, 1f, 1f);
        m.ring(0, up, 0, R + 0.07f * s, 0, up, 0, Math.max(0.01f, r - 0.06f * s), 32, c, c, 0.22f, 0.22f);
    }

    private static void horns(Mesh m) {
        int c1 = Theme.pick(Features.HR_C1), c2 = Theme.pick(Features.HR_C2);
        float s = Features.HR_SIZE.getf();
        for (int side = -1; side <= 1; side += 2) {
            float bx = side * 0.17f, by = 0.5f, bz = -0.07f;
            if (Features.HR_STYLE.index == 0) {
                m.ring(bx, by - 0.02f, bz, 0.065f * s, bx + side * 0.035f * s, by + 0.16f * s, bz - 0.01f, 0.04f * s, 10, c1, c1, 1f, 1f);
                m.ring(bx + side * 0.035f * s, by + 0.16f * s, bz - 0.01f, 0.04f * s,
                        bx + side * 0.07f * s, by + 0.3f * s, bz - 0.02f, 0.006f * s, 10, c2, c2, 1f, 1f);
            } else {
                m.ring(bx, by - 0.02f, bz, 0.07f * s, bx + side * 0.05f * s, by + 0.14f * s, bz - 0.01f, 0.05f * s, 10, c1, c1, 1f, 1f);
                m.ring(bx + side * 0.05f * s, by + 0.14f * s, bz - 0.01f, 0.05f * s,
                        bx + side * 0.13f * s, by + 0.25f * s, bz - 0.04f, 0.028f * s, 10, c1, c2, 1f, 1f);
                m.ring(bx + side * 0.13f * s, by + 0.25f * s, bz - 0.04f, 0.028f * s,
                        bx + side * 0.22f * s, by + 0.3f * s, bz - 0.07f, 0.005f * s, 10, c2, c2, 1f, 1f);
            }
        }
    }

    private static void ears(Mesh m, float time) {
        int c1 = Theme.pick(Features.EA_C1), c2 = Theme.pick(Features.EA_C2);
        float s = Features.EA_SIZE.getf();
        for (int side = -1; side <= 1; side += 2) {
            float ex = side * 0.17f;
            float tw = Features.EA_TWITCH.value
                    ? (float) Math.sin(time * 9f + side) * 0.012f * (Math.sin(time * 0.7f) > 0.85 ? 3f : 0.4f) : 0f;
            m.ring(ex, 0.5f, -0.02f, 0.1f * s, ex + side * 0.05f * s + tw, 0.5f + 0.2f * s, -0.02f, 0.006f * s, 3, c1, c1, 1f, 1f);
            m.ring(ex, 0.505f, -0.045f, 0.055f * s, ex + side * 0.045f * s + tw, 0.5f + 0.15f * s, -0.045f, 0.004f * s, 3, c2, c2, 1f, 1f);
        }
    }

    // ================= хвост =================

    private static void tail(Mesh m, float time) {
        boolean rb = Features.TL_RAINBOW.value;
        int c1 = col(Features.TL_C1, rb, time, 0f), c2 = col(Features.TL_C2, rb, time, 0.5f);
        float len = Features.TL_LENGTH.getf(), swing = Features.TL_SWING.getf();
        boolean fox = Features.TL_STYLE.index == 1;
        int n = 12;
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            float x = (float) Math.sin(time * 2.5f - u * 3.2f) * swing * 0.28f * u * len;
            float up = -0.68f + (u * u * 0.55f - u * 0.12f) * len;
            float back = 0.13f + u * 0.5f * len;
            float sz = fox ? 0.08f + 0.06f * (float) Math.sin(Math.PI * u) : 0.055f;
            int c = fox ? (u > 0.72f ? c2 : c1) : (i >= n - 2 ? c2 : c1);
            m.box(x, up, back, sz, sz, sz, c, 1f);
        }
    }

    // ================= крылья =================

    private static final float PX = 0.16f, PU = -0.06f, PB = 0.13f;

    private static float[] W(float side, float dx, float du, float psi, float db) {
        return new float[]{side * (PX + dx * (float) Math.cos(psi)), PU + du, PB + dx * (float) Math.sin(psi) + db};
    }

    private static void wings(Mesh m, float time) {
        boolean rb = Features.CW_RAINBOW.value;
        int c1 = col(Features.CW_C1, rb, time, 0f), c2 = col(Features.CW_C2, rb, time, 0.4f);
        float s = Features.CW_SIZE.getf();
        float psi = (float) Math.toRadians(Features.CW_OPEN.getf()
                + Math.sin(time * Features.CW_SPEED.getf() * 3f) * Features.CW_FLAP.getf());
        for (int side = -1; side <= 1; side += 2) {
            switch (Features.CW_STYLE.index) {
                case 0 -> angelWing(m, side, s, psi, c1, c2);
                case 1 -> demonWing(m, side, s, psi, c1, c2);
                case 2 -> butterflyWing(m, side, s, psi, c1, c2);
                default -> rayWing(m, side, s, psi, c1, c2);
            }
        }
    }

    private static void feather(Mesh m, float side, float psi, float a, float len, float w0, float w1,
                                int cBase, int cTip, float aBase, float aTip, float db) {
        float dx = (float) Math.sin(a), du = (float) Math.cos(a);
        float nx = (float) Math.cos(a), nu = -(float) Math.sin(a);
        float bx = dx * 0.06f, bu = du * 0.06f;
        float tx = dx * len, tu = du * len;
        float[] p0 = W(side, bx - nx * w0 / 2, bu - nu * w0 / 2, psi, db);
        float[] p1 = W(side, bx + nx * w0 / 2, bu + nu * w0 / 2, psi, db);
        float[] p2 = W(side, tx + nx * w1 / 2, tu + nu * w1 / 2, psi, db);
        float[] p3 = W(side, tx - nx * w1 / 2, tu - nu * w1 / 2, psi, db);
        m.quadG(p0, p1, p2, p3, cBase, cBase, cTip, cTip, aBase, aBase, aTip, aTip);
    }

    private static void angelWing(Mesh m, int side, float s, float psi, int c1, int c2) {
        float[] lens = {0.95f, 1.05f, 1.0f, 0.9f, 0.78f, 0.62f, 0.48f};
        for (int i = 0; i < lens.length; i++) {
            float a = (float) Math.toRadians(18 + i * 14f);
            feather(m, side, psi, a, lens[i] * s, 0.14f * s, 0.05f * s, c1, mixRgb(c1, c2, 0.8f), 1f, 0.7f, i * 0.004f);
        }
    }

    private static void bone(Mesh m, float side, float psi, float ax, float au, float bx, float bu, float w, int col, float db) {
        float dx = bx - ax, du = bu - au;
        float len = (float) Math.sqrt(dx * dx + du * du);
        if (len < 1e-5f) return;
        float nx = -du / len * w / 2, nu = dx / len * w / 2;
        m.quad(W(side, ax - nx, au - nu, psi, db), W(side, ax + nx, au + nu, psi, db),
                W(side, bx + nx, bu + nu, psi, db), W(side, bx - nx, bu - nu, psi, db), col, 1f);
    }

    private static void demonWing(Mesh m, int side, float s, float psi, int c1, int c2) {
        float[][] t = {{0.55f, 0.45f}, {1.0f, 0.25f}, {1.15f, -0.1f}, {0.95f, -0.5f}, {0.55f, -0.75f}, {0.05f, -0.55f}};
        float[] c = {0.1f * s, -0.1f * s};
        for (int i = 0; i < t.length; i++) {
            float[] a = t[i], b = t[(i + 1) % t.length];
            m.tri(W(side, c[0], c[1], psi, 0f), W(side, a[0] * s, a[1] * s, psi, 0f),
                    W(side, b[0] * s, b[1] * s, psi, 0f), c2, 0.88f);
        }
        m.tri(W(side, c[0], c[1], psi, 0f), W(side, t[0][0] * s, t[0][1] * s, psi, 0f), W(side, 0f, 0f, psi, 0f), c2, 0.88f);
        int boneCol = Theme.mix(c1, 0x000000, 0.35f);
        bone(m, side, psi, 0f, 0f, t[0][0] * s, t[0][1] * s, 0.045f * s, c1, 0.004f);
        for (int i = 1; i < t.length - 1; i++) {
            bone(m, side, psi, t[0][0] * s, t[0][1] * s, t[i][0] * s, t[i][1] * s, 0.03f * s, boneCol, 0.004f);
        }
    }

    private static void fanPoly(Mesh m, int side, float s, float psi, float[][] pts, int col, float al, float db) {
        float cx = 0f, cu = 0f;
        for (float[] p : pts) { cx += p[0]; cu += p[1]; }
        cx /= pts.length;
        cu /= pts.length;
        for (int i = 0; i < pts.length; i++) {
            float[] a = pts[i], b = pts[(i + 1) % pts.length];
            m.tri(W(side, cx * s, cu * s, psi, db), W(side, a[0] * s, a[1] * s, psi, db), W(side, b[0] * s, b[1] * s, psi, db), col, al);
        }
    }

    private static float[][] scalePoly(float[][] pts, float k) {
        float cx = 0f, cu = 0f;
        for (float[] p : pts) { cx += p[0]; cu += p[1]; }
        cx /= pts.length;
        cu /= pts.length;
        float[][] r = new float[pts.length][2];
        for (int i = 0; i < pts.length; i++) {
            r[i][0] = cx + (pts[i][0] - cx) * k;
            r[i][1] = cu + (pts[i][1] - cu) * k;
        }
        return r;
    }

    private static void butterflyWing(Mesh m, int side, float s, float psi, int c1, int c2) {
        float[][] up = {{0.1f, 0.0f}, {0.5f, 0.6f}, {0.95f, 0.75f}, {1.05f, 0.35f}, {0.85f, 0.0f}, {0.4f, -0.1f}};
        float[][] lo = {{0.05f, -0.1f}, {0.55f, -0.1f}, {0.8f, -0.45f}, {0.6f, -0.75f}, {0.25f, -0.65f}, {0.05f, -0.4f}};
        fanPoly(m, side, s, psi, up, c1, 0.95f, 0f);
        fanPoly(m, side, s, psi, lo, c2, 0.95f, 0.002f);
        fanPoly(m, side, s, psi, scalePoly(up, 0.55f), c2, 0.9f, 0.006f);
        fanPoly(m, side, s, psi, scalePoly(lo, 0.5f), c1, 0.9f, 0.008f);
    }

    private static void rayWing(Mesh m, int side, float s, float psi, int c1, int c2) {
        int n = 7;
        for (int i = 0; i < n; i++) {
            float a = (float) Math.toRadians(10 + i * 22f);
            float len = (i % 2 == 0 ? 1.1f : 0.8f) * s;
            feather(m, side, psi, a, len * 1.04f, 0.14f * s, 0.07f * s, c1, c2, 0.22f, 0.05f, i * 0.003f);
            feather(m, side, psi, a, len, 0.05f * s, 0.0f, c1, c2, 0.95f, 0.35f, i * 0.003f + 0.002f);
        }
    }
}
