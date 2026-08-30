package com.odtheking.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.odtheking.odin.features.impl.combat.ModuleSwordBlock;
import com.odtheking.odin.features.impl.skyblock.OverlayType;
import com.odtheking.odin.features.impl.skyblock.PlayerDisplay;
import com.odtheking.odin.features.impl.render.AntiDebuff;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @ModifyExpressionValue(
            method = "extractItemHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z")
    )
    private boolean hideShieldHotbarSlot(boolean original) {
        return original || ModuleSwordBlock.INSTANCE.getHideShieldSlot() && ModuleSwordBlock.shouldHideOffhand();
    }

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void cancelArmorBar(GuiGraphicsExtractor graphics, Player player, int yLineBase, int numHealthRows, int healthRowHeight, int xLeft, CallbackInfo ci) {
        if (PlayerDisplay.shouldCancelOverlay(OverlayType.ARMOR)) ci.cancel();
    }

    @Inject(method = "extractHearts", at = @At("HEAD"), cancellable = true)
    private void cancelHealthBar(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase, int healthRowHeight, int heartOffsetIndex, float maxHealth, int currentHealth, int oldHealth, int absorption, boolean blink, CallbackInfo ci) {
        if (PlayerDisplay.shouldCancelOverlay(OverlayType.HEARTS)) ci.cancel();
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void cancelFoodBar(GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, CallbackInfo ci) {
        if (PlayerDisplay.shouldCancelOverlay(OverlayType.FOOD)) ci.cancel();
    }

    @ModifyExpressionValue(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z"))
    private boolean cancelXPLevelRender(boolean original) {
        if (PlayerDisplay.shouldCancelOverlay(OverlayType.XP)) return false;
        return original;
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void hookNauseaOverlay(GuiGraphicsExtractor graphics, float distortionStrength, CallbackInfo ci) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getNausea()) ci.cancel();
    }

    @ModifyArg(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"), index = 1)
    private float disableNauseaOverlay(float strength) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getNausea()) return 0f;
        return strength;
    }

    @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(GuiGraphicsExtractor graphics, Identifier texture, float opacity, CallbackInfo ci) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getPumpkinBlur() && AntiDebuff.TEXTURE_PUMPKIN_BLUR.equals(texture)) ci.cancel();
    }

    @Redirect(method = "extractCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float disableNauseaBlend(LocalPlayer player, Holder<MobEffect> effect, float partialTick) {
        if (AntiDebuff.INSTANCE.getEnabled() && AntiDebuff.INSTANCE.getNausea() && effect == MobEffects.NAUSEA) return 0f;
        return player.getEffectBlendFactor(effect, partialTick);
    }
}

