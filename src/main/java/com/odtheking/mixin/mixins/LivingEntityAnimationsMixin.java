package com.odtheking.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.odtheking.odin.features.impl.render.animations.ModuleAnimations;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAnimationsMixin {

    @ModifyExpressionValue(
            method = "getCurrentSwingDuration",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I"),
            require = 0
    )
    private int modifySwingDuration(int duration) {
        if (ModuleAnimations.isActive() && (Object) this == net.minecraft.client.Minecraft.getInstance().player) {
            return ModuleAnimations.swingDuration();
        }
        return duration;
    }
}
