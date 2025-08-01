package com.project.luminacn.game.module.impl.misc

import com.project.luminacn.constructors.ArrayListManager
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.game.InterceptablePacket
import com.project.luminacn.constructors.BoolValue
import com.project.luminacn.util.AssetManager

class ToggleSound : Element(
    name = "ToggleSound",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("module_togglesound"),
    iconResId = AssetManager.getAsset("ic_music")
) {
    private var celestial by boolValue("天体", true)
    private var nursultan by boolValue("Nursultan", false)
    private var smooth by boolValue("舒适", false)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            session.toggleSounds(false)
            return
        }

        session.toggleSounds(true)

        when {
            celestial -> {
                disableOthers(except = 1)
                session.soundList(ArrayListManager.SoundSet.CELESTIAL)
            }

            nursultan -> {
                disableOthers(except = 2)
                session.soundList(ArrayListManager.SoundSet.ALTERNATE)
            }

            smooth -> {
                disableOthers(except = 3)
                session.soundList(ArrayListManager.SoundSet.SPECIAL)
            }
        }
    }

    private fun disableOthers(except: Int) {
        when (except) {
            1 -> {
                nursultan = false
                smooth = false
            }

            2 -> {
                celestial = false
                smooth = false
            }

            3 -> {
                celestial = false
                nursultan = false
            }
        }
    }
}