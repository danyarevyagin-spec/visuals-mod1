package ru.visuals;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Оптимизация: временно меняет настройки видео и возвращает их при выключении. */
public final class Optimizer {
    private Optimizer() { }

    private static final Map<String, Object> SAVED = new HashMap<>();
    private static int lastSig = Integer.MIN_VALUE;

    @SuppressWarnings("unchecked")
    private static <T> void put(String key, boolean enabled, net.minecraft.client.option.SimpleOption<T> opt, T target) {
        if (enabled) {
            if (!SAVED.containsKey(key)) SAVED.put(key, opt.getValue());
            if (!Objects.equals(opt.getValue(), target)) opt.setValue(target);
        } else {
            restoreOne(key, opt);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void restoreOne(String key, net.minecraft.client.option.SimpleOption<T> opt) {
        if (!SAVED.containsKey(key)) return;
        T v = (T) SAVED.remove(key);
        if (opt != null && !Objects.equals(opt.getValue(), v)) opt.setValue(v);
    }

    public static void restoreAll() {
        // без доступа к mc — просто очищаем; следующий tick восстановит через put(enabled=false)
        SAVED.clear();
        lastSig = Integer.MIN_VALUE;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.options == null) return;
        if (!Features.OPT.on()) {
            if (!SAVED.isEmpty()) SAVED.clear();
            return;
        }
        int sig = Objects.hash(
                Features.OPT_VIEW_ON.value, Features.OPT_VIEW.geti(),
                Features.OPT_SIM_ON.value, Features.OPT_SIM.geti(),
                Features.OPT_CLOUDS.value, Features.OPT_SHADOW.value,
                Features.OPT_BLEND.value, Features.OPT_VSYNC.value,
                Features.OPT_FPS_ON.value, Features.OPT_FPS.geti());
        if (sig == lastSig) return;
        lastSig = sig;

        put("view", Features.OPT_VIEW_ON.value, mc.options.getViewDistance(), Features.OPT_VIEW.geti());
        put("sim",  Features.OPT_SIM_ON.value,  mc.options.getSimulationDistance(), Features.OPT_SIM.geti());
        put("shadow", Features.OPT_SHADOW.value, mc.options.getEntityShadows(), Boolean.FALSE);
        put("blend",  Features.OPT_BLEND.value,  mc.options.getBiomeBlendRadius(), 1);
        put("vsync",  Features.OPT_VSYNC.value,  mc.options.getEnableVsync(), Boolean.FALSE);
        put("fps",    Features.OPT_FPS_ON.value,  mc.options.getMaxFps(), Features.OPT_FPS.geti());
    }

    public static float particleScale(MinecraftClient mc) {
        if (!Features.OPT.on() || !Features.OPT_ADAPT.value) return 1f;
        float fps = mc.getCurrentFps();
        float target = Features.OPT_ADAPT_FPS.getf();
        if (fps <= 0f) return 1f;
        return Math.max(0.25f, Math.min(1f, fps / target));
    }
}
