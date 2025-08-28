package com.phoenix.luminacn.game.event

import com.phoenix.luminacn.constructors.NetBound
import com.phoenix.luminacn.constructors.Element

class EventModuleToggle(session: NetBound, val module: Element, val targetState: Boolean) : GameEventCancellable(session, "module_toggle")
