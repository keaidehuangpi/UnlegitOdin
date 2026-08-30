package com.odtheking.odin.features.impl.render

import com.odtheking.odin.clickgui.settings.AlwaysActive
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.minecraft.resources.Identifier

@AlwaysActive
object AntiDebuff : Module(
    name = "Anti Debuff",
    toggled = true,
    description = "Block Debuff you dont want"
) {
    // Individual toggles remain configurable while the module is always subscribed for mixin checks.
    val blinding by BooleanSetting("blinding", true, desc = "Disables blinding effect.")
    val darkness by BooleanSetting("darkness", true, desc = "Disables darkness effect.")
    val nausea by BooleanSetting("nausea", true, desc = "Disables nausea effect.")
    val pumpkinBlur by BooleanSetting("pumpkinBlur", true, desc = "Disables pumpkin blur effect.")
    @JvmField
    val TEXTURE_PUMPKIN_BLUR: Identifier = Identifier.withDefaultNamespace("textures/misc/pumpkinblur.png")
}
