package ru.visuals.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.visuals.Features;

/** Прячет ванильный прицел, когда включён свой. Если метод не найден — мод всё равно запустится. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void visuals$hideCrosshair(CallbackInfo ci) {
        if (Features.CROSS.on() && Features.X_HIDE.value) ci.cancel();
    }
}
