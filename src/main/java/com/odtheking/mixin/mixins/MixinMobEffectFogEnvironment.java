package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.AntiDebuff;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MixinMobEffectFogEnvironment {
    @Shadow
    public abstract Holder<MobEffect> getMobEffect();

    @Inject(method = "isApplicable", at = @At("RETURN"), cancellable = true)
    private void disableDebuffFog(net.minecraft.world.level.material.FogType fogType, net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !AntiDebuff.INSTANCE.getEnabled()) return;
        Holder<MobEffect> effect = getMobEffect();
        if (effect == MobEffects.BLINDNESS && AntiDebuff.INSTANCE.getBlinding()) cir.setReturnValue(false);
        if (effect == MobEffects.DARKNESS && AntiDebuff.INSTANCE.getDarkness()) cir.setReturnValue(false);
    }
}
