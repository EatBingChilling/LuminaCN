package com.phoenix.luminacn.remlink

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phoenix.luminacn.constructors.BoolValue
import com.phoenix.luminacn.constructors.ComposedPacketHandler
import com.phoenix.luminacn.constructors.FloatValue
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.IntValue
import com.phoenix.luminacn.constructors.NetBound
import com.project.lumina.relay.LuminaRelaySession
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import com.phoenix.luminacn.game.InterceptablePacket

open class RemSession(val moduleManager: GameManager) : ComposedPacketHandler {

    private val prefix = "!"
    private val feedback = true


    private val headerColor = "§6"
    private val accentColor = "§e"
    private val successColor = "§a"
    private val errorColor = "§c"
    private val infoColor = "§7"
    private var isIntercepted = false

    open lateinit var session: NetBound
    private val isSessionCreated: Boolean
        get() = ::session.isInitialized

    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        if (packet !is TextPacket) {
            return false
        }

        if (!isSessionCreated || !session.isProxyPlayer(packet.sourceName)) {
            return false
        }

        val message = packet.message.trim()
        if (!message.startsWith(prefix)) {
            return false
        }

        isIntercepted = true

        val args = message.substring(prefix.length).split(" ")
        if (args.isEmpty()) {
            return false
        }

        val command = args[0].lowercase()

        if (isSessionCreated && feedback) {
            try {
                when (command) {
                    "toggle" -> {
                        if (args.size > 1) {
                            handleToggleCommand(args[1])
                        } else {
                            sendClientMessage("${errorColor}⚠ Usage: ${accentColor}!toggle <module>")
                        }
                    }

                    "ping" -> {
                        sendClientMessage("${successColor}✦ Pong! ${infoColor}Response from CmdListener")
                    }

                    "help" -> {
                        handleHelpCommand()
                    }

                    "set" -> {
                        if (args.size >= 4) {
                            handleSetCommand(args[1], args[2], args[3])
                        } else {
                            sendClientMessage("${errorColor}⚠ Usage: ${accentColor}!set <module> <setting> <value>")
                        }
                    }

                    else -> {
                        sendClientMessage("${errorColor}✗ Unknown command: ${accentColor}$command${infoColor} - Try ${accentColor}!help")
                    }
                }
            } catch (e: Exception) {
                Log.e("RemSession", "Error processing command: ${e.message}")
            }
        }
        return false
    }

    private fun sendClientMessage(message: String) {
        if (isSessionCreated) {
            try {
                session.displayClientMessage(message, TextPacket.Type.RAW)
            } catch (e: Exception) {
                Log.e("RemSession", "Failed to send client message: ${e.message}")
            }
        }
    }

    private fun handleToggleCommand(moduleName: String) {
        val module = moduleManager.getModule(moduleName)
        if (module != null) {
            module.isEnabled = !module.isEnabled
            val state = if (module.isEnabled) "enabled" else "disabled"
            val stateColor = if (module.isEnabled) successColor else errorColor
            sendClientMessage("${headerColor}✪ ${accentColor}${module.name} ${infoColor}has been ${stateColor}$state${infoColor}!")
        } else {
            sendClientMessage("${errorColor}✗ Artifact '${accentColor}$moduleName${errorColor}' not found.")
        }
    }

    private fun handleHelpCommand() {
        val modules = moduleManager.elements.filter { !it.private }
        if (modules.isEmpty()) {
            sendClientMessage("${errorColor}✗ No artifacts available.")
            return
        }

        sendClientMessage("${headerColor}✪ CmdListener Help ${infoColor}▼")
        sendClientMessage("${accentColor}Modules:")
        modules.forEach { module ->
            val status = if (module.isEnabled) "${successColor}ON" else "${errorColor}OFF"
            sendClientMessage("${infoColor}├─ ${accentColor}${module.name} ${infoColor}[$status]")
            module.values.filter { it is BoolValue || it is FloatValue || it is IntValue }
                .forEach { value ->
                    val currentValue = when (value) {
                        is BoolValue -> if (value.value) "${successColor}true" else "${errorColor}false"
                        is FloatValue -> "${value.value}"
                        is IntValue -> "${value.value}"
                        else -> "N/A"
                    }
                    sendClientMessage("${infoColor}│  ${value.name}: $currentValue")
                }
        }
        sendClientMessage("${accentColor}Commands:")
        sendClientMessage("${infoColor}├─ ${accentColor}!toggle <module> ${infoColor}- Toggle a module")
        sendClientMessage("${infoColor}├─ ${accentColor}!set <module> <setting> <value> ${infoColor}- Set a setting")
        sendClientMessage("${infoColor}├─ ${accentColor}!ping ${infoColor}- Test response")
        sendClientMessage("${infoColor}└─ ${accentColor}!help ${infoColor}- Show this menu")
    }

    private fun handleSetCommand(moduleName: String, settingName: String, valueString: String) {
        val module = moduleManager.getModule(moduleName)
        if (module == null) {
            sendClientMessage("${errorColor}✗ Artifact '${accentColor}$moduleName${errorColor}' not found.")
            return
        }

        val value = module.values.find { it.name.equals(settingName, ignoreCase = true) }
        if (value == null) {
            sendClientMessage("${errorColor}✗ Setting '${accentColor}$settingName${errorColor}' not found in ${accentColor}$moduleName${errorColor}.")
            return
        }

        try {
            when (value) {
                is BoolValue -> {
                    value.value = valueString.toBooleanStrict()
                }

                is FloatValue -> {
                    val newValue = valueString.toFloat()
                    if (newValue in value.range) value.value =
                        newValue else throw IllegalArgumentException("Value out of range ${value.range}")
                }

                is IntValue -> {
                    val newValue = valueString.toInt()
                    if (newValue in value.range) value.value =
                        newValue else throw IllegalArgumentException("Value out of range ${value.range}")
                }

                else -> {
                    sendClientMessage("${errorColor}✗ Setting '${accentColor}$settingName${errorColor}' is not a basic type (bool, float, int).")
                    return
                }
            }
            sendClientMessage("${headerColor}✪ ${accentColor}${module.name}.${settingName} ${infoColor}set to $valueString${infoColor}!")
        } catch (e: Exception) {
            sendClientMessage("${errorColor}⚠ Invalid value '${accentColor}$valueString${errorColor}' for ${accentColor}$settingName${errorColor}: ${e.message}")
            Log.e("CmdListener", "Failed to set $settingName: ${e.message}")
        }
    }


}