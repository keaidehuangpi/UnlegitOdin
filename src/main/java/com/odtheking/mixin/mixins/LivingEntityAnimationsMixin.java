package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.animations.ModuleAnimations;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAnimationsMixin {

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (ModuleAnimations.isActive() && (Object) this == net.minecraft.client.Minecraft.getInstance().player) {
            cir.setReturnValue(ModuleAnimations.modifySwingDuration(cir.getReturnValueI()));
        }
    }
}
