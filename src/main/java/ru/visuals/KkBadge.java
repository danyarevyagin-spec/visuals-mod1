package ru.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** KK значок над ником: рисуется поверх экрана через HUD. */
public final class KkBadge {
    private KkBadge() { }

    public static void init() { } // инициализация не нужна, рендер через HUD

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Features.BADGE.on() || mc.player == null || mc.world == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        String[] styles = {"KK", "\u2726KK\u2726", "\u300AKK\u300B", "\u272EKK\u272E"};
        String label = styles[Math.min(Features.BD_STYLE.index, styles.length - 1)];
        float time = System.nanoTime() / 1_000_000_000f;
        int col = Features.BD_RAINBOW.value ? (0xFF000000 | Theme.rainbow(time, 0f))
                : (0xFF000000 | Theme.pick(Features.BD_COLOR));

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !Features.BD_SELF.value) continue;
            if (!p.isAlive()) continue;
            double dist = mc.player.distanceTo(p);
            if (dist > 32) continue;
            // Проецируем позицию игрока на экран
            double[] screen = project(mc, p.getX(), p.getY() + p.getHeight() + Features.BD_Y.get(), p.getZ(), sw, sh);
            if (screen == null) continue;
            int sx = (int) screen[0], sy = (int) screen[1];
            if (sx < -50 || sx > sw + 50 || sy < -50 || sy > sh + 50) continue;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(sx, sy, 0f);
            float scale = Features.BD_SCALE.getf() * Math.max(0.3f, 1f - (float)(dist / 40));
            ctx.getMatrices().scale(scale, scale, 1f);
            int w = mc.textRenderer.getWidth(label);
            Gfx.roundRect(ctx, -w / 2 - 3, -5, w / 2 + 3, 7, 3, 0x80000000);
            ctx.drawText(mc.textRenderer, label, -w / 2, -4, col, true);
            ctx.getMatrices().pop();
        }
    }

    private static double[] project(MinecraftClient mc, double wx, double wy, double wz, int sw, int sh) {
        try {
            var cam = mc.gameRenderer.getCamera();
            double dx = wx - cam.getPos().x;
            double dy = wy - cam.getPos().y;
            double dz = wz - cam.getPos().z;
            var rotation = cam.getRotation();
            // простая трансформация через матрицу вида (приближение)
            float yaw = (float) Math.toRadians(cam.getYaw());
            float pitch = (float) Math.toRadians(cam.getPitch());
            double cy = Math.cos(yaw), sy2 = Math.sin(yaw);
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double rx = dx * cy - dz * sy2;
            double rz = dx * sy2 + dz * cy;
            double ry = dy * cp - rz * sp;
            double rz2 = dy * sp + rz * cp;
            if (rz2 <= 0.1) return null; // позади камеры
            double fov = Math.toRadians(mc.options.getFov().getValue());
            double f = 1.0 / Math.tan(fov / 2.0);
            double aspect = (double) sw / sh;
            double px = rx * f / (aspect * rz2) * (sw / 2.0) + sw / 2.0;
            double py = -ry * f / rz2 * (sh / 2.0) + sh / 2.0;
            return new double[]{px, py};
        } catch (Exception e) {
            return null;
        }
    }
}
