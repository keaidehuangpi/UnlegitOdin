package com.odtheking.odin.features.impl.render

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.PacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.render.drawLine
import net.minecraft.network.protocol.game.ClientboundSoundPacket

@AlwaysActive
object HitEffect : Module(
    name = "Hit Effect",
    toggled = true,
    description = "Shows a mark on your crosshair when hit"
) {
    private var stampHit = 0L
    private val size by NumberSetting("Size", 15, 0, 30, 1, desc = "Size.")
    private val width by NumberSetting("Width", 5, 0, 15, 1, desc = "Width.")
    private val hitMarkTime by NumberSetting("Hitmarktime", 300, 0, 3000, 1, desc = "Hitmarktime")
    private val hud by HUD(name, "Shows a mark on your crosshair when hit", toggleable = false, x = 0, y = 0, scale = 1f) {
        if (System.currentTimeMillis() - stampHit < hitMarkTime) {
            val color = Color(255, 0, 0)
            val gap = 2f
            val centerX = mc.window.guiScaledWidth / 2f
            val centerY = mc.window.guiScaledHeight / 2f
            drawLine(centerX + gap, centerY + gap, centerX + gap + size, centerY + gap + size, color, width.toFloat())
            drawLine(centerX - gap, centerY - gap, centerX - gap - size, centerY - gap - size, color, width.toFloat())
            drawLine(centerX - gap, centerY + gap, centerX - gap - size, centerY + gap + size, color, width.toFloat())
            drawLine(centerX + gap, centerY - gap, centerX + gap + size, centerY - gap - size, color, width.toFloat())
        }
        0 to 0
    }
    init {
        on<PacketEvent.Receive> {
            val p = packet as? ClientboundSoundPacket ?: return@on
            if (p.pitch == 0.7936508F && p.sound.registeredName == "minecraft:entity.arrow.hit_player") {
                stampHit = System.currentTimeMillis()
            }
        }
    }
}
