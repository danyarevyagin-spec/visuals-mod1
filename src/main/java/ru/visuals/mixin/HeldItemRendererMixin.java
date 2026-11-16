package ru.visuals.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.visuals.HandAnim;

/** Положение, размер и анимация руки/предмета от первого лица. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void visuals$transform(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                   float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        HandAnim.apply(matrices, hand, player.getMainArm(), item);
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 2, argsOnly = true, require = 0)
    private float visuals$swing(float swingProgress) {
        return HandAnim.swing(swingProgress);
    }

    @ModifyVariable(method = "renderFirstPersonItem", at = @At("HEAD"), ordinal = 3, argsOnly = true, require = 0)
    private float visuals$equip(float equipProgress) {
        return HandAnim.equip(equipProgress);
    }
}
