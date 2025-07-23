package com.project.luminacn.game.event

import com.project.luminacn.constructors.NetBound
import com.project.luminacn.game.inventory.AbstractInventory

class EventInventorySlotUpdate(
    session: NetBound,
    val inventory: AbstractInventory,
    val slot: Int
) : GameEvent(session, "InventorySlotUpdate")
