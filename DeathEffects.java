package ru.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Death Animation: 67, Тунг-тунг Сахур, тряска экрана. */
public final class DeathEffects {
    private DeathEffects() { }

    private static long killNs = -1L;
    private static float shake = 0f;
    private static final List<TungParticle> TUNGS = new ArrayList<>();
    private static final List<SixSeven> SIXSEVENS = new ArrayList<>();

    private static class TungParticle {
        float x, y, vy = -120f, rot, rotSpeed;
        float size, age = 0f;
        final float maxAge;
        TungParticle(float x, float maxAge) {
            this.x = x; this.y = -60f; this.size = 40f + (float)(Math.random() * 30);
            this.rot = (float)(Math.random() * 360);
            this.rotSpeed = -60f + (float)(Math.random() * 120);
            this.maxAge = maxAge;
        }
        boolean tick(float dt) {
            vy = Math.min(400f, vy + 320f * dt);
            y += vy * dt; rot += rotSpeed * dt; age += dt;
            return age < maxAge && y < 1200f;
        }
    }

    private static class SixSeven {
        float x, y, vy = -180f, age = 0f;
        final float maxAge = 2.4f;
        int digit; // 0=6, 1=7
        float scale = 0f;
        SixSeven(float x, float maxAge, int d) {
            this.x = x; this.y = 0.3f; this.digit = d;
        }
        boolean tick(float dt) {
            y += vy * dt; vy += 300f * dt; age += dt;
            scale = Math.min(1f, age * 8f) * (1f - Math.max(0f, (age - maxAge + 0.3f) / 0.3f));
            return age < maxAge;
        }
    }

    public static void onKill() {
        if (!Features.DEATH.on()) return;
        killNs = System.nanoTime();
        if (Features.DA_SHAKER.value) shake = 1f;
        if (Features.DA_TUNG.value) {
            TUNGS.clear();
            int n = 3 + (int)(Math.random() * 3);
            float dur = Features.DA_DURATION.getf();
            for (int i = 0; i < n; i++) TUNGS.add(new TungParticle(80f + (float)(Math.random() * 760), dur));
        }
        if (Features.DA_67.value) {
            SIXSEVENS.clear();
            float dur = Features.DA_DURATION.getf();
            SIXSEVENS.add(new SixSeven(0.3f, dur, 0));
            SIXSEVENS.add(new SixSeven(0.5f, dur, 1));
        }
    }

    public static float shakeDx = 0f, shakeDy = 0f;

    public static void render(DrawContext ctx, MinecraftClient mc, int sw, int sh, float dt) {
        if (!Features.DEATH.on() || killNs < 0L) return;
        float elapsed = (System.nanoTime() - killNs) / 1_000_000_000f;
        float maxDur = Features.DA_DURATION.getf();
        if (elapsed > maxDur + 1f) { killNs = -1L; TUNGS.clear(); SIXSEVENS.clear(); shake = 0f; return; }

        shake = Math.max(0f, shake - dt * 3f);
        if (shake > 0f) {
            shakeDx = (float)(Math.random() - 0.5) * shake * 4f;
            shakeDy = (float)(Math.random() - 0.5) * shake * 4f;
        } else {
            shakeDx = 0f; shakeDy = 0f;
        }

        ctx.getMatrices().push();
        ctx.getMatrices().translate(shakeDx, shakeDy, 0f);

        // "67" текст
        SIXSEVENS.removeIf(s -> !s.tick(dt));
        int mode = Features.DA_MODE.index;
        for (SixSeven ss : SIXSEVENS) {
            int col = 0xFF000000 | Theme.rainbow(elapsed + ss.digit * 0.3f, 0f);
            String txt = ss.digit == 0 ? "6" : "7";
            if (mode == 0) {
                int fs = (int)(24 * ss.scale);
                if (fs < 4) continue;
                ctx.getMatrices().push();
                float bx = sw * ss.x;
                float by2 = (float)(sh * (0.3 + ss.y * 0.2));
                ctx.getMatrices().translate(bx, by2, 0f);
                ctx.getMatrices().scale(ss.scale * 2f, ss.scale * 2f, 1f);
                ctx.drawText(mc.textRenderer, txt, -mc.textRenderer.getWidth(txt) / 2, -4, col, true);
                ctx.getMatrices().pop();
            } else if (mode == 1) {
                ctx.getMatrices().push();
                float bx = sw * ss.x;
                float by2 = (float)(sh * (0.3 + ss.y * 0.2));
                ctx.getMatrices().translate(bx, by2, 0f);
                ctx.getMatrices().scale(ss.scale * 5f, ss.scale * 5f, 1f);
                ctx.drawText(mc.textRenderer, txt, -mc.textRenderer.getWidth(txt) / 2, -4, col, true);
                ctx.getMatrices().pop();
            }
        }

        // Тунг-тунг Сахур
        TUNGS.removeIf(p -> !p.tick(dt));
        for (TungParticle p : TUNGS) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(p.x, p.y, 0f);
            ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rot));
            float s = p.size;
            // голова (прямоугольник)
            ctx.fill(-(int)(s * 0.4f), -(int)(s * 0.6f), (int)(s * 0.4f), 0, 0xFFE8A020);
            // морда
            ctx.fill(-(int)(s * 0.1f), -(int)(s * 0.4f), (int)(s * 0.3f), -(int)(s * 0.15f), 0xFFFFC040);
            // ноги
            ctx.fill(-(int)(s * 0.3f), 0, -(int)(s * 0.1f), (int)(s * 0.6f), 0xFFD09020);
            ctx.fill((int)(s * 0.1f), 0, (int)(s * 0.3f), (int)(s * 0.6f), 0xFFD09020);
            ctx.getMatrices().pop();
        }

        ctx.getMatrices().pop();
    }
}
