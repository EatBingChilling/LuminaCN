package com.phoenix.luminacn.game.module.impl.misc

import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.util.AssetManager

class ArrayListElement(iconResId: Int = AssetManager.getAsset("ic_alien")) : Element(
    name = "ArrayList",
    category = CheatCategory.Misc,
    displayNameResId = AssetManager.getString("arraylist"),
    iconResId = iconResId
) {

    private var praxStyle by boolValue("无用", true)


    private var solsticeStyle = false
    private var splitStyle = false
    private var outlineStyle = false

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            session.enableArrayList(false)
            return
        }

        session.enableArrayList(true)



        if (praxStyle) {
            solsticeStyle = false
            splitStyle = false
            outlineStyle = false
            session.arrayListUi("无")
        } else if (solsticeStyle) {
            praxStyle = false
            splitStyle = false
            outlineStyle = false
            session.arrayListUi("底栏")
        } else if (splitStyle) {
            praxStyle = false
            solsticeStyle = false
            outlineStyle = false
            session.arrayListUi("拆分")
        } else if (outlineStyle) {
            praxStyle = false
            solsticeStyle = false
            splitStyle = false
            session.arrayListUi("描线")
        }
    }
}
