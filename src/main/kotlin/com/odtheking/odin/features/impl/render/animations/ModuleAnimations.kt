package com.odtheking.odin.features.impl.render.animations

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemUseAnimation
import net.minecraft.tags.ItemTags
import org.joml.Quaternionf
import kotlin.math.min
import kotlin.math.roundToInt

/** First-person item and swing animation customization. */
@Suppress("MagicNumber")
object ModuleAnimations : Module(
    name = "Animations",
    description = "Customizes first-person item, blocking, and swing animations.",
    category = Category.RENDER,
) {
    private val mainHandEnabled by BooleanSetting("Main Hand", false, desc = "Enables custom main-hand item transformations.")
    private val mainHandItemScale by NumberSetting("Main Hand Item Scale", 0f, -5f, 5f, 0.01f, desc = "Moves the main-hand item along the view axis.").withDependency { mainHandEnabled }
    private val mainHandX by NumberSetting("Main Hand X", 0f, -5f, 5f, 0.01f, desc = "Moves the main-hand item on the X axis.").withDependency { mainHandEnabled }
    private val mainHandY by NumberSetting("Main Hand Y", 0f, -5f, 5f, 0.01f, desc = "Moves the main-hand item on the Y axis.").withDependency { mainHandEnabled }
    private val mainHandPositiveX by NumberSetting("Main Hand Rotation X", 0f, -50f, 50f, 0.1f, desc = "Rotates the main-hand item around X.").withDependency { mainHandEnabled }
    private val mainHandPositiveY by NumberSetting("Main Hand Rotation Y", 0f, -50f, 50f, 0.1f, desc = "Rotates the main-hand item around Y.").withDependency { mainHandEnabled }
    private val mainHandPositiveZ by NumberSetting("Main Hand Rotation Z", 0f, -50f, 50f, 0.1f, desc = "Rotates the main-hand item around Z.").withDependency { mainHandEnabled }

    private val offHandEnabled by BooleanSetting("Off Hand", false, desc = "Enables custom off-hand item transformations.")
    private val offHandItemScale by NumberSetting("Off Hand Item Scale", 0f, -5f, 5f, 0.01f, desc = "Moves the off-hand item along the view axis.").withDependency { offHandEnabled }
    private val offHandX by NumberSetting("Off Hand X", 0f, -1f, 1f, 0.01f, desc = "Moves the off-hand item on the X axis.").withDependency { offHandEnabled }
    private val offHandY by NumberSetting("Off Hand Y", 0f, -1f, 1f, 0.01f, desc = "Moves the off-hand item on the Y axis.").withDependency { offHandEnabled }
    private val offHandPositiveX by NumberSetting("Off Hand Rotation X", 0f, -50f, 50f, 0.1f, desc = "Rotates the off-hand item around X.").withDependency { offHandEnabled }
    private val offHandPositiveY by NumberSetting("Off Hand Rotation Y", 0f, -50f, 50f, 0.1f, desc = "Rotates the off-hand item around Y.").withDependency { offHandEnabled }
    private val offHandPositiveZ by NumberSetting("Off Hand Rotation Z", 0f, -50f, 50f, 0.1f, desc = "Rotates the off-hand item around Z.").withDependency { offHandEnabled }

    private val swingAnimationsEnabled by BooleanSetting("Swing Animations", false, desc = "Replaces the vanilla attack swing animation.")
    private val swingMode by SelectorSetting(
        "Swing Mode",
        "Spin",
        SwingAnimations.Mode.entries.map { it.tag },
        desc = "Animation used when swinging the main-hand item."
    ).withDependency { swingAnimationsEnabled }
    private val swingDuration by NumberSetting("Swing Duration", 6, 1, 20, 1, desc = "Duration of the local player's swing in ticks.")
    private val swingSpeedMultiplier by NumberSetting(
        "Swing Speed Multiplier", 1f, 0.1f, 5f, 0.01f,
        desc = "Multiplies the vanilla swing duration."
    )

    // Ancient Animations' configurable 1.7 swing arc.
    private val legacySwingTransX by NumberSetting("1.7 Swing Trans X", 0.240f, -1f, 1f, 0.001f, desc = "Horizontal translation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingTransY by NumberSetting("1.7 Swing Trans Y", 0.128f, -1f, 1f, 0.001f, desc = "Vertical translation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingTransZ by NumberSetting("1.7 Swing Trans Z", -0.052f, -1f, 1f, 0.001f, desc = "Depth translation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingRotX by NumberSetting("1.7 Swing Rot X", -65f, -180f, 180f, 0.1f, desc = "X rotation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingRotY by NumberSetting("1.7 Swing Rot Y", 25f, -180f, 180f, 0.1f, desc = "First Y rotation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingRotZ by NumberSetting("1.7 Swing Rot Z", -7.5f, -180f, 180f, 0.1f, desc = "Z rotation of the 1.7 swing.").withDependency { swingAnimationsEnabled }
    private val legacySwingRotY2 by NumberSetting("1.7 Swing Rot Y2", 50f, -180f, 180f, 0.1f, desc = "Final Y rotation of the 1.7 swing.").withDependency { swingAnimationsEnabled }

    private val eatPunchEnabled by BooleanSetting("Eating/Drinking Punch", false, desc = "Swings while consuming food or potions.")
    private val bowPunchEnabled by BooleanSetting("Bow Draw Punch", false, desc = "Swings while drawing a bow or crossbow.")

    private val legacyItemTransformEnabled by BooleanSetting("1.7 Item Transform", false, desc = "Applies the classic configurable transform to tools.")
    private val legacyItemPosX by NumberSetting("1.7 Item Pos X", 0f, -1f, 1f, 0.001f, desc = "Horizontal item offset.").withDependency { legacyItemTransformEnabled }
    private val legacyItemPosY by NumberSetting("1.7 Item Pos Y", 0f, -1f, 1f, 0.001f, desc = "Vertical item offset.").withDependency { legacyItemTransformEnabled }
    private val legacyItemPosZ by NumberSetting("1.7 Item Pos Z", 0f, -1f, 1f, 0.001f, desc = "Depth item offset.").withDependency { legacyItemTransformEnabled }
    private val legacyItemRotX by NumberSetting("1.7 Item Rot X", 0f, -180f, 180f, 0.1f, desc = "X item rotation.").withDependency { legacyItemTransformEnabled }
    private val legacyItemRotY by NumberSetting("1.7 Item Rot Y", 0f, -180f, 180f, 0.1f, desc = "Y item rotation.").withDependency { legacyItemTransformEnabled }
    private val legacyItemRotZ by NumberSetting("1.7 Item Rot Z", 0f, -180f, 180f, 0.1f, desc = "Z item rotation.").withDependency { legacyItemTransformEnabled }
    private val legacyItemScale by NumberSetting("1.7 Item Scale", 1f, 0.1f, 3f, 0.01f, desc = "Item scale.").withDependency { legacyItemTransformEnabled }

    private val blockAnimation by SelectorSetting(
        "Blocking Animation",
        "1.7",
        listOf("1.7", "Pushdown", "Sigma", "Exhibition", "Avatar", "Dortware"),
        desc = "Transformation used while using a sword."
    ).withDependency { enabled }
    private val oneSevenY by NumberSetting("1.7 Y", 0.1f, 0.05f, 0.3f, 0.01f, desc = "Vertical offset for the 1.7 animation.").withDependency { enabled && blockAnimation == 0 }
    private val oneSevenSwingScale by NumberSetting("1.7 Swing Scale", 0.9f, 0.1f, 1f, 0.01f, desc = "Swing progress scale for the 1.7 animation.").withDependency { enabled && blockAnimation == 0 }
    private val sigmaY by NumberSetting("Sigma Y", 0.1f, 0.05f, 0.3f, 0.01f, desc = "Vertical offset for the Sigma animation.").withDependency { enabled && blockAnimation == 2 }
    private val exhibitionY by NumberSetting("Exhibition Y", 0.1f, 0.05f, 0.3f, 0.01f, desc = "Vertical offset for the Exhibition animation.").withDependency { enabled && blockAnimation == 3 }
    private val avatarY by NumberSetting("Avatar Y", 0.1f, 0.05f, 0.3f, 0.01f, desc = "Vertical offset for the Avatar animation.").withDependency { enabled && blockAnimation == 4 }
    private val dortwareY by NumberSetting("Dortware Y", 0.1f, 0.05f, 0.3f, 0.01f, desc = "Vertical offset for the Dortware animation.").withDependency { enabled && blockAnimation == 5 }

    private val equipOffsetEnabled by BooleanSetting("Equip Offset", true, desc = "Keeps the vanilla item equip movement.")
    private val ignoreBlocking by BooleanSetting("Ignore Blocking", false, desc = "Disables equip movement while blocking.").withDependency { enabled && equipOffsetEnabled }
    private val ignorePlace by BooleanSetting("Ignore Place", false, desc = "Disables equip movement after placing an item.").withDependency { enabled && equipOffsetEnabled }
    private val ignoreAmount by BooleanSetting("Ignore Amount", false, desc = "Ignores stack count changes when deciding whether to re-equip.").withDependency { enabled && equipOffsetEnabled }
    private val airWalker by BooleanSetting("Air Walker", false, desc = "Applies the walk animation while airborne.").withDependency { enabled }

    @JvmStatic
    fun isActive(): Boolean = enabled

    @JvmStatic
    fun isSwingAnimationActive(): Boolean = enabled && swingAnimationsEnabled

    @JvmStatic
    fun isMainHandActive(): Boolean = enabled && mainHandEnabled

    @JvmStatic
    fun isOffHandActive(): Boolean = enabled && offHandEnabled

    @JvmStatic
    fun shouldApplyLegacyItemTransform(stack: ItemStack): Boolean =
        enabled && legacyItemTransformEnabled && isTool(stack)

    @JvmStatic
    fun isEquipOffsetActive(): Boolean = enabled && equipOffsetEnabled

    @JvmStatic
    fun shouldIgnoreBlocking(): Boolean = enabled && equipOffsetEnabled && ignoreBlocking

    @JvmStatic
    fun shouldIgnorePlace(): Boolean = enabled && equipOffsetEnabled && ignorePlace

    @JvmStatic
    fun shouldIgnoreAmount(): Boolean = enabled && equipOffsetEnabled && ignoreAmount

    @JvmStatic
    fun swingDuration(): Int = swingDuration

    /** Applies the source module's swing-duration multiplier to vanilla's duration. */
    @JvmStatic
    fun modifySwingDuration(original: Int): Int {
        if (!enabled) return original
        val baseDuration = if (swingAnimationsEnabled && swingDuration != 6) swingDuration else original
        val duration = (baseDuration * swingSpeedMultiplier).roundToInt()
        return duration.coerceAtLeast(1)
    }

    /** True when the active item should receive Ancient Animations' consume punch. */
    @JvmStatic
    fun shouldPunchWhileUsing(entity: LivingEntity): Boolean {
        if (!enabled || !entity.isUsingItem) return false
        val stack = entity.useItem
        return (eatPunchEnabled && isEatingOrDrinking(stack)) ||
            (bowPunchEnabled && isDrawingBow(stack))
    }

    @JvmStatic
    fun shouldPunchWhileUsing(entity: LivingEntity, renderedStack: ItemStack): Boolean {
        if (!shouldPunchWhileUsing(entity)) return false
        val activeAction = entity.useItem.getUseAnimation()
        return renderedStack.getUseAnimation() == activeAction
    }

    @JvmStatic
    fun isEatingOrDrinking(stack: ItemStack): Boolean =
        stack.getUseAnimation() == ItemUseAnimation.EAT || stack.getUseAnimation() == ItemUseAnimation.DRINK

    @JvmStatic
    fun isDrawingBow(stack: ItemStack): Boolean =
        stack.getUseAnimation() == ItemUseAnimation.BOW || stack.getUseAnimation() == ItemUseAnimation.CROSSBOW

    internal fun swingModeName(): SwingAnimations.Mode = SwingAnimations.Mode.entries[swingMode]

    /** Exact configurable 1.7 arc from Ancient Animations. */
    @JvmStatic
    fun applyLegacySwing(poseStack: PoseStack, swingProgress: Float) {
        val progress = swingProgress.coerceIn(0f, 1f)
        val arc = Mth.sin(Mth.sqrt(progress) * Math.PI)
        poseStack.translate(arc * legacySwingTransX, 0f, 0f)
        poseStack.translate(0f, arc * legacySwingTransY, 0f)
        poseStack.translate(0f, 0f, arc * legacySwingTransZ)
        poseStack.mulPose(Axis.YP.rotationDegrees(arc * legacySwingRotY))
        poseStack.mulPose(Axis.XP.rotationDegrees(arc * legacySwingRotX))
        poseStack.mulPose(Axis.ZP.rotationDegrees(arc * legacySwingRotZ))
        poseStack.mulPose(Axis.YP.rotationDegrees(arc * legacySwingRotY2))
    }

    /** Ancient Animations' old eating/drinking punch. */
    @JvmStatic
    fun applyEatPunch(poseStack: PoseStack, swingProgress: Float) {
        val progress = swingProgress.coerceIn(0f, 1f)
        val arc = Mth.sin(Mth.sqrt(progress) * Math.PI)
        poseStack.translate(arc * -0.4f, 0f, 0f)
        poseStack.mulPose(Axis.YP.rotationDegrees(arc * -20f))
        poseStack.mulPose(Axis.XP.rotationDegrees(arc * -80f))
    }

    /** Ancient Animations' old bow-draw punch. */
    @JvmStatic
    fun applyBowPunch(poseStack: PoseStack, swingProgress: Float) {
        val progress = swingProgress.coerceIn(0f, 1f)
        val sqrtArc = Mth.sin(Mth.sqrt(progress) * Math.PI)
        val tx = -0.4f * sqrtArc
        val ty = 0.2f * Mth.sin(Mth.sqrt(progress) * Math.PI * 2.0)
        val tz = -0.2f * Mth.sin(progress * Math.PI)
        poseStack.translate(tx, ty, tz)

        val f = Mth.sin(progress * progress * Math.PI)
        poseStack.mulPose(Axis.YP.rotationDegrees(f * 20f))
        poseStack.mulPose(Axis.ZP.rotationDegrees(sqrtArc * 20f))
        poseStack.mulPose(Axis.XP.rotationDegrees(sqrtArc * -80f))
    }

    @JvmStatic
    fun modifyStride(original: Float): Float {
        if (!enabled || !airWalker) return original
        val player = OdinMod.mc.player ?: return original
        return min(0.1, player.deltaMovement.horizontalDistance()).toFloat()
    }

    /** Applies the source module's hand transform, including its map-in-both-hands behavior. */
    @JvmStatic
    fun applyHandTransform(poseStack: PoseStack, hand: InteractionHand, mapWithEmptyOffHand: Boolean) {
        if (mapWithEmptyOffHand && hand == InteractionHand.MAIN_HAND) {
            when {
                mainHandEnabled && offHandEnabled -> applyTransform(
                    poseStack,
                    (mainHandX + offHandX) / 2f,
                    (mainHandY + offHandY) / 2f,
                    (mainHandItemScale + offHandItemScale) / 2f,
                    (mainHandPositiveX + offHandPositiveX) / 2f,
                    (mainHandPositiveY + offHandPositiveY) / 2f,
                    (mainHandPositiveZ + offHandPositiveZ) / 2f,
                )
                mainHandEnabled -> poseStack.translate(0f, 0f, mainHandItemScale)
                offHandEnabled -> applyTransform(poseStack, offHandX, offHandY, offHandItemScale, offHandPositiveX, offHandPositiveY, offHandPositiveZ)
            }
            return
        }

        if (hand == InteractionHand.MAIN_HAND && mainHandEnabled) {
            applyTransform(poseStack, mainHandX, mainHandY, mainHandItemScale, mainHandPositiveX, mainHandPositiveY, mainHandPositiveZ)
        } else if (hand == InteractionHand.OFF_HAND && offHandEnabled) {
            applyTransform(poseStack, offHandX, offHandY, offHandItemScale, offHandPositiveX, offHandPositiveY, offHandPositiveZ)
        }
    }

    /** Applies the source module's tool-only item transform after vanilla's hand base transform. */
    @JvmStatic
    fun applyLegacyItemTransform(poseStack: PoseStack, arm: HumanoidArm, stack: ItemStack) {
        if (!shouldApplyLegacyItemTransform(stack)) return
        poseStack.translate(legacyItemPosX, legacyItemPosY, legacyItemPosZ)
        poseStack.mulPose(Axis.XP.rotationDegrees(legacyItemRotX))
        poseStack.mulPose(Axis.YP.rotationDegrees(legacyItemRotY))
        poseStack.mulPose(Axis.ZP.rotationDegrees(legacyItemRotZ))
        poseStack.scale(legacyItemScale, legacyItemScale, legacyItemScale)
    }

    /** Same tool classification used by Ancient Animations. */
    @JvmStatic
    fun isTool(stack: ItemStack): Boolean =
        stack.`is`(ItemTags.SWORDS) || stack.`is`(ItemTags.AXES) ||
            stack.`is`(ItemTags.PICKAXES) || stack.`is`(ItemTags.SHOVELS) ||
            stack.`is`(ItemTags.HOES) || stack.`is`(Items.BOW) ||
            stack.`is`(Items.CROSSBOW) || stack.`is`(Items.TRIDENT)

    @JvmStatic
    fun applyBlockAnimation(poseStack: PoseStack, arm: HumanoidArm, equipProgress: Float, swingProgress: Float) {
        when (blockAnimation) {
            0 -> oneSeven(poseStack, arm, swingProgress)
            1 -> pushdown(poseStack, arm, swingProgress)
            2 -> sigma(poseStack, arm, swingProgress)
            3 -> exhibition(poseStack, arm, swingProgress)
            4 -> avatar(poseStack, arm, swingProgress)
            else -> dortware(poseStack, arm, swingProgress)
        }
    }

    @JvmStatic
    fun applyDefaultBlockAnimation(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        oneSeven(poseStack, arm, swingProgress)
    }

    private fun applyTransform(poseStack: PoseStack, x: Float, y: Float, z: Float, rotateX: Float, rotateY: Float, rotateZ: Float) {
        poseStack.translate(x, y, z)
        poseStack.mulPose(Axis.XP.rotationDegrees(rotateX))
        poseStack.mulPose(Axis.YP.rotationDegrees(rotateY))
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotateZ))
    }

    private fun applySwingOffset(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        val side = if (arm == HumanoidArm.RIGHT) 1 else -1
        val f = Mth.sin(swingProgress * swingProgress * Math.PI)
        poseStack.mulPose(Axis.YP.rotationDegrees(side * (45f + f * -20f)))
        val g = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * g * -20f))
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80f))
        poseStack.mulPose(Axis.YP.rotationDegrees(side * -45f))
    }

    private fun oneSeven(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        poseStack.translate(if (arm == HumanoidArm.RIGHT) -0.1f else 0.1f, oneSevenY, 0f)
        applySwingOffset(poseStack, arm, swingProgress * oneSevenSwingScale)
    }

    private fun pushdown(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        poseStack.translate(if (arm == HumanoidArm.RIGHT) -0.1f else 0.1f, 0.1f, 0f)
        val g = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        poseStack.mulPose(Axis.ZP.rotationDegrees((if (arm == HumanoidArm.RIGHT) 1 else -1) * g * 10f))
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -35f))
    }

    private fun sigma(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        val side = if (arm == HumanoidArm.RIGHT) 1f else -1f
        val rot = Quaternionf().rotationAxis(
            Math.toRadians((-sine * 27.5f * side).toDouble()).toFloat(), -8f * side, 0f, 9f
        ).rotateAxis(
            Math.toRadians((-sine * 45f * side).toDouble()).toFloat(), 1f * side, sine / 2f, 0f
        )
        poseStack.mulPose(rot)
        poseStack.translate(0f, sigmaY, 0f)
        applySwingOffset(poseStack, arm, 0f)
    }

    private fun exhibition(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        val side = if (arm == HumanoidArm.RIGHT) 1f else -1f
        poseStack.translate(0.0, -0.1, 0.0)
        applySwingOffset(poseStack, arm, 0f)
        poseStack.translate(0.1f, 0.4f, -0.1f)
        val rot = Quaternionf().rotationAxis(
            Math.toRadians((-sine * 30f * side).toDouble()).toFloat(), sine / 2f, 0f, 9f
        ).rotateAxis(
            Math.toRadians((-sine * 50f * side).toDouble()).toFloat(), 0.8f * side, sine / 2f, 0f
        )
        poseStack.mulPose(rot)
        poseStack.translate(0f, exhibitionY - 0.2f, 0f)
    }

    private fun avatar(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        val sine1 = Mth.sin(swingProgress * swingProgress * Math.PI)
        val side = if (arm == HumanoidArm.RIGHT) 1f else -1f
        val rot = Quaternionf()
            .rotateY(Math.toRadians((sine1 * -20f * side).toDouble()).toFloat())
            .rotateZ(Math.toRadians((sine * -20f * side).toDouble()).toFloat())
            .rotateAxis(Math.toRadians((sine * -40f * side).toDouble()).toFloat(), side, 0f, 0f)
        poseStack.translate(0.2f * side, avatarY, 0f)
        poseStack.mulPose(rot)
        applySwingOffset(poseStack, arm, 0f)
    }

    private fun dortware(poseStack: PoseStack, arm: HumanoidArm, swingProgress: Float) {
        val sine = Mth.sin(Mth.sqrt(swingProgress) * Math.PI)
        val sqrtSwing = Mth.sqrt(swingProgress)
        val altSine = Mth.sin(sqrtSwing * Math.PI - 3.0)
        val rot = Quaternionf().rotationAxis(
            Math.toRadians((-sine * 10f).toDouble()).toFloat(), 0f, 15f, 200f
        ).rotateAxis(
            Math.toRadians((-sine * 10f).toDouble()).toFloat(), 300f, sine / 2f, 1f
        )
        poseStack.mulPose(rot)
        poseStack.translate(3.4, 0.3, -0.4)
        poseStack.translate(-2.1f, -0.2f, 0.1f)
        poseStack.mulPose(Quaternionf().rotationAxis(
            Math.toRadians((altSine * 13f).toDouble()).toFloat(), -10f, -1.4f, -10f
        ))
        poseStack.translate(if (arm == HumanoidArm.RIGHT) -1f else -2f, dortwareY, 0f)
    }
}
