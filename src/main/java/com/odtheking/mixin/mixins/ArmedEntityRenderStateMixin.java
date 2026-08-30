package com.odtheking.mixin.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.odtheking.odin.features.impl.combat.ModuleSwordBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmedEntityRenderState.class)
public abstract class ArmedEntityRenderStateMixin {

    @WrapOperation(
            method = "extractArmedEntityRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getItemHeldByArm(Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/world/item/ItemStack;")
    )
    private static ItemStack hideOffhandShield(
            LivingEntity entity,
            HumanoidArm arm,
            Operation<ItemStack> original,
            @Local(argsOnly = true, name = "state") ArmedEntityRenderState state
    ) {
        if (entity == Minecraft.getInstance().player
                && ModuleSwordBlock.shouldApplyToThirdPersonView()
                && ModuleSwordBlock.shouldHideOffhand()
                && arm != state.mainArm) {
            return ItemStack.EMPTY;
        }

        return original.call(entity, arm);
    }
}
