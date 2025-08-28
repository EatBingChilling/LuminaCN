package com.phoenix.luminacn.game.module.api.commands

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.phoenix.luminacn.R
import com.phoenix.luminacn.constructors.BoolValue
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.FloatValue
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.IntValue
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.remlink.TerminalViewModel
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket

class CmdListener(gameManager: GameManager) : Element(
    name = "ChatListener",
    category = CheatCategory.Misc,
    displayNameResId = R.string.module_chat_listener
) {
    init {
        this.moduleManager = gameManager
    }
    override val state: Boolean
        get() = isEnabled

    companion object {
        const val PREFIX = "!"
        const val FEEDBACK_ENABLED = true
        const val HEADER_COLOR = "§6"
        const val ACCENT_COLOR = "§e"
        const val SUCCESS_COLOR = "§a"
        const val ERROR_COLOR = "§c"
        const val INFO_COLOR = "§7"
        const val VALUE_COLOR = "§b"

        var isModuleEnabled by mutableStateOf(true)
    }


    private var isInGame by mutableStateOf(false)

    
    fun interceptOutboundPacket(interceptablePacket: InterceptablePacket) {
        if (!isModuleEnabled) return

        
        if (interceptablePacket.packet is TextPacket) {
            val packet = interceptablePacket.packet as TextPacket
            val message = packet.message.trim()

            
            if (message.startsWith(PREFIX)) {
                
                interceptablePacket.isIntercepted = true

                
                processCommand(message)

                
                TerminalViewModel.addTerminalLog("GameSession", "Command intercepted and not sent to server: $message")
            }
        }
    }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isModuleEnabled) return

        when (interceptablePacket.packet) {
            is PlayerAuthInputPacket -> isInGame = true
            is TextPacket -> {
                val packet = interceptablePacket.packet as TextPacket

                
                val message = packet.message.trim()
                if (message.startsWith(PREFIX) && session.isProxyPlayer(packet.sourceName)) {
                    
                    interceptablePacket.isIntercepted = true

                    
                    processCommand(message)

                    
                    TerminalViewModel.addTerminalLog("GameSession", "Command intercepted: $message")
                }
            }
        }
    }

    private fun processCommand(message: String) {
        if (!message.startsWith(PREFIX)) return

        TerminalViewModel.addTerminalLog("GameSession", "Processing command: $message")

        val args = message.substring(PREFIX.length).split(" ").filter { it.isNotBlank() }
        if (args.isEmpty()) {
            sendClientMessage("${ERROR_COLOR}空指令. 使用 $ACCENT_COLOR!help")
            return
        }

        if (!isSessionCreated || !FEEDBACK_ENABLED) return

        when (val command = args[0].lowercase()) {
            "toggle" -> handleToggle(args.getOrNull(1))
            "ping" -> handlePing()
            "help" -> handleHelp(args.getOrNull(1))
            "set" -> handleSet(args.getOrNull(1), args.getOrNull(2), args.getOrNull(3))
            "list" -> handleList()
            "reset" -> handleReset(args.getOrNull(1))
            "info" -> handleInfo(args.getOrNull(1))
            "module" -> handleModuleToggle(args.getOrNull(1))
            else -> sendClientMessage("${ERROR_COLOR}未知的指令: $ACCENT_COLOR$command$INFO_COLOR - 尝试 $ACCENT_COLOR!help")
        }
    }

    private fun sendClientMessage(message: String) {
        session.displayClientMessage(message, TextPacket.Type.RAW)
        TerminalViewModel.addTerminalLog("GameSession", "Feedback sent: $message")
    }

    private fun formatModuleName(rawName: String): String {
        return rawName.replace("_", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun parseModuleName(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
    }

    private fun handleModuleToggle(state: String?) {
        if (state == null) {
            sendClientMessage("${ERROR_COLOR}使用方法: $ACCENT_COLOR!module <on/off>")
            return
        }
        when (state.lowercase()) {
            "on" -> {
                isModuleEnabled = true
                sendClientMessage("${SUCCESS_COLOR}聊天监听器开启")
            }
            "off" -> {
                isModuleEnabled = false
                sendClientMessage("${SUCCESS_COLOR}聊天监听器关闭")
            }
            else -> sendClientMessage("${ERROR_COLOR}非法的输入. 使用 'on' 或 'off'")
        }
        TerminalViewModel.addTerminalLog("GameSession", "Module enabled: $isModuleEnabled")
    }

    private fun handleToggle(moduleName: String?) {
        if (moduleName == null) {
            sendClientMessage("${ERROR_COLOR}使用方法: $ACCENT_COLOR!toggle <模块名称>")
            return
        }
        val parsedName = parseModuleName(moduleName)
        val module = moduleManager.getModule(parsedName)
            ?: return sendClientMessage("${ERROR_COLOR}模块 '$ACCENT_COLOR$moduleName$ERROR_COLOR' 未找到.")

        module.isEnabled = !module.isEnabled
        val state = if (module.isEnabled) "on" else "off"
        val stateColor = if (module.isEnabled) SUCCESS_COLOR else ERROR_COLOR
        sendClientMessage("$HEADER_COLOR${formatModuleName(module.name)} $INFO_COLOR${stateColor}$state$INFO_COLOR.")
        TerminalViewModel.addTerminalLog("GameSession", "Toggled module ${module.name} to $state")
    }

    private fun handlePing() {
        sendClientMessage("${SUCCESS_COLOR}砰!")
        TerminalViewModel.addTerminalLog("GameSession", "Executed ping command")
    }

    private fun handleHelp(moduleName: String?) {
        if (moduleName == null) {
            displayGeneralHelp()
        } else {
            displayModuleHelp(moduleName)
        }
    }

    private fun displayGeneralHelp() {
        val modules = moduleManager.elements.filter { !it.private }.sortedBy { formatModuleName(it.name) }
        if (modules.isEmpty()) {
            sendClientMessage("${ERROR_COLOR}无可用的模块.")
            return
        }

        sendClientMessage("${HEADER_COLOR}指令帮助 ${INFO_COLOR}▼")
        sendClientMessage("${ACCENT_COLOR}可用的指令:")
        listOf(
            "!module <on/off> - 打开或关闭对应的模块",
            "!toggle <模块名称> -  切换模块开关",
            "!set <模块名称> <选项> <参数> - 设置模块参数",
            "!help [模块名称] - 显示全部帮助或显示特定功能的帮助",
            "!list - 列出所有模块",
            "!reset <模块名称> - 重置模块设置",
            "!info <模块名称> - 显示模块信息",
            "!ping - 测试是否连接成功"
        ).forEach { sendClientMessage("$INFO_COLOR- $ACCENT_COLOR$it") }

        sendClientMessage("${ACCENT_COLOR}模块 (使用 !help <module> 来查看详情):")
        modules.forEach { module ->
            val status = if (module.isEnabled) "${SUCCESS_COLOR}开启" else "${ERROR_COLOR}关闭"
            sendClientMessage("$INFO_COLOR- $ACCENT_COLOR${formatModuleName(module.name)} $INFO_COLOR[$status]")
        }
        TerminalViewModel.addTerminalLog("GameSession", "Displayed general help")
    }

    private fun displayModuleHelp(moduleName: String) {
        val parsedName = parseModuleName(moduleName)
        val module = moduleManager.getModule(parsedName)
            ?: return sendClientMessage("${ERROR_COLOR}Module '$ACCENT_COLOR$moduleName$ERROR_COLOR' 未找到.")

        sendClientMessage("$HEADER_COLOR${formatModuleName(module.name)} 帮助 ${INFO_COLOR}▼")
        sendClientMessage("${ACCENT_COLOR}状态: ${if (module.isEnabled) "${SUCCESS_COLOR}Enabled" else "${ERROR_COLOR}Disabled"}")
        val settings = module.values.filter { it is BoolValue || it is FloatValue || it is IntValue }
        if (settings.isNotEmpty()) {
            sendClientMessage("${ACCENT_COLOR}设置:")
            settings.forEach { value ->
                val currentValue = when (value) {
                    is BoolValue -> if (value.value) "${SUCCESS_COLOR}true" else "${ERROR_COLOR}false"
                    is FloatValue -> "$VALUE_COLOR${value.value}$INFO_COLOR (Range: ${value.range})"
                    is IntValue -> "$VALUE_COLOR${value.value}$INFO_COLOR (Range: ${value.range})"
                    else -> "N/A"
                }
                sendClientMessage("$INFO_COLOR- $ACCENT_COLOR${value.name}: $currentValue")
            }
        } else {
            sendClientMessage("${INFO_COLOR}无已配置的信息.")
        }
        TerminalViewModel.addTerminalLog("GameSession", "Displayed help for module $moduleName")
    }

    private fun handleSet(moduleName: String?, settingName: String?, valueString: String?) {
        if (moduleName == null || settingName == null || valueString == null) {
            sendClientMessage("${ERROR_COLOR}用法: $ACCENT_COLOR!set <模块名称> <设定> <参数>")
            return
        }

        val parsedName = parseModuleName(moduleName)
        val module = moduleManager.getModule(parsedName)
            ?: return sendClientMessage("${ERROR_COLOR}模块 '$ACCENT_COLOR$moduleName$ERROR_COLOR' 未找到.")

        val value = module.values.find { it.name.equals(settingName, ignoreCase = true) }
            ?: return sendClientMessage("${ERROR_COLOR}设定 '$ACCENT_COLOR$settingName$ERROR_COLOR' 未找到.")

        try {
            when (value) {
                is BoolValue -> value.value = valueString.toBooleanStrict()
                is FloatValue -> {
                    val newValue = valueString.toFloat()
                    if (newValue in value.range) value.value = newValue
                    else throw IllegalArgumentException("非法的数值 ${value.range}")
                }
                is IntValue -> {
                    val newValue = valueString.toInt()
                    if (newValue in value.range) value.value = newValue
                    else throw IllegalArgumentException("非法的数值 ${value.range}")
                }
                else -> throw IllegalArgumentException("不支持的格式")
            }
            sendClientMessage("$HEADER_COLOR${formatModuleName(module.name)}.${settingName} ${INFO_COLOR}设定为 $VALUE_COLOR$valueString$INFO_COLOR.")
            TerminalViewModel.addTerminalLog("GameSession", "Set ${module.name}.${settingName} to $valueString")
        } catch (e: Exception) {
            sendClientMessage("${ERROR_COLOR}Invalid '$ACCENT_COLOR$valueString$ERROR_COLOR' for $ACCENT_COLOR$settingName$ERROR_COLOR: ${e.message}")
            Log.e("CmdListener", "Failed to set $settingName: ${e.message}")
            TerminalViewModel.addTerminalLog("GameSession", "Failed to set ${module.name}.${settingName}: ${e.message}")
        }
    }

    private fun handleList() {
        val modules = moduleManager.elements.filter { !it.private }.sortedBy { formatModuleName(it.name) }
        if (modules.isEmpty()) {
            sendClientMessage("${ERROR_COLOR}无可用的模块.")
            return
        }

        sendClientMessage("${HEADER_COLOR}模块列表 ${INFO_COLOR}▼")
        modules.forEach { module ->
            val status = if (module.isEnabled) "${SUCCESS_COLOR}ON" else "${ERROR_COLOR}OFF"
            sendClientMessage("$INFO_COLOR- $ACCENT_COLOR${formatModuleName(module.name)} $INFO_COLOR[$status]")
        }
        sendClientMessage("${INFO_COLOR}共l: ${modules.size} 个模块")
        TerminalViewModel.addTerminalLog("GameSession", "Listed all modules")
    }

    private fun handleReset(moduleName: String?) {
        if (moduleName == null) {
            sendClientMessage("${ERROR_COLOR}用法: $ACCENT_COLOR!reset <模块名称>")
            return
        }

        val parsedName = parseModuleName(moduleName)
        val module = moduleManager.getModule(parsedName)
            ?: return sendClientMessage("${ERROR_COLOR}模块 '$ACCENT_COLOR$moduleName$ERROR_COLOR' 未找到.")

        module.values.forEach { value ->
            when (value) {
                is BoolValue -> value.value = false
                is FloatValue -> value.value = value.range.start
                is IntValue -> value.value = value.range.first
                else  -> return
            }
        }
        sendClientMessage("$HEADER_COLOR${formatModuleName(module.name)} ${INFO_COLOR}的设定已恢复.")
        TerminalViewModel.addTerminalLog("GameSession", "Reset settings for module $moduleName")
    }

    private fun handleInfo(moduleName: String?) {
        if (moduleName == null) {
            sendClientMessage("${ERROR_COLOR}用法: $ACCENT_COLOR!info <模块名称>")
            return
        }

        val parsedName = parseModuleName(moduleName)
        val module = moduleManager.getModule(parsedName)
            ?: return sendClientMessage("${ERROR_COLOR}模块 '$ACCENT_COLOR$moduleName$ERROR_COLOR' 未找到.")

        sendClientMessage("$HEADER_COLOR${formatModuleName(module.name)} 信息 ${INFO_COLOR}▼")
        sendClientMessage("${ACCENT_COLOR}Status: ${if (module.isEnabled) "${SUCCESS_COLOR}Enabled" else "${ERROR_COLOR}Disabled"}")
        sendClientMessage("${ACCENT_COLOR}Category: $INFO_COLOR${module.category}")
        sendClientMessage("${ACCENT_COLOR}Private: $INFO_COLOR${if (module.private) "Yes" else "No"}")
        TerminalViewModel.addTerminalLog("GameSession", "Displayed info for module $moduleName")
    }
}