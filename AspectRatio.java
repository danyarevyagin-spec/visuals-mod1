package ru.visuals;

/** Растяжение картинки: подменяет соотношение сторон при построении проекции мира. */
public final class AspectRatio {
    private AspectRatio() { }

    private static final float[] RATIOS = {4f / 3f, 5f / 4f, 16f / 10f, 16f / 9f, 21f / 9f, 3f / 2f, 1f, 32f / 9f};
    private static float cur = -1f;
    private static long lastNs = 0L;

    private static float targetRatio() {
        int i = Features.AR_PRESET.index;
        return i >= 0 && i < RATIOS.length ? RATIOS[i] : Features.AR_CUSTOM.getf();
    }

    /** Вызывается из миксина вместо исходного соотношения сторон окна. */
    public static float apply(float original) {
        float target = Features.AR.on() ? targetRatio() : original;
        if (cur < 0f) cur = original;
        long now = System.nanoTime();
        float dt = (now - lastNs) / 1_000_000_000f;
        if (dt > 0.002f) {
            lastNs = now;
            if (Features.AR_SMOOTH.value) cur += (target - cur) * Math.min(1f, Math.min(dt, 0.1f) * 10f);
            else cur = target;
        } else if (!Features.AR_SMOOTH.value) {
            cur = target;
        }
        if (Math.abs(cur - target) < 0.002f) cur = target;
        return cur;
    }
}
