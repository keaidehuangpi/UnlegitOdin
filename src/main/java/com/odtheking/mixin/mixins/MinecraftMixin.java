package com.odtheking.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.odtheking.odin.events.BlockInteractEvent;
import com.odtheking.odin.events.EntityInteractEvent;
import com.odtheking.odin.features.impl.combat.ModuleSwordBlock;
import com.odtheking.odin.features.impl.render.animations.ModuleAnimations;
import com.odtheking.odin.features.impl.boss.TerminalSolver;
import com.odtheking.odin.utils.skyblock.dungeon.terminals.TerminalUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public HitResult hitResult;

    private boolean ancientPunchStarted;

    /** Recreates the old attack-while-using loop for food, potions, bows, and crossbows. */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void ancientAnimationsPunch(CallbackInfo ci) {
        LocalPlayer player = ((Minecraft) (Object) this).player;
        if (player == null || !ModuleAnimations.shouldPunchWhileUsing(player)) {
            ancientPunchStarted = false;
            return;
        }

        Minecraft minecraft = (Minecraft) (Object) this;
        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean useDown = minecraft.options.keyUse.isDown();
        if (!attackDown || !useDown) {
            if (!attackDown) ancientPunchStarted = false;
            return;
        }

        // The first attack starts immediately; subsequent swings are limited by LivingEntity.swing().
        if (!ancientPunchStarted || minecraft.hitResult instanceof BlockHitResult) {
            player.swing(InteractionHand.MAIN_HAND);
            ancientPunchStarted = true;
        }
    }

    /**
     * While an item is being used (the shield case), vanilla consumes attack
     * clicks without entering startAttack(), so no swing state is created for
     * the first-person renderer. Turn that consumed click into a client-side
     * main-hand swing for SwordBlock. ClientLevel swings do not send packets.
     */
    @ModifyExpressionValue(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z",
                    ordinal = 13
            )
    )
    private boolean swordBlockVisualSwing(boolean clicked) {
        if (!clicked) return false;

        LocalPlayer player = ((Minecraft) (Object) this).player;
        if (player != null && ModuleSwordBlock.shouldAnimateSwordBlock(player)) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
    private void cancelBlockUse(CallbackInfo ci) {
        if (!(this.hitResult instanceof BlockHitResult blockHitResult)) return;
        if ((new BlockInteractEvent(blockHitResult.getBlockPos()).postAndCatch())) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
    private void cancelEntityUse(CallbackInfo ci) {
        if (!(this.hitResult instanceof EntityHitResult entityHitResult)) return;
        if (new EntityInteractEvent(entityHitResult.getLocation(), entityHitResult.getEntity()).postAndCatch()) ci.cancel();
    }

    @ModifyExpressionValue(method = "resizeGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object modifyGuiScaleValue(Object original) {
        if (TerminalUtils.getCurrentTerm() != null && TerminalSolver.getTermSize() != (Integer) original) return TerminalSolver.getTermSize();
        return original;
    }
}
