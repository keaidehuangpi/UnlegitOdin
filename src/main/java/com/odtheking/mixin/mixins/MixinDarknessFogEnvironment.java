package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.AntiDebuff;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DarknessFogEnvironment.class)
public abstract class MixinDarknessFogEnvironment {
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void disableDarknessFog(FogData fogData, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getDarkness()) {
            fogData.environmentalStart = Float.MAX_VALUE;
            fogData.environmentalEnd = Float.MAX_VALUE;
            fogData.skyEnd = Float.MAX_VALUE;
            fogData.cloudEnd = Float.MAX_VALUE;
            ci.cancel();
        }
    }

    @Shadow public abstract Holder<MobEffect> getMobEffect();
    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    public void getModifiedDarkness(LivingEntity livingEntity, float f, float g, CallbackInfoReturnable<Float> cir) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getDarkness()) cir.setReturnValue(0f);
    }
}
