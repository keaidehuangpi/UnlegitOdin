package com.odtheking.odin.features.impl.combat

import com.odtheking.mixin.accessors.MultiPlayerGameModeAccessor
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Category
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.handlers.schedule
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.tags.ItemTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShieldItem

/** Allows sword blocking on servers which still understand the legacy hand-use interaction. */
object ModuleSwordBlock : Module(
    name = "SwordBlock",
    description = "Allows sword blocking with visual and legacy packet modes.",
    category = Category.custom("Combat"),
) {
    val onlyVisual by BooleanSetting("OnlyVisual", false, desc = "Only changes the client-side blocking animation.")
    val fakeOnPressing by BooleanSetting("FakeOnPressing", false, desc = "Shows blocking while the use key is held.")
    val noShield by BooleanSetting("NoShield", false, desc = "Allows visual sword blocking without a shield in the off hand.")
    val hideShieldSlot by BooleanSetting("HideShieldSlot", false, desc = "Keeps the source module's shield-slot option.")
    val applyToThirdPersonView by BooleanSetting("ApplyToThirdPersonView", true, desc = "Applies sword blocking to your third-person model.")
    private val alwaysHideShield by BooleanSetting("AlwaysHideShield", false, desc = "Hides a shield in the off hand whenever SwordBlock is enabled.")

    /** True when the entity should render a sword as if it were being used. */
    @JvmStatic
    @JvmOverloads
    fun shouldAnimateSwordBlock(entity: LivingEntity, mainHandItem: ItemStack = entity.mainHandItem): Boolean {
        return enabled && shouldApplySwordBlock(entity) && isSword(mainHandItem)
    }

    @JvmStatic
    fun shouldApplyToThirdPersonView(): Boolean = enabled && applyToThirdPersonView

    @JvmStatic
    @JvmOverloads
    fun shouldHideOffhand(
        offHandStack: ItemStack = mc.player?.offhandItem ?: ItemStack.EMPTY,
        mainHandStack: ItemStack = mc.player?.mainHandItem ?: ItemStack.EMPTY,
    ): Boolean {
        if (!enabled || offHandStack.item !is ShieldItem) return false
        return isSword(mainHandStack) || alwaysHideShield
    }

    private fun shouldApplySwordBlock(entity: LivingEntity): Boolean {
        val player = mc.player
        if (entity === player && mc.options.keyUse.isDown()
            && (fakeOnPressing || (noShield && player.offhandItem.item !is ShieldItem))
        ) return true

        if (!entity.isUsingItem) return false
        val useItem = entity.useItem
        return (entity.usedItemHand == InteractionHand.OFF_HAND && useItem.item is ShieldItem) ||
            entity.usedItemHand == InteractionHand.MAIN_HAND
    }

    private fun isSword(stack: ItemStack): Boolean = stack.typeHolder().`is`(ItemTags.SWORDS)

    init {
        onSend<ServerboundUseItemPacket> { event ->
            if (onlyVisual) return@onSend

            val player = mc.player ?: return@onSend
            if (hand != InteractionHand.MAIN_HAND || !isSword(player.getItemInHand(hand))) return@onSend

            val offHandItem = player.offhandItem
            if (offHandItem.item is ShieldItem) {
                event.cancel()
                player.connection.send(
                    ServerboundUseItemPacket(
                        InteractionHand.OFF_HAND,
                        sequence,
                        player.yRot,
                        player.xRot,
                    )
                )
                return@onSend
            }

            // NoShield is intentionally visual-only when there is no shield to use.
            if (noShield) return@onSend

            // Match vanilla's prediction sequence after the original sword-use packet is sent.
            val level = mc.level ?: return@onSend
            schedule(1) {
                if (!enabled || mc.player !== player || mc.level !== level) return@schedule
                val gameMode = mc.gameMode as? MultiPlayerGameModeAccessor ?: return@schedule
                gameMode.invokeStartPrediction(level) { nextSequence ->
                    ServerboundUseItemPacket(
                        InteractionHand.OFF_HAND,
                        nextSequence,
                        player.yRot,
                        player.xRot,
                    )
                }
            }
        }
    }
}
