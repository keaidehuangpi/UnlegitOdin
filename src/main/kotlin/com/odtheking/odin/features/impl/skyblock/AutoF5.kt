package com.odtheking.odin.features.impl.skyblock

import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import net.minecraft.client.CameraType
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

object AutoF5 : Module(
    name = "AutoF5",
    description = "Switches to third person while a container is open"
) {
    init {
        onReceive<ClientboundOpenScreenPacket> {
            // Intentionally no chat notification.
            mc.options.cameraType = CameraType.THIRD_PERSON_BACK
        }
        onReceive<ClientboundContainerClosePacket> {
            mc.options.cameraType = CameraType.FIRST_PERSON
        }
        onSend<ServerboundContainerClosePacket> {
            // Intentionally no chat notification.
            mc.options.cameraType = CameraType.FIRST_PERSON
        }
    }
}
