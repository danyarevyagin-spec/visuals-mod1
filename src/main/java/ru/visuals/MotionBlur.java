package ru.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * Motion blur через накопление кадров: предыдущий (уже размытый) кадр подмешивается
 * к текущему с заданной силой, поэтому движущиеся объекты оставляют плавный шлейф.
 */
public final class MotionBlur {
    private MotionBlur() { }

    private static Framebuffer accum;
    private static boolean first = true;
    private static long lastNs = System.nanoTime();
    private static float lastYaw, lastPitch;
    private static float motion = 0f;

    public static void run(DrawContext ctx, MinecraftClient mc) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;

        if (!Features.MB.on() || mc.currentScreen != null || mc.player == null) {
            first = true;
            return;
        }

        // насколько двигается камера и игрок
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        float ang = Math.abs(yaw - lastYaw) + Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        float angSpeed = dt > 0f ? ang / dt : 0f;
        float vel = (float) mc.player.getVelocity().length();
        float target = Math.min(1f, angSpeed / 120f + vel * 1.5f);
        motion += (target - motion) * Math.min(1f, dt * 12f);

        float strength = Features.MB_STR.getf() / 100f;
        if (Features.MB_REACT.value) {
            float minS = Math.min(strength, Features.MB_MIN.getf() / 100f);
            strength = minS + (strength - minS) * motion;
        }
        if (Features.MB_ADAPT.value && dt > 0f) {
            strength = (float) Math.pow(strength, dt * 60f);
        }
        strength = Math.max(0f, Math.min(0.95f, strength));

        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        ctx.draw();

        if (accum == null || accum.textureWidth != w || accum.textureHeight != h) {
            if (accum != null) accum.delete();
            accum = new SimpleFramebuffer(w, h, false);
            first = true;
        }

        if (!first && strength > 0.01f) {
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL14.glBlendColor(0f, 0f, 0f, strength);
            accum.draw(w, h);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }

        accum.beginWrite(true);
        RenderSystem.disableBlend();
        main.draw(w, h);
        main.beginWrite(true);
        first = false;
    }

    public static void release() {
        if (accum != null) {
            accum.delete();
            accum = null;
        }
        first = true;
    }
}
