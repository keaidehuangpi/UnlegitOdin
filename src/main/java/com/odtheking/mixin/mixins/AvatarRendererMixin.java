package com.odtheking.mixin.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.odtheking.odin.features.impl.combat.ModuleSwordBlock;
import com.odtheking.odin.features.impl.render.PlayerSize;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {

    @Inject(
            method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void swordBlockArmPose(
            Avatar entity,
            ItemStack stack,
            InteractionHand hand,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<HumanoidModel.ArmPose> cir
    ) {
        if (entity != Minecraft.getInstance().player || !ModuleSwordBlock.shouldApplyToThirdPersonView()) return;

        if (hand == InteractionHand.MAIN_HAND && ModuleSwordBlock.shouldAnimateSwordBlock((net.minecraft.world.entity.LivingEntity) entity, stack)) {
            cir.setReturnValue(HumanoidModel.ArmPose.BLOCK);
        } else if (hand == InteractionHand.OFF_HAND
                && ModuleSwordBlock.shouldHideOffhand(stack, ((Avatar) entity).getMainHandItem())) {
            cir.setReturnValue(HumanoidModel.ArmPose.EMPTY);
        }
    }

    @Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("HEAD"))
    private void scale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        PlayerSize.preRenderCallbackScaleHook(state, poseStack);
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("HEAD")
    )
    private void extractRenderState(Avatar entity, AvatarRenderState avatarRenderState, float f, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer clientAvatarEntity)) return;
        avatarRenderState.setData(PlayerSize.getGAME_PROFILE_KEY(), clientAvatarEntity.getGameProfile());
    }
}
