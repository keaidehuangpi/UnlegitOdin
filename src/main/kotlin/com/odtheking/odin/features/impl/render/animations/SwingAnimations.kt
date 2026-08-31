package com.odtheking.odin.features.impl.render.animations

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.util.Mth
import net.minecraft.world.entity.HumanoidArm

/** Alternative first-person attack swing transforms from LiquidBounce. */
@Suppress("MagicNumber")
object SwingAnimations {
    enum class Mode(val tag: String) {
        Swipe("Swipe"), Spin("Spin"), Hook("Hook"), Dash("Dash"),
        Tap("Tap"), Inject("Inject"), Slap("Slap"), Akrien("Akrien"),
        Smooth("Smooth"), Power("Power"), Feast("Feast")
    }

    private const val PI = Math.PI.toFloat()

    private fun sin(value: Float) = Mth.sin(value.toDouble())

    @JvmStatic
    fun apply(poseStack: PoseStack, swing: Float, arm: HumanoidArm) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1
        val sqrt = Mth.sqrt(swing)
        val g = sin(sqrt * PI)
        val sin1 = sin(swing * swing * PI)
        val sin2 = sin(sqrt * PI)
        val sinSmooth = sin(swing * PI) * 0.5f

        when (ModuleAnimations.swingModeName()) {
            Mode.Swipe -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45f + swing * -20f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -70f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-70f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45f))
            }
            Mode.Spin -> poseStack.mulPose(Axis.XP.rotationDegrees(swing * -360f))
            Mode.Hook -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-30f * (1f - g) - 30f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Dash -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (-60f * g - 50f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 110f))
            }
            Mode.Tap -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(50f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * (110f + 20f * g)))
            }
            Mode.Inject -> {
                poseStack.translate(0.0, 0.0, -g.toDouble() / 4.0)
                poseStack.mulPose(Axis.XP.rotationDegrees(-120f))
            }
            Mode.Slap -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-sin(swing * 3f) * 60f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * -60f * g))
            }
            Mode.Akrien -> {
                if (swing > 0f) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * 45f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(g * -85f))
                    poseStack.translate(i * -0.1f, 0.28f, 0.2f)
                    poseStack.mulPose(Axis.XP.rotationDegrees(-85f))
                } else {
                    val m = 0.2f * sin(sqrt * PI * 2f)
                    val f2 = -0.2f * sin(swing * PI)
                    val n = -0.4f * g
                    poseStack.translate(i * n.toDouble(), m.toDouble(), f2.toDouble())
                    applySwingOffset(poseStack, arm, swing)
                }
            }
            Mode.Smooth -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * (45f + sin1 * -20f)))
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * sin2 * -20f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -80f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * -45f))
                poseStack.translate(0.0, -0.1, 0.0)
            }
            Mode.Power -> {
                poseStack.translate(-sinSmooth * sinSmooth * sin1 * i, 0f, 0f)
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 61f))
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * sin1 * -5f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * sin1 * -30f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-60f))
                poseStack.mulPose(Axis.XP.rotationDegrees(sinSmooth * -60f))
            }
            Mode.Feast -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * 75f * i))
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -45f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 30f))
                poseStack.mulPose(Axis.XP.rotationDegrees(-80f))
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 35f))
            }
        }
    }

    private fun applySwingOffset(poseStack: PoseStack, arm: HumanoidArm, swing: Float) {
        val i = if (arm == HumanoidArm.RIGHT) 1 else -1
        val f1 = sin(swing * swing * PI)
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45f + f1 * -20f)))
        val g = sin(Mth.sqrt(swing) * PI)
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20f))
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80f))
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45f))
    }
}
