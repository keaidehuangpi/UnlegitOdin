package com.odtheking.odin.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ListSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.modMessage
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundSoundPacket

object SoundBlocker : Module(
    name = "Sound blocker",
    description = "Blocks sound"
) {
    val blockedSounds by ListSetting("Blocked Sounds", hashSetOf<SoundProperty>())
    val blockedSoundNames by ListSetting("Blocked Sound Names", hashSetOf<String>())
    private val mode by SelectorSetting("Block Mode", "AlsoPitch", arrayListOf("OnlyName", "AlsoPitch"), desc = "choose the mode")
    private val editSoundBlocker by BooleanSetting("Edit Sound blocker", false, desc = "Enable it and check your chat")

    init {
        on<PacketEvent.Receive> {
            val p = packet as? ClientboundSoundPacket ?: return@on
            val receivedSound = SoundProperty(p.sound.registeredName, p.pitch)
            var shouldGray = false
            when (mode) {
                0 -> if (blockedSoundNames.contains(receivedSound.name)) {
                    cancel()
                    shouldGray = true
                }
                1 -> if (blockedSounds.contains(receivedSound)) {
                    shouldGray = true
                    cancel()
                }
            }
            if (editSoundBlocker) {
                modMessage(Component.literal("${if (shouldGray) "搂7" else "搂6"} Sound Received: registeredName ${receivedSound.name} pitch ${receivedSound.pitch} ").withStyle {
                    it.withClickEvent(ClickEvent.RunCommand("blockedsounds ${receivedSound.name} ${receivedSound.pitch}"))
                        .withHoverEvent(HoverEvent.ShowText(Component.literal(if (shouldGray) "搂7Click to remove this sound into your block list." else "搂6Click to add this sound into your block list.")))
                })
            }
        }
    }

    fun addOrRemove(soundProperty: SoundProperty): String {
        return if (mode == 0) {
            if (blockedSoundNames.remove(soundProperty.name)) "SUCCESSFULLY REMOVED!"
            else { blockedSoundNames.add(soundProperty.name); "SUCCESSFULLY ADDED!" }
        } else {
            if (blockedSounds.remove(soundProperty)) "SUCCESSFULLY REMOVED!"
            else { blockedSounds.add(soundProperty); "SUCCESSFULLY ADDED!" }
        }
    }

    @Serializable
    data class SoundProperty(val name: String, val pitch: Float)
}
