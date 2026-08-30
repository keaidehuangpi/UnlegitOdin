package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.AntiDebuff;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlindnessFogEnvironment.class)
public class MixinBlindnessFogEnvironment {
    @Inject(method = "getMobEffect", at = @At("HEAD"), cancellable = true)
    public void hookGetStatusEffect(CallbackInfoReturnable<Holder<MobEffect>> cir) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getBlinding()) cir.setReturnValue(null);
    }
}
