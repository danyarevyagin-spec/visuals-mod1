package ru.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.SoundManager;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Простой плеер: играет .wav / .ogg / .mp3 (через JavaSound или fallback) из выбранных файлов. */
public final class MusicPlayer {
    private MusicPlayer() { }

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kk-music");
        t.setDaemon(true);
        return t;
    });

    private static final List<File> PLAYLIST = new ArrayList<>();
    private static int index = 0;
    private static volatile Clip clip;
    private static volatile boolean playing = false;
    private static volatile String currentName = "";
    private static volatile long startMs = 0, lenMs = 0;
    private static float animPos = 0f;
    private static long lastNs = System.nanoTime();
    private static boolean prevKey = false;
    private static boolean fileChooserOpen = false;

    public static void openFileChooser() {
        if (fileChooserOpen) return;
        fileChooserOpen = true;
        Thread t = new Thread(() -> {
            try {
                Class<?> fc = Class.forName("javax.swing.JFileChooser");
                Class<?> filter = Class.forName("javax.swing.filechooser.FileNameExtensionFilter");
                Object f = filter.getConstructor(String.class, String[].class)
                        .newInstance("Аудио (wav, ogg, mp3, flac)", new String[]{"wav", "ogg", "mp3", "flac"});
                Object chooser = fc.getDeclaredConstructor().newInstance();
                fc.getMethod("setDialogTitle", String.class).invoke(chooser, "КК Visuals — выбери музыку");
                fc.getMethod("setFileFilter", Class.forName("javax.swing.filechooser.FileFilter")).invoke(chooser, f);
                fc.getMethod("setMultiSelectionEnabled", boolean.class).invoke(chooser, true);
                int res = (int) fc.getMethod("showOpenDialog", java.awt.Component.class).invoke(chooser, (Object) null);
                if (res == 0) {
                    File[] files = (File[]) fc.getMethod("getSelectedFiles").invoke(chooser);
                    if (files != null && files.length > 0) {
                        synchronized (PLAYLIST) {
                            for (File file : files) PLAYLIST.add(file);
                        }
                        if (!playing) next();
                    }
                }
            } catch (Exception e) {
                System.err.println("[KK Visuals] File chooser error: " + e);
            } finally {
                fileChooserOpen = false;
            }
        }, "kk-filechooser");
        t.setDaemon(true);
        t.start();
    }

    private static void play(File file) {
        POOL.submit(() -> {
            try {
                stop();
                AudioInputStream ais;
                try {
                    ais = AudioSystem.getAudioInputStream(file);
                } catch (Exception e) {
                    System.err.println("[KK Visuals] Cannot open: " + file + " – " + e);
                    playing = false;
                    return;
                }
                AudioFormat fmt = ais.getFormat();
                if (fmt.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fmt.getSampleRate(),
                            16, fmt.getChannels(), fmt.getChannels() * 2, fmt.getSampleRate(), false);
                    ais = AudioSystem.getAudioInputStream(pcm, ais);
                }
                Clip c = AudioSystem.getClip();
                c.open(ais);
                FloatControl vol = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
                float db = 20f * (float) Math.log10(Math.max(0.01f, Features.MP_VOL.getf() / 100f));
                vol.setValue(Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), db)));
                clip = c;
                currentName = file.getName().replaceFirst("\\.[^.]+$", "");
                lenMs = c.getMicrosecondLength() / 1000;
                startMs = System.currentTimeMillis();
                playing = true;
                c.start();
                c.addLineListener(ev -> {
                    if (ev.getType() == LineEvent.Type.STOP && playing) {
                        playing = false;
                        try { Thread.sleep(80); } catch (InterruptedException ignored) { }
                        nextInternal();
                    }
                });
            } catch (Exception e) {
                System.err.println("[KK Visuals] Playback error: " + e);
                playing = false;
            }
        });
    }

    public static void next() {
        List<File> pl;
        synchronized (PLAYLIST) { pl = new ArrayList<>(PLAYLIST); }
        if (pl.isEmpty()) return;
        if (Features.MP_SHUFFLE.value) index = (int)(Math.random() * pl.size());
        else index = (index + 1) % pl.size();
        play(pl.get(index));
    }

    private static void nextInternal() {
        if (!Features.MPLAYER.on()) return;
        List<File> pl;
        synchronized (PLAYLIST) { pl = new ArrayList<>(PLAYLIST); }
        if (pl.isEmpty()) return;
        if (Features.MP_LOOP.value && pl.size() == 1) { play(pl.get(0)); return; }
        if (Features.MP_SHUFFLE.value) index = (int)(Math.random() * pl.size());
        else index = (index + 1) % pl.size();
        play(pl.get(index));
    }

    public static void prev() {
        List<File> pl;
        synchronized (PLAYLIST) { pl = new ArrayList<>(PLAYLIST); }
        if (pl.isEmpty()) return;
        index = Math.floorMod(index - 1, pl.size());
        play(pl.get(index));
    }

    public static void toggle() {
        if (clip == null) { next(); return; }
        if (playing) { clip.stop(); playing = false; }
        else { clip.start(); playing = true; startMs = System.currentTimeMillis() - clip.getMicrosecondPosition() / 1000; }
    }

    public static void stop() {
        playing = false;
        Clip c = clip;
        if (c != null) {
            c.stop();
            c.close();
            clip = null;
        }
    }

    public static void clearPlaylist() {
        stop();
        synchronized (PLAYLIST) { PLAYLIST.clear(); }
        index = 0;
    }

    public static void tickKey(MinecraftClient mc) {
        if (mc.player == null || mc.currentScreen != null) return;
        long h = mc.getWindow().getHandle();
        boolean k = org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == 1
                && org.lwjgl.glfw.GLFW.glfwGetKey(h, org.lwjgl.glfw.GLFW.GLFW_KEY_P) == 1;
        if (k && !prevKey) openFileChooser();
        prevKey = k;
    }

    public static void render(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        if (!Features.MPLAYER.on()) return;
        synchronized (PLAYLIST) { if (PLAYLIST.isEmpty()) return; }

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        Theme.P th = Theme.cur();
        int acc = 0xFF000000 | Theme.accent();
        boolean mini = Features.MP_MINIFY.value;

        int w = mini ? 100 : 200, h2 = mini ? 18 : 46;
        int bx = switch (Features.MP_POS.index) {
            case 1 -> sw - w - 4;
            case 2 -> 4;
            default -> 4;
        };
        int by = switch (Features.MP_POS.index) {
            case 2 -> sh - h2 - 24;
            default -> 24;
        };

        Gfx.glass(ctx, bx, by, bx + w, by + h2, 8, Features.T_OPACITY.getf() / 100f, 1f, false);
        float time = System.nanoTime() / 1_000_000_000f;
        int tcol = Theme.pick(Features.BD_COLOR);

        // название трека
        String name = playing ? currentName : (PLAYLIST.size() > 0 ? "⏸ пауза" : "");
        if (!mini) {
            String n = textFit(mc, name, w - 14);
            ctx.drawText(mc.textRenderer, n, bx + 7, by + 5, th.text(), false);

            if (lenMs > 0) {
                long pos = Math.min(lenMs, System.currentTimeMillis() - startMs);
                float frac = playing ? (float)pos / lenMs : (clip != null ? (float)(clip.getMicrosecondPosition() / 1000) / lenMs : 0f);
                int t1 = bx + 7, t2 = bx + w - 7, ty = by + 18;
                Gfx.roundRect(ctx, t1, ty, t2, ty + 2, 1, Gfx.mul(th.line(), 1f));
                int fx = t1 + Math.round(frac * (t2 - t1));
                if (fx > t1) Gfx.roundRect(ctx, t1, ty, fx, ty + 2, 1, acc);

                // кнопки ⏮ ⏯ ⏭
                int[] bxs = {bx + w / 2 - 24, bx + w / 2 - 8, bx + w / 2 + 8};
                String[] btns = {"⏮", playing ? "⏸" : "▶", "⏭"};
                for (int i = 0; i < 3; i++) {
                    boolean hov = false; // hover handled via handleClick
                    Gfx.roundRect(ctx, bxs[i], by + 25, bxs[i] + 14, by + 39, 4, Gfx.mul(hov ? acc : th.card(), 1f));
                    ctx.drawText(mc.textRenderer, btns[i], bxs[i] + 3, by + 29, th.text(), false);
                }
            }
        } else {
            String n = textFit(mc, (playing ? "▶ " : "⏸ ") + name, w - 10);
            ctx.drawText(mc.textRenderer, n, bx + 5, by + 5, th.text(), false);
        }
    }

    private static String textFit(MinecraftClient mc, String s, int max) {
        while (mc.textRenderer.getWidth(s) > max && s.length() > 1) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static boolean handleClick(double mx, double my, MinecraftClient mc, int sw, int sh) {
        if (!Features.MPLAYER.on() || Features.MP_MINIFY.value) return false;
        boolean empty;
        synchronized (PLAYLIST) { empty = PLAYLIST.isEmpty(); }
        if (empty) return false;
        int w = 200, h2 = 46;
        int bx = switch (Features.MP_POS.index) {
            case 1 -> sw - w - 4;
            default -> 4;
        };
        int by = switch (Features.MP_POS.index) {
            case 2 -> sh - h2 - 24;
            default -> 24;
        };
        int[] bxs = {bx + w / 2 - 24, bx + w / 2 - 8, bx + w / 2 + 8};
        if (my >= by + 25 && my <= by + 39) {
            if (mx >= bxs[0] && mx <= bxs[0] + 14) { prev(); return true; }
            if (mx >= bxs[1] && mx <= bxs[1] + 14) { toggle(); return true; }
            if (mx >= bxs[2] && mx <= bxs[2] + 14) { next(); return true; }
        }
        return false;
    }
}
