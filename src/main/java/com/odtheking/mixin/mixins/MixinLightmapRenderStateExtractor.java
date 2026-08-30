package com.odtheking.mixin.mixins;

import com.odtheking.odin.features.impl.render.AntiDebuff;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class MixinLightmapRenderStateExtractor {
    @ModifyVariable(method = "extract", at = @At(value = "STORE"), name = "darknessEffectScaleOption")
    private float disableDarknessLightmap(float darknessEffectScaleOption) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getDarkness()) return 0f;
        return darknessEffectScaleOption;
    }
}
