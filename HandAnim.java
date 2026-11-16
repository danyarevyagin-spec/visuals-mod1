package ru.visuals;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

public final class HandAnim {
    private HandAnim() { }

    private static long startNs = 0L;
    private static long lastPos = 0L;
    private static float lastV = 0f;

    public static float swing(float v) {
        if (!Features.HAND.on() || !Features.HA_ANIM.value) return v;
        long now = System.nanoTime();
        if (v > 0f) {
            if (now - lastPos > 150_000_000L || v < lastV - 0.2f) startNs = now;
            lastPos = now;
            lastV = v;
        }
        return Features.HA_SUPPRESS.value ? 0f : v;
    }

    public static float equip(float e) {
        if (Features.HAND.on() && Features.HA_EQUIP.value) return e * e * (3f - 2f * e);
        return e;
    }

    private static float ease(int mode, float t) {
        return switch (mode) {
            case 0 -> 1f - (float) Math.pow(1f - t, 3);
            case 1 -> t < 0.5f ? 4 * t * t * t : 1f - (float) Math.pow(-2 * t + 2, 3) / 2f;
            case 2 -> t * t * t;
            case 3 -> t;
            default -> 1f - (float)(Math.exp(-6f * t) * Math.cos(t * 12f));
        };
    }

    public static void apply(MatrixStack m, Hand hand, Arm mainArm, ItemStack item) {
        if (!Features.HAND.on()) return;
        boolean off = hand == Hand.OFF_HAND;
        if (off && !Features.HA_OFFHAND.value) return;
        int mode = Features.HA_APPLY.index;
        boolean empty = item.isEmpty();
        if (mode == 1 && empty) return;
        if (mode == 2 && !empty) return;

        Arm arm = off ? mainArm.getOpposite() : mainArm;
        float side = Features.HA_MIRROR.value ? (arm == Arm.RIGHT ? 1f : -1f) : 1f;
        float px = 0.56f * (arm == Arm.RIGHT ? 1f : -1f);
        float py = -0.52f, pz = -0.72f;

        float ax = 0f, ay = 0f, az = 0f, rx = 0f, ry = 0f, rz = 0f;
        if (Features.HA_ANIM.value && !off && startNs != 0L) {
            float t = (System.nanoTime() - startNs) / (Features.HA_TIME.getf() * 1_000_000f);
            if (t >= 0f && t < 1f) {
                float e = ease(Features.HA_EASE.index, t);
                float b = (float) Math.sin(Math.PI * e);
                float p = Features.HA_POWER.getf();
                switch (Features.HA_TYPE.index) {
                    case 0 -> { } // ванильный — ничего не делаем
                    case 1 -> { // Mode 1: снизу вверх, широкий диагональный
                        rx = -120f * e * p; rz = -40f * b * p * side;
                        ay = 0.08f * b * p; az = 0.1f * b * p;
                    }
                    case 2 -> { // Mode 2: горизонтальный
                        rz = 70f * b * p * side; ry = -20f * b * p * side;
                        ax = 0.08f * b * p * side;
                    }
                    case 3 -> { // HMI: вперёд и назад
                        az = -0.6f * b * p; rx = -8f * b * p; ay = 0.03f * b * p;
                    }
                    case 4 -> { // сверху вниз
                        rx = -90f * b * p; ay = 0.1f * b * p; az = -0.05f * b * p;
                    }
                    case 5 -> { // блок-хит 1.8
                        ry = 35f * b * p * side; rz = -30f * b * p * side;
                        ax = -0.1f * b * p * side; ay = 0.04f * b * p; rx = -20f * b * p;
                    }
                    case 6 -> { // маятник
                        rz = (float)Math.sin(e * Math.PI * 2) * 40f * p * side;
                        ax = (float)Math.sin(e * Math.PI * 2) * 0.08f * p * side;
                    }
                    case 7 -> { // круговой
                        double a = e * Math.PI * 2;
                        ax = (float)Math.sin(a) * 0.18f * p * side;
                        ay = ((float)Math.cos(a) - 1f) * 0.10f * p;
                        rz = (float)Math.sin(a) * 25f * p * side;
                        rx = -20f * b * p;
                    }
                    default -> { // плавный взмах
                        rx = -55f * b * p; ay = -0.06f * b * p;
                        az = -0.12f * b * p; ry = 12f * b * p * side;
                    }
                }
            }
        }

        m.translate(side * Features.HA_X.getf(), Features.HA_Y.getf(), -Features.HA_Z.getf());
        m.translate(px, py, pz);
        float sc = Features.HA_SCALE.getf();
        if (sc != 1f) m.scale(sc, sc, sc);
        m.translate(ax, ay, az);
        float fx = Features.HA_RX.getf() + rx;
        float fy = (Features.HA_RY.getf() + ry) * side;
        float fz = (Features.HA_RZ.getf() + rz) * side;
        if (fx != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(fx));
        if (fy != 0f) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fy));
        if (fz != 0f) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(fz));
        m.translate(-px, -py, -pz);
    }
}
