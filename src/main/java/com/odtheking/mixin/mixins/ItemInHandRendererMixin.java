package com.odtheking.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.odtheking.odin.features.impl.combat.ModuleSwordBlock;
import com.odtheking.odin.features.impl.render.animations.ModuleAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    @Final
    private static float ITEM_POS_Y;

    @Shadow
    private ItemStack offHandItem;

    @Inject(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void applyHandTransform(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!ModuleAnimations.isActive()) return;

        boolean mapWithEmptyOffHand = hand == InteractionHand.MAIN_HAND
                && itemStack.has(DataComponents.MAP_ID)
                && offHandItem.isEmpty();
        ModuleAnimations.applyHandTransform(poseStack, hand, mapWithEmptyOffHand);
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void replaceSwing(
            float attack,
            PoseStack poseStack,
            int invert,
            HumanoidArm arm,
            CallbackInfo ci
    ) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || !ModuleAnimations.isSwingAnimationActive() || arm != player.getMainArm()
                || ModuleSwordBlock.shouldAnimateSwordBlock(player)) return;

        com.odtheking.odin.features.impl.render.animations.SwingAnimations.apply(poseStack, attack, arm);
        ci.cancel();
    }

    @Inject(
            method = "renderArmWithItem",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void transformSwordUse(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (!itemStack.typeHolder().is(ItemTags.SWORDS)) return;

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        if (ModuleAnimations.isActive()) {
            ModuleAnimations.applyBlockAnimation(poseStack, arm, inverseArmHeight, attack);
        } else {
            ModuleAnimations.applyDefaultBlockAnimation(poseStack, arm, attack);
        }
    }

    @ModifyArg(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 3),
            index = 2
    )
    private float ignoreBlockingEquipOffset(float equipProgress) {
        return ModuleAnimations.shouldIgnoreBlocking() ? 0.0F : equipProgress;
    }

    /**
     * The shield makes the client lower both hand heights while it is being used. That vanilla
     * equip animation is correct for a shield, but it must not be applied to the sword that is
     * being rendered as a legacy blockhit, otherwise the sword dips and rises independently of
     * the attack swing.
     */
    @ModifyArg(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
                    ordinal = 1
            ),
            index = 2
    )
    private float stabilizeSwordBlockEquipOffset(
            float equipProgress,
            @Local(argsOnly = true, name = "player") AbstractClientPlayer player,
            @Local(argsOnly = true, name = "hand") InteractionHand hand,
            @Local(argsOnly = true, name = "itemStack") ItemStack itemStack
    ) {
        if (hand == InteractionHand.MAIN_HAND
                && ModuleSwordBlock.shouldAnimateSwordBlock(player, itemStack)) {
            return 0.0F;
        }
        return equipProgress;
    }

    @ModifyArg(
            method = "applyItemArmTransform",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
            index = 1
    )
    private float disableEquipOffset(float y) {
        return ModuleAnimations.isActive() && !ModuleAnimations.isEquipOffsetActive() ? ITEM_POS_Y : y;
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void ignorePlace(InteractionHand hand, CallbackInfo ci) {
        if (ModuleAnimations.shouldIgnorePlace()) ci.cancel();
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"), cancellable = true)
    private void ignoreAmount(ItemStack currentlyVisibleItem, ItemStack expectedItem, CallbackInfoReturnable<Boolean> cir) {
        if (!ModuleAnimations.isActive() || cir.getReturnValueZ()) return;

        if (!ModuleAnimations.isEquipOffsetActive()) {
            cir.setReturnValue(true);
            return;
        }

        boolean sameCount = currentlyVisibleItem.getCount() == expectedItem.getCount();
        if ((sameCount || ModuleAnimations.shouldIgnoreAmount())
                && ItemStack.isSameItemSameComponents(currentlyVisibleItem, expectedItem)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (hand == InteractionHand.OFF_HAND && player == Minecraft.getInstance().player
                && ModuleSwordBlock.shouldHideOffhand(itemStack, player.getMainHandItem())) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;",
                    ordinal = 0
            )
    )
    private ItemUseAnimation forceSwordBlockUseAnimation(
            ItemUseAnimation original,
            @Local(argsOnly = true, name = "itemStack") ItemStack itemStack,
            @Local(argsOnly = true, name = "player") AbstractClientPlayer player
    ) {
        return ModuleSwordBlock.shouldAnimateSwordBlock(player, itemStack) ? ItemUseAnimation.BLOCK : original;
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z",
                    ordinal = 1
            )
    )
    private boolean forceSwordBlockUsingItem(
            boolean original,
            @Local(argsOnly = true, name = "player") AbstractClientPlayer player
    ) {
        return original || ModuleSwordBlock.shouldAnimateSwordBlock(player);
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;",
                    ordinal = 1
            )
    )
    private InteractionHand forceSwordBlockHand(
            InteractionHand original,
            @Local(argsOnly = true, name = "player") AbstractClientPlayer player
    ) {
        return ModuleSwordBlock.shouldAnimateSwordBlock(player) ? InteractionHand.MAIN_HAND : original;
    }

    @ModifyExpressionValue(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I",
                    ordinal = 2
            )
    )
    private int forceSwordBlockUseTicks(
            int original,
            @Local(argsOnly = true, name = "player") AbstractClientPlayer player
    ) {
        return ModuleSwordBlock.shouldAnimateSwordBlock(player) ? 7200 : original;
    }
}
