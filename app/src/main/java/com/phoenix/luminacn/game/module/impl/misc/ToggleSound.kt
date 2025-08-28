package com.phoenix.luminacn.game.module.impl.misc

import com.phoenix.luminacn.constructors.ArrayListManager
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.module.api.setting.stringValue
import com.phoenix.luminacn.util.AssetManager

class ToggleSound : Element(
    name = "ToggleSound",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_togglesound"),
    iconResId = AssetManager.getAsset("ic_music")
) {
    private var selectedSound by stringValue("音效选择", "protohax", listOf("天体", "Nursultan", "舒适", "protohax", "syneo", "sybrse"))

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            session.toggleSounds(false)
            return
        }

        session.toggleSounds(true)

        when (selectedSound) {
            "天体" -> session.soundList(ArrayListManager.SoundSet.CELESTIAL)
            "Nursultan" -> session.soundList(ArrayListManager.SoundSet.ALTERNATE)
            "舒适" -> session.soundList(ArrayListManager.SoundSet.SPECIAL)
            "protohax" -> session.soundList(ArrayListManager.SoundSet.PROTOHAX)
            "syneo" -> session.soundList(ArrayListManager.SoundSet.SYNEO)
            "sybrse" -> session.soundList(ArrayListManager.SoundSet.SYBRSE)
        }
    }
}