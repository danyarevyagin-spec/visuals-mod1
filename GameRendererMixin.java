package ru.visuals.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ru.visuals.AspectRatio;

/** Подмена соотношения сторон проекции (растяжение экрана). Если вызов не найден, функция просто не работает. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyArg(method = "getBasicProjectionMatrix",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"),
            index = 1, require = 0)
    private float visuals$aspectA(float aspect) {
        return AspectRatio.apply(aspect);
    }

    @ModifyArg(method = "getBasicProjectionMatrix",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setPerspective(FFFF)Lorg/joml/Matrix4f;"),
            index = 1, require = 0)
    private float visuals$aspectB(float aspect) {
        return AspectRatio.apply(aspect);
    }
}
