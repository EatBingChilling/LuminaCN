package com.phoenix.luminacn.game.event

import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.game.inventory.AbstractInventory

class EventInventorySlotUpdate(
    session: NetBound,
    val inventory: AbstractInventory,
    val slot: Int
) : GameEvent(session, "InventorySlotUpdate")
