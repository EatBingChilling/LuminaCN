package com.project.lumina.relay.handler

import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler

class SessionCloseHandler(private val onSessionClose: (String) -> Unit) : BedrockPacketHandler {

    override fun onDisconnect(reason: String) {
        onSessionClose(reason)
    }

}