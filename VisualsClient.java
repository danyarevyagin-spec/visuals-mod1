package ru.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import ru.visuals.mixin.SimpleOptionAccessor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VisualsClient implements ClientModInitializer {
    private static final String CATEGORY = "category.visuals";
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private KeyBinding zoomKey;

    // zoom
    private boolean zoomActive = false;
    private float zoomCur;
    private int savedFov = 70;

    // fullbright
    private static boolean fbApplied = false;
    private static double fbLevel = -1;
    private static double savedGamma = 1.0;

    // .bind add: 0 нет, 1 ждём отпускания клавиш, 2 ловим клавишу
    private int bindState = 0;
    private boolean menuWasDown = false;

    // цель
    private Entity target = null;
    private int targetTicks = 0;
    private int age = 0;

    // прочее
    private Object lastWorld = null;
    private double prevX, prevZ, speed;

    @Override
    public void onInitializeClient() {
        Config.load();
        Cosmetics.init();
        KkBadge.init();

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register(this::renderHud);
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::restoreAll);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) onAttack(entity);
            return ActionResult.PASS;
        });
    }

    // ================= чат-команды =================

    private boolean onChat(String message) {
        String m = message.trim().toLowerCase();
        if (!m.startsWith(".bind")) return true;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (m.equals(".bind add")) {
            bindState = 1;
            say(mc, "§dКК Visuals§r: нажми кнопку для открытия меню (Esc — отмена).");
        } else if (m.equals(".bind clear")) {
            Config.menuKey = -1;
            Config.save();
            say(mc, "§dКК Visuals§r: кнопка меню удалена.");
        } else {
            say(mc, "§dКК Visuals§r: .bind add — назначить кнопку меню, .bind clear — убрать.");
        }
        return false;
    }

    private static void say(MinecraftClient mc, String text) {
        if (mc.player != null) mc.player.sendMessage(Text.literal(text), false);
    }

    // ================= тик =================

    private boolean isDown(MinecraftClient mc, int key) {
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }

    private void tick(MinecraftClient mc) {
        if (mc.options == null) return;
        applyFullbright(mc);
        Optimizer.tick(mc);
        if (mc.player == null || mc.world == null) {
            lastWorld = null;
            return;
        }
        age++;
        greet(mc);
        handleBind(mc);
        handleMenuKey(mc);
        handleZoom(mc);
        trackSpeed(mc);
        HudExtras.tick(mc);
        MusicPlayer.tickKey(mc);
        applyAim();
        spawnParticles(mc);
    }

    private void applyAim() {
        Entity a = HudExtras.aimed;
        if (a != null && Features.TARGET.on() && Features.TG_AIM.value && passes(a, Features.TG_FILTER.index)) {
            target = a;
            targetTicks = Math.max(targetTicks, 8);
        }
    }

    private void greet(MinecraftClient mc) {
        if (lastWorld == mc.world) return;
        lastWorld = mc.world;
        if (!Features.M_GREET.value) return;
        if (Config.menuKey < 0) say(mc, "§dКК Visuals§r загружен. Напиши §f.bind add§r, чтобы выбрать кнопку меню.");
        else say(mc, "§dКК Visuals§r загружен. Меню: §f" + menuKeyName() + "§r.");
    }

    private void handleBind(MinecraftClient mc) {
        if (bindState == 0 || mc.currentScreen != null) return;
        if (bindState == 1) {
            boolean any = false;
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (isDown(mc, k)) { any = true; break; }
            }
            if (!any) bindState = 2;
            return;
        }
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            if (!isDown(mc, k)) continue;
            bindState = 0;
            if (k == GLFW.GLFW_KEY_ESCAPE) {
                say(mc, "§dКК Visuals§r: отменено.");
            } else {
                Config.menuKey = k;
                Config.save();
                say(mc, "§dКК Visuals§r: меню теперь на кнопке §f" + menuKeyName() + "§r.");
            }
            menuWasDown = true;
            return;
        }
    }

    private void handleMenuKey(MinecraftClient mc) {
        if (Config.menuKey < 0) return;
        boolean down = isDown(mc, Config.menuKey);
        if (down && !menuWasDown && mc.currentScreen == null && bindState == 0) {
            mc.setScreen(new MenuScreen());
        }
        menuWasDown = down;
    }

    private void trackSpeed(MinecraftClient mc) {
        double x = mc.player.getX(), z = mc.player.getZ();
        double inst = Math.hypot(x - prevX, z - prevZ) * 20.0;
        if (inst > 100) inst = 0;
        speed = speed * 0.7 + inst * 0.3;
        prevX = x;
        prevZ = z;
    }

    // ================= Fullbright / Zoom =================

    @SuppressWarnings("unchecked")
    private static void writeGamma(MinecraftClient mc, double v) {
        SimpleOption<Double> o = mc.options.getGamma();
        ((SimpleOptionAccessor<Double>) (Object) o).visuals$setValue(v);
    }

    @SuppressWarnings("unchecked")
    private static void writeFov(MinecraftClient mc, int v) {
        SimpleOption<Integer> o = mc.options.getFov();
        ((SimpleOptionAccessor<Integer>) (Object) o).visuals$setValue(v);
    }

    private void applyFullbright(MinecraftClient mc) {
        boolean want = Features.FULLBRIGHT.on();
        double level = Features.FB_LEVEL.get();
        if (want) {
            if (!fbApplied) {
                savedGamma = mc.options.getGamma().getValue();
                fbApplied = true;
                fbLevel = -1;
            }
            if (fbLevel != level) {
                writeGamma(mc, level);
                fbLevel = level;
            }
        } else if (fbApplied) {
            writeGamma(mc, savedGamma);
            fbApplied = false;
        }
    }

    private void handleZoom(MinecraftClient mc) {
        boolean want = Features.ZOOM.on() && zoomKey.isPressed() && mc.currentScreen == null;
        if (want && !zoomActive) {
            savedFov = mc.options.getFov().getValue();
            zoomCur = savedFov;
            zoomActive = true;
        }
        if (!zoomActive) return;
        float target = want ? Features.Z_FOV.getf() : savedFov;
        float k = Math.max(0.04f, 1f - Features.Z_SMOOTH.getf() / 100f);
        zoomCur += (target - zoomCur) * k;
        if (!want && Math.abs(target - zoomCur) < 0.5f) {
            writeFov(mc, savedFov);
            zoomActive = false;
            return;
        }
        writeFov(mc, Math.round(zoomCur));
    }

    private void restoreAll(MinecraftClient mc) {
        if (mc.options == null) return;
        Optimizer.restoreAll();
        MotionBlur.release();
        if (fbApplied) {
            writeGamma(mc, savedGamma);
            fbApplied = false;
        }
        if (zoomActive) {
            writeFov(mc, savedFov);
            zoomActive = false;
        }
    }

    // ================= частицы =================

    private static boolean passes(Entity e, int mode) {
        if (mode == 0) return true;
        if (mode == 1) return e instanceof PlayerEntity;
        return e instanceof LivingEntity && !(e instanceof PlayerEntity);
    }

    private void onAttack(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        HudExtras.onHit();
        DeathEffects.onKill();
        if (!passes(entity, Features.TG_FILTER.index)) return;
        target = entity;
        targetTicks = Features.TG_TIME.geti() * 20;

        if (Features.HIT.on() && passes(entity, Features.H_FILTER.index)) {
            burst(mc, entity, Features.PARTICLES[Features.H_STYLE.index], Features.H_COUNT.geti(),
                    Features.H_RADIUS.get(), Features.H_SPEED.get(), Features.H_SHAPE.index);
        }
    }

    private void burst(MinecraftClient mc, Entity e, ParticleEffect eff, int count, double rad, double speed, int shape) {
        Random r = mc.world.random;
        double cx = e.getX(), cy = e.getY() + e.getHeight() / 2.0, cz = e.getZ();
        switch (shape) {
            case 0 -> {
                for (int i = 0; i < count; i++) {
                    double dx = r.nextGaussian(), dy = r.nextGaussian(), dz = r.nextGaussian();
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (len < 1e-4) continue;
                    dx /= len; dy /= len; dz /= len;
                    mc.world.addParticle(eff, cx + dx * rad, cy + dy * rad, cz + dz * rad,
                            dx * speed, dy * speed, dz * speed);
                }
            }
            case 1 -> {
                for (int i = 0; i < count; i++) {
                    double a = Math.PI * 2 * i / count;
                    double cs = Math.cos(a), sn = Math.sin(a);
                    mc.world.addParticle(eff, cx + cs * rad, cy, cz + sn * rad, cs * speed, 0.0, sn * speed);
                }
            }
            case 2 -> {
                for (int i = 0; i < count; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double rr = r.nextDouble() * rad;
                    mc.world.addParticle(eff, cx + Math.cos(a) * rr, e.getY() + 0.1, cz + Math.sin(a) * rr,
                            Math.cos(a) * speed * 0.3, speed * (0.8 + r.nextDouble()), Math.sin(a) * speed * 0.3);
                }
            }
            default -> {
                for (int i = 0; i < count; i++) {
                    double t = i / (double) count;
                    double a = t * Math.PI * 6;
                    double y = e.getY() + t * e.getHeight();
                    mc.world.addParticle(eff, cx + Math.cos(a) * rad, y, cz + Math.sin(a) * rad,
                            Math.cos(a) * speed * 0.3, 0.01, Math.sin(a) * speed * 0.3);
                }
            }
        }
    }

    private static boolean isNight(MinecraftClient mc) {
        long t = mc.world.getTimeOfDay() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    private void spawnParticles(MinecraftClient mc) {
        Random r = mc.world.random;

        // ---- частицы в воздухе ----
        if (Features.PART.on()
                && !(Features.P_NIGHT.value && !isNight(mc))
                && !(Features.P_OUTDOOR.value && !mc.world.isSkyVisible(mc.player.getBlockPos()))) {
            ParticleEffect eff = Features.PARTICLES[Features.P_STYLE.index];
            int n = Features.P_DENSITY.geti();
            double rad = Features.P_RADIUS.get(), hgt = Features.P_HEIGHT.get(), sp = Features.P_SPEED.get();
            int move = Features.P_MOVE.index;
            for (int i = 0; i < n; i++) {
                double ang = r.nextDouble() * Math.PI * 2;
                double d = Math.sqrt(r.nextDouble()) * rad;
                double x = mc.player.getX() + Math.cos(ang) * d;
                double z = mc.player.getZ() + Math.sin(ang) * d;
                double y = mc.player.getY() - 1 + r.nextDouble() * hgt;
                double vx = (r.nextDouble() - 0.5) * 0.01 * sp;
                double vz = (r.nextDouble() - 0.5) * 0.01 * sp;
                double vy;
                switch (move) {
                    case 0 -> vy = 0.008 * sp;
                    case 1 -> vy = -0.06 * sp;
                    case 2 -> vy = 0.05 * sp;
                    default -> {
                        vx = -Math.sin(ang) * 0.05 * sp;
                        vz = Math.cos(ang) * 0.05 * sp;
                        vy = 0.01 * sp;
                    }
                }
                mc.world.addParticle(eff, x, y, z, vx, vy, vz);
            }
        }

        // ---- цель ----
        if (target == null) return;
        if (!target.isAlive()) {
            if (Features.HIT.on() && Features.H_DEATH.value) {
                double m = Features.H_DEATH_MULT.get();
                burst(mc, target, Features.PARTICLES[Features.H_STYLE.index],
                        (int) (Features.H_COUNT.get() * m), Features.H_RADIUS.get() * (0.8 + m * 0.4),
                        Features.H_SPEED.get() * (0.8 + m * 0.3), 0);
            }
            target = null;
            return;
        }
        if (--targetTicks <= 0 || target.isRemoved()) {
            target = null;
            return;
        }
        if (!Features.TARGET.on()) return;

        ParticleEffect eff = Features.PARTICLES[Features.TG_STYLE.index];
        int n = Features.TG_COUNT.geti();
        double rad = Features.TG_RADIUS.get() * Math.max(0.6, target.getWidth());
        double rot = age * Features.TG_SPEED.get();
        double lift = Features.TG_LIFT.get();
        double h = target.getHeight();
        double y = switch (Features.TG_MODE.index) {
            case 0 -> target.getY() + 0.05;
            case 1 -> target.getY() + h / 2.0;
            case 2 -> target.getY() + h + 0.1;
            default -> target.getY() + (0.5 + 0.5 * Math.sin(age * 0.15)) * h;
        };
        int rings = Features.TG_SECOND.value ? 2 : 1;
        for (int ring = 0; ring < rings; ring++) {
            double rr = ring == 0 ? rad : rad * 0.7;
            double dir = ring == 0 ? 1 : -1;
            for (int i = 0; i < n; i++) {
                double a = dir * rot + i * (Math.PI * 2 / n);
                mc.world.addParticle(eff, target.getX() + Math.cos(a) * rr, y, target.getZ() + Math.sin(a) * rr, 0, lift, 0);
            }
        }
    }

    // ================= для меню =================

    public static String menuKeyName() {
        if (Config.menuKey < 0) return "не назначена";
        return InputUtil.fromKeyCode(Config.menuKey, 0).getLocalizedText().getString();
    }

    // ================= HUD =================

    private void panel(DrawContext ctx, int x, int y, int w, int h, int style, int r, float opacity) {
        if (style == 0) {
            Gfx.glass(ctx, x, y, x + w, y + h, r, opacity, 1f, false);
        } else if (style == 1) {
            Gfx.roundRect(ctx, x, y, x + w, y + h, r, 0xE0000000 | Theme.cur().bg());
            if (Features.T_BORDER.value) Gfx.stroke(ctx, x, y, x + w, y + h, r, Theme.cur().line());
        }
    }

    private void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;
        MotionBlur.run(ctx, mc);
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        if (Features.WM.on()) renderWatermark(ctx, mc, sw);
        if (Features.COORDS.on()) renderCoords(ctx, mc, sw, sh);
        if (Features.EFFECTS.on()) renderEffects(ctx, mc, sw);
        if (Features.ARMOR.on()) renderArmor(ctx, mc, sw, sh);
        HudExtras.render(ctx, mc, sw, sh);
        KkBadge.render(ctx, mc);
        MusicPlayer.render(ctx, mc, sw, sh);
        float dt2 = 1f / Math.max(1f, mc.getCurrentFps());
        DeathEffects.render(ctx, mc, sw, sh, dt2);
    }

    private void renderWatermark(DrawContext ctx, MinecraftClient mc, int sw) {
        Theme.P th = Theme.cur();
        List<String> parts = new ArrayList<>();
        if (Features.W_NAME.value) parts.add("КК Visuals");
        if (Features.W_USER.value) parts.add(mc.player.getName().getString());
        if (Features.W_FPS.value) parts.add(mc.getCurrentFps() + " fps");
        if (Features.W_PING.value && !mc.isInSingleplayer() && mc.getNetworkHandler() != null) {
            PlayerListEntry pe = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (pe != null) parts.add(pe.getLatency() + " ms");
        }
        if (Features.W_TIME.value) parts.add(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (parts.isEmpty()) return;

        String text = String.join("  |  ", parts);
        int style = Features.W_STYLE.index;
        int w = mc.textRenderer.getWidth(text) + 16;
        int h = 16;
        int x = switch (Features.W_POS.index) {
            case 1 -> 4;
            case 2 -> sw - w - 4;
            default -> (sw - w) / 2;
        };
        int y = 4;
        int r = Features.W_RADIUS.geti();
        panel(ctx, x, y, w, h, style, r, Features.W_OPACITY.getf() / 100f);
        if (Features.W_LINE.value && style != 2) {
            ctx.fill(x + r + 2, y + h - 1, x + w - r - 2, y + h, 0xFF000000 | Theme.accent());
        }
        int col = style == 2 ? 0xFFFFFFFF : th.text();
        ctx.drawText(mc.textRenderer, text, x + 8, y + 4, col, style == 2);
    }

    private void renderCoords(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        Theme.P th = Theme.cur();
        List<String> lines = new ArrayList<>();
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        boolean dec = Features.C_DEC.value;
        lines.add(dec ? String.format(java.util.Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", x, y, z)
                : String.format("XYZ: %d / %d / %d", (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
        if (Features.C_NETHER.value) {
            var key = mc.world.getRegistryKey();
            if (key == World.OVERWORLD) {
                lines.add(String.format("Незер: %d / %d", (int) Math.floor(x / 8), (int) Math.floor(z / 8)));
            } else if (key == World.NETHER) {
                lines.add(String.format("Обычный мир: %d / %d", (int) Math.floor(x * 8), (int) Math.floor(z * 8)));
            }
        }
        if (Features.C_DIR.value) {
            Direction f = mc.player.getHorizontalFacing();
            lines.add("Направление: " + f.asString().toUpperCase());
        }
        if (Features.C_BIOME.value) {
            String biome = mc.world.getBiome(mc.player.getBlockPos()).getKey()
                    .map(k -> {
                        Identifier id = k.getValue();
                        return Text.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString();
                    }).orElse("?");
            lines.add("Биом: " + biome);
        }
        if (Features.C_SPEED.value) lines.add(String.format(java.util.Locale.ROOT, "Скорость: %.1f б/с", speed));

        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, mc.textRenderer.getWidth(s));
        int w = maxW + 14;
        int h = lines.size() * 11 + 6;
        int bx = Features.C_POS.index == 2 ? sw - w - 4 : 4;
        int by = Features.C_POS.index == 1 ? 24 : sh - h - 4;
        int style = Features.C_STYLE.index;
        panel(ctx, bx, by, w, h, style, Features.C_RADIUS.geti(), Features.C_OPACITY.getf() / 100f);
        int col = style == 2 ? 0xFFFFFFFF : th.text();
        int ly = by + 4;
        for (String s : lines) {
            ctx.drawText(mc.textRenderer, s, bx + 7, ly, col, style == 2);
            ly += 11;
        }
    }

    private void renderEffects(DrawContext ctx, MinecraftClient mc, int sw) {
        Theme.P th = Theme.cur();
        var list = mc.player.getStatusEffects();
        if (list.isEmpty()) return;

        List<String> names = new ArrayList<>();
        List<String> times = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Float> alphas = new ArrayList<>();
        int maxName = 0, maxTime = 0;
        for (StatusEffectInstance e : list) {
            String name = e.getEffectType().value().getName().getString();
            if (Features.E_LEVEL.value && e.getAmplifier() > 0) {
                int lvl = e.getAmplifier() + 1;
                name += " " + (lvl < ROMAN.length ? ROMAN[lvl] : String.valueOf(lvl));
            }
            String time = "";
            if (Features.E_TIMER.value) {
                if (e.isInfinite()) time = "∞";
                else {
                    int s = e.getDuration() / 20;
                    time = String.format("%02d:%02d", s / 60, s % 60);
                }
            }
            float a = 1f;
            if (Features.E_BLINK.value && !e.isInfinite() && e.getDuration() < 200) {
                a = (float) (0.55 + 0.45 * Math.sin(age * 0.6));
            }
            names.add(name);
            times.add(time);
            colors.add(e.getEffectType().value().getColor());
            alphas.add(a);
            maxName = Math.max(maxName, mc.textRenderer.getWidth(name));
            maxTime = Math.max(maxTime, mc.textRenderer.getWidth(time));
        }
        int dot = Features.E_DOT.value ? 10 : 0;
        int w = 8 + dot + maxName + (maxTime > 0 ? 10 + maxTime : 0) + 8;
        int h = names.size() * 11 + 6;
        int bx = Features.E_POS.index == 1 ? 4 : sw - w - 4;
        int by = 24;
        int style = Features.E_STYLE.index;
        panel(ctx, bx, by, w, h, style, Features.E_RADIUS.geti(), Features.E_OPACITY.getf() / 100f);
        int ly = by + 4;
        for (int i = 0; i < names.size(); i++) {
            float a = alphas.get(i);
            int tcol = Gfx.mul(style == 2 ? 0xFFFFFFFF : th.text(), a);
            int x = bx + 8;
            if (Features.E_DOT.value) {
                Gfx.roundRect(ctx, x, ly + 1, x + 6, ly + 7, 3, Gfx.mul(0xFF000000 | colors.get(i), a));
                x += 10;
            }
            ctx.drawText(mc.textRenderer, names.get(i), x, ly, tcol, style == 2);
            if (!times.get(i).isEmpty()) {
                ctx.drawText(mc.textRenderer, times.get(i), bx + w - 8 - mc.textRenderer.getWidth(times.get(i)), ly, tcol, style == 2);
            }
            ly += 11;
        }
    }

    private void renderArmor(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        Theme.P th = Theme.cur();
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) items.add(st);
        }
        if (Features.A_HAND.value) {
            ItemStack st = mc.player.getMainHandStack();
            if (!st.isEmpty()) items.add(st);
        }
        if (items.isEmpty()) return;

        int mode = Features.A_DUR.index;
        List<String> texts = new ArrayList<>();
        List<Float> fracs = new ArrayList<>();
        int maxW = 0;
        for (ItemStack st : items) {
            String t = "";
            float frac = 1f;
            if (st.isDamageable()) {
                int max = st.getMaxDamage();
                int left = max - st.getDamage();
                frac = left / (float) max;
                if (mode == 1) t = Math.round(frac * 100) + "%";
                else if (mode == 2) t = String.valueOf(left);
            } else if (st.getCount() > 1 && mode != 3) {
                t = "x" + st.getCount();
            }
            texts.add(t);
            fracs.add(frac);
            maxW = Math.max(maxW, mc.textRenderer.getWidth(t));
        }
        int rowH = 18;
        int w = 6 + 16 + (maxW > 0 ? 4 + maxW : 0) + 6;
        int h = items.size() * rowH + 4;
        int bx = Features.A_POS.index == 1 ? sw - w - 6 : 6;
        int by = (sh - h) / 2;
        if (Features.A_BG.value) {
            Gfx.glass(ctx, bx, by, bx + w, by + h, 6, Features.A_OPACITY.getf() / 100f, 1f, false);
        }
        int y = by + 2;
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            ctx.drawItem(st, bx + 6, y);
            if (mode == 0) ctx.drawStackOverlay(mc.textRenderer, st, bx + 6, y);
            if (!texts.get(i).isEmpty()) {
                int col = st.isDamageable() ? (0xFF000000 | Theme.mix(0xFF5555, 0x55FF55, fracs.get(i))) : th.text();
                ctx.drawText(mc.textRenderer, texts.get(i), bx + 6 + 16 + 4, y + 5, col, !Features.A_BG.value);
            }
            y += rowH;
        }
    }
}
