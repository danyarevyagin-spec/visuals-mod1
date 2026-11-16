package ru.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Optional;

/** Прицел, инфо о цели при наведении и счётчик CPS. */
public final class HudExtras {
    private HudExtras() { }

    public static Entity aimed;

    private static Entity infoEntity;
    private static int infoTimer;
    private static float infoAnim;
    private static float shownHp = -1f;
    private static int shownId = -1;

    private static float hitFlash;
    private static float dyn;

    private static boolean lDown, rDown;
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>();
    private static final ArrayDeque<Long> RMB = new ArrayDeque<>();
    private static long lastNs = System.nanoTime();

    public static void onHit() {
        hitFlash = 1f;
    }

    public static boolean passes(Entity e, int mode) {
        if (mode == 0) return true;
        if (mode == 1) return e instanceof PlayerEntity;
        return e instanceof LivingEntity && !(e instanceof PlayerEntity);
    }

    // ================= наведение =================

    public static Entity findAimed(MinecraftClient mc, double range, boolean through) {
        Entity cam = mc.getCameraEntity();
        if (cam == null) cam = mc.player;
        if (cam == null || mc.world == null) return null;
        Vec3d eye = cam.getCameraPosVec(1f);
        Vec3d dir = cam.getRotationVec(1f);
        Vec3d end = eye.add(dir.multiply(range));
        Box box = cam.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
        Entity best = null;
        double bestD = range * range;
        for (Entity e : mc.world.getOtherEntities(cam, box, en -> en instanceof LivingEntity && en.isAlive() && !en.isSpectator())) {
            Box eb = e.getBoundingBox().expand(e.getTargetingMargin());
            Optional<Vec3d> hit = eb.raycast(eye, end);
            double d;
            Vec3d hp;
            if (eb.contains(eye)) {
                d = 0;
                hp = eye;
            } else if (hit.isPresent()) {
                hp = hit.get();
                d = eye.squaredDistanceTo(hp);
            } else {
                continue;
            }
            if (d >= bestD) continue;
            if (!through) {
                HitResult blk = mc.world.raycast(new RaycastContext(eye, hp,
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cam));
                if (blk.getType() != HitResult.Type.MISS) continue;
            }
            best = e;
            bestD = d;
        }
        return best;
    }

    public static void tick(MinecraftClient mc) {
        aimed = null;
        boolean ti = Features.TI.on();
        boolean need = ti || (Features.TARGET.on() && Features.TG_AIM.value);
        if (need) {
            aimed = findAimed(mc, ti ? Features.TI_RANGE.get() : 12.0, ti && Features.TI_THROUGH.value);
        }
        if (ti) {
            if (aimed != null && passes(aimed, Features.TI_FILTER.index)) {
                infoEntity = aimed;
                infoTimer = (int) (Features.TI_HOLD.get() * 20);
            } else if (infoTimer > 0) {
                infoTimer--;
            }
        }
    }

    // ================= общий рендер =================

    public static void render(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        hitFlash = Math.max(0f, hitFlash - dt * 4f);

        if (Features.CROSS.on()) renderCrosshair(ctx, mc, sw, sh, dt);
        if (Features.TI.on()) renderTargetInfo(ctx, mc, sw, sh, dt);
        if (Features.CPS.on()) renderCps(ctx, mc, sw, sh);
    }

    // ================= прицел =================

    private static void bar(DrawContext c, int x1, int y1, int x2, int y2, int col, boolean ol, int oa) {
        if (ol) c.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, oa);
        c.fill(x1, y1, x2, y2, col);
    }

    private static void cross(DrawContext c, int col, boolean ol, int oa, int size, int gap, int t, boolean noTop) {
        int h0 = -(t / 2), h1 = h0 + t;
        bar(c, -gap - size, h0, -gap, h1, col, ol, oa);
        bar(c, gap, h0, gap + size, h1, col, ol, oa);
        if (!noTop) bar(c, h0, -gap - size, h1, -gap, col, ol, oa);
        bar(c, h0, gap, h1, gap + size, col, ol, oa);
    }

    private static void ring(DrawContext c, int r, int t, int col, boolean ol, int oa, int radiusCorner) {
        if (ol) {
            Gfx.stroke(c, -r - 1, -r - 1, r + 1, r + 1, radiusCorner == 0 ? 0 : r + 1, oa);
            Gfx.stroke(c, -r + t, -r + t, r - t, r - t, radiusCorner == 0 ? 0 : Math.max(0, r - t), oa);
        }
        for (int k = 0; k < t; k++) {
            Gfx.stroke(c, -r + k, -r + k, r - k, r - k, radiusCorner == 0 ? 0 : r - k, col);
        }
    }

    private static void renderCrosshair(DrawContext ctx, MinecraftClient mc, int sw, int sh, float dt) {
        if (mc.currentScreen != null || mc.player == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        float time = System.nanoTime() / 1_000_000_000f;
        boolean moving = mc.player.getVelocity().horizontalLengthSquared() > 0.0009;
        float tdyn = Features.X_DYNAMIC.value
                ? (mc.player.isSprinting() ? 1f : (moving ? 0.5f : 0f)) + (mc.player.isOnGround() ? 0f : 0.7f)
                : 0f;
        dyn += (tdyn - dyn) * Math.min(1f, dt * 10f);

        int rgb = Features.X_RAINBOW.value ? Theme.rainbow(time, 0f) : Theme.pick(Features.X_COLOR);
        if (Features.X_TARGETCOL.value && mc.targetedEntity != null) rgb = 0xFF4D4D;
        float op = Features.X_OPACITY.getf() / 100f;
        int col = ((int) (255 * op) << 24) | rgb;
        boolean ol = Features.X_OUTLINE.value;
        int oa = ((int) (0xB0 * op)) << 24;

        int size = Features.X_SIZE.geti();
        int t = Features.X_THICK.geti();
        int gap = Math.round(Features.X_GAP.getf() + dyn * Features.X_DYN_AMT.getf() + hitFlash * 2f);
        int style = Features.X_STYLE.index;
        float spin = Features.X_SPIN.getf();

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(sw / 2f, sh / 2f, 0f);
        if (spin > 0f) ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(time * spin * 40f));

        switch (style) {
            case 0 -> cross(ctx, col, ol, oa, size, gap, t, false);
            case 1 -> {
                int d = Math.max(2, size / 2 + t);
                bar(ctx, -(d / 2), -(d / 2), -(d / 2) + d, -(d / 2) + d, col, ol, oa);
            }
            case 2 -> ring(ctx, size + gap + 2, t, col, ol, oa, 1);
            case 3 -> {
                cross(ctx, col, ol, oa, size, gap + 1, t, false);
                int d = t + 1;
                bar(ctx, -(d / 2), -(d / 2), -(d / 2) + d, -(d / 2) + d, col, ol, oa);
            }
            case 4 -> ring(ctx, size + gap, t, col, ol, oa, 0);
            case 5 -> {
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
                ring(ctx, Math.max(2, Math.round((size + gap) * 0.75f)), t, col, ol, oa, 0);
                ms.pop();
            }
            case 6 -> cross(ctx, col, ol, oa, size, gap, t, true);
            case 7 -> {
                int d = gap + size;
                int len = Math.max(2, size / 2 + 1);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int hx1 = Math.min(sx * d, sx * (d - len)), hx2 = Math.max(sx * d, sx * (d - len));
                        int vy1 = Math.min(sy * d, sy * (d - len)), vy2 = Math.max(sy * d, sy * (d - len));
                        bar(ctx, hx1, sy * d - (t == 1 ? 0 : t / 2), hx2, sy * d - (t == 1 ? 0 : t / 2) + t, col, ol, oa);
                        bar(ctx, sx * d - (t == 1 ? 0 : t / 2), vy1, sx * d - (t == 1 ? 0 : t / 2) + t, vy2, col, ol, oa);
                    }
                }
            }
            case 8 -> {
                cross(ctx, col, ol, oa, size, gap, t, false);
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
                cross(ctx, col, ol, oa, size, gap, t, false);
                ms.pop();
            }
            default -> {
                ring(ctx, size + gap + 3, t, col, ol, oa, 1);
                cross(ctx, col, ol, oa, 2, 1, t, false);
            }
        }
        if (Features.X_DOT.value && style != 1 && style != 3) {
            int d = t + 1;
            bar(ctx, -(d / 2), -(d / 2), -(d / 2) + d, -(d / 2) + d, col, ol, oa);
        }
        ms.pop();

        // хитмаркер
        if (Features.X_HITMARK.value && hitFlash > 0.01f) {
            int hcol = ((int) (255 * Math.min(1f, hitFlash * 1.5f)) << 24) | Theme.pick(Features.X_HITCOL);
            ms.push();
            ms.translate(sw / 2f, sh / 2f, 0f);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
            int a = 4 + Math.round((1f - hitFlash) * 3f);
            int b = a + 5;
            ctx.fill(-b, 0, -a, 1, hcol);
            ctx.fill(a, 0, b, 1, hcol);
            ctx.fill(0, -b, 1, -a, hcol);
            ctx.fill(0, a, 1, b, hcol);
            ms.pop();
        }
    }

    // ================= инфо о цели =================

    private static void txt(DrawContext c, TextRenderer tr, String s, int x, int y, int col, float a) {
        int cc = Gfx.mul(col, a);
        if (((cc >>> 24) & 255) < 8) return;
        c.drawText(tr, s, x, y, cc, false);
    }

    private static String fitText(TextRenderer tr, String s, int maxW) {
        if (tr.getWidth(s) <= maxW) return s;
        while (s.length() > 1 && tr.getWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static void renderTargetInfo(DrawContext ctx, MinecraftClient mc, int sw, int sh, float dt) {
        boolean show = infoEntity != null && infoEntity.isAlive() && (aimed == infoEntity || infoTimer > 0);
        infoAnim += ((show ? 1f : 0f) - infoAnim) * Math.min(1f, dt * 14f);
        if (infoAnim < 0.03f) {
            if (!show) infoEntity = null;
            return;
        }
        if (!(infoEntity instanceof LivingEntity le) || mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        Theme.P th = Theme.cur();
        float an = infoAnim;

        String name = le.getName().getString();
        float hp = le.getHealth(), max = Math.max(1f, le.getMaxHealth()), abs = le.getAbsorptionAmount();
        if (shownId != le.getId() || shownHp < 0f) {
            shownId = le.getId();
            shownHp = hp;
        }
        shownHp += (hp - shownHp) * Math.min(1f, dt * 10f);

        String dist = Features.TI_DIST.value
                ? String.format(Locale.ROOT, "%.1f м", mc.player.distanceTo(le)) : "";
        boolean armor = Features.TI_ARMOR.value, hands = Features.TI_HANDS.value;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        ItemStack main = le.getMainHandStack(), off = le.getOffHandStack();

        String mainName = hands && Features.TI_NAMES.value && !main.isEmpty() ? "Рука: " + main.getName().getString() : "";
        String offName = hands && Features.TI_NAMES.value && !off.isEmpty() ? "Левая: " + off.getName().getString() : "";
        mainName = fitText(tr, mainName, 150);
        offName = fitText(tr, offName, 150);

        int iconsW = (armor ? 4 * 18 : 0) + (armor && hands ? 8 : 0) + (hands ? 2 * 18 : 0);
        int w = 130;
        w = Math.max(w, tr.getWidth(name) + (dist.isEmpty() ? 0 : tr.getWidth(dist) + 14) + 14);
        w = Math.max(w, iconsW + 12);
        w = Math.max(w, Math.max(tr.getWidth(mainName), tr.getWidth(offName)) + 14);

        boolean armorLine = Features.TI_ARMORVAL.value;
        int h = 6 + 12 + 13 + (armorLine ? 11 : 0) + ((armor || hands) ? 20 : 0)
                + (mainName.isEmpty() ? 0 : 11) + (offName.isEmpty() ? 0 : 11) + 4;

        int x, y;
        switch (Features.TI_POS.index) {
            case 1 -> { x = sw / 2 - w / 2; y = sh / 2 + 24; }
            case 2 -> { x = sw / 2 - w / 2; y = 30; }
            case 3 -> { x = sw / 2 - 24 - w; y = sh / 2 - h / 2; }
            default -> { x = sw / 2 + 24; y = sh / 2 - h / 2; }
        }
        if (x < 2) x = 2;
        if (x + w > sw - 2) x = sw - 2 - w;
        if (y < 2) y = 2;
        if (y + h > sh - 2) y = sh - 2 - h;

        Gfx.glass(ctx, x, y, x + w, y + h, Features.TI_RADIUS.geti(), Features.TI_OPACITY.getf() / 100f, an, false);

        int cy = y + 6;
        txt(ctx, tr, name, x + 7, cy, th.text(), an);
        if (!dist.isEmpty()) txt(ctx, tr, dist, x + w - 7 - tr.getWidth(dist), cy, th.sub(), an);
        cy += 13;

        // полоска здоровья
        int bx1 = x + 7, bx2 = x + w - 7, bh = 9;
        Gfx.roundRect(ctx, bx1, cy, bx2, cy + bh, 4, Gfx.mul(th.light() ? 0x30000000 : 0x40FFFFFF, an));
        float ratio = Math.max(0f, Math.min(1f, shownHp / max));
        int fx = bx1 + Math.round((bx2 - bx1) * ratio);
        int hpCol = Gfx.lerp(0xFFFF4D4D, 0xFF4DFF7A, ratio);
        if (fx - bx1 >= 2) Gfx.roundRect(ctx, bx1, cy, fx, cy + bh, 4, Gfx.mul(hpCol, an));
        if (abs > 0f) {
            int ax = bx1 + Math.round((bx2 - bx1) * Math.min(1f, abs / max));
            ctx.fill(bx1 + 2, cy + bh, Math.max(bx1 + 3, ax), cy + bh + 1, Gfx.mul(0xFFFFD34D, an));
        }
        String hpText = String.format(Locale.ROOT, "%.1f / %.0f", hp, max) + (abs > 0f ? String.format(Locale.ROOT, " +%.0f", abs) : "");
        txt(ctx, tr, hpText, (bx1 + bx2) / 2 - tr.getWidth(hpText) / 2, cy + 1, 0xFFFFFFFF, an);
        cy += bh + 4;

        if (armorLine) {
            txt(ctx, tr, "Броня: " + le.getArmor(), x + 7, cy, th.sub(), an);
            cy += 11;
        }

        if (armor || hands) {
            int ix = x + 7;
            boolean items = an > 0.9f;
            if (armor) {
                for (EquipmentSlot s : slots) {
                    ItemStack st = le.getEquippedStack(s);
                    Gfx.roundRect(ctx, ix - 1, cy - 1, ix + 17, cy + 17, 4, Gfx.mul(th.card(), an));
                    if (items && !st.isEmpty()) {
                        ctx.drawItem(st, ix, cy);
                        if (Features.TI_DUR.value) ctx.drawStackOverlay(tr, st, ix, cy);
                    }
                    ix += 18;
                }
            }
            if (armor && hands) ix += 8;
            if (hands) {
                for (ItemStack st : new ItemStack[]{main, off}) {
                    Gfx.roundRect(ctx, ix - 1, cy - 1, ix + 17, cy + 17, 4, Gfx.mul(Gfx.argb(Theme.accent(), 0x30), an));
                    if (items && !st.isEmpty()) {
                        ctx.drawItem(st, ix, cy);
                        if (Features.TI_DUR.value) ctx.drawStackOverlay(tr, st, ix, cy);
                    }
                    ix += 18;
                }
            }
            cy += 20;
        }
        if (!mainName.isEmpty()) {
            txt(ctx, tr, mainName, x + 7, cy, th.text(), an);
            cy += 11;
        }
        if (!offName.isEmpty()) {
            txt(ctx, tr, offName, x + 7, cy, th.sub(), an);
        }
    }

    // ================= CPS =================

    private static void renderCps(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            long hnd = mc.getWindow().getHandle();
            boolean l = GLFW.glfwGetMouseButton(hnd, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(hnd, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (l && !lDown) LMB.add(now);
            if (r && !rDown) RMB.add(now);
            lDown = l;
            rDown = r;
        }
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000L) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000L) RMB.pollFirst();

        String s = "CPS  " + LMB.size() + (Features.CPS_RMB.value ? "  |  " + RMB.size() : "");
        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth(s) + 14, h = 15;
        int x, y;
        switch (Features.CPS_POS.index) {
            case 1 -> { x = 4; y = 24; }
            case 2 -> { x = sw / 2 - w / 2; y = sh / 2 + 22; }
            default -> { x = sw - w - 4; y = sh - h - 4; }
        }
        Gfx.glass(ctx, x, y, x + w, y + h, Features.CPS_RADIUS.geti(), Features.CPS_OPACITY.getf() / 100f, 1f, false);
        ctx.drawText(tr, s, x + 7, y + 4, Theme.cur().text(), false);
    }
}
