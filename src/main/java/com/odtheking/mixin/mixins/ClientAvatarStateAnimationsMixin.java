package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.animations.ModuleAnimations;
import net.minecraft.client.entity.ClientAvatarState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientAvatarState.class)
public abstract class ClientAvatarStateAnimationsMixin {

    @Shadow
    private float bob;

    @Inject(method = "updateBob", at = @At("RETURN"))
    private void applyAirWalker(float movement, CallbackInfo ci) {
        bob = ModuleAnimations.modifyStride(bob);
    }
}
