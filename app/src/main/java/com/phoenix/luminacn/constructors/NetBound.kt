package com.phoenix.luminacn.constructors

import android.util.Log
import com.phoenix.luminacn.application.AppContext
import com.phoenix.luminacn.game.InterceptablePacket
import com.phoenix.luminacn.game.entity.LocalPlayer
import com.phoenix.luminacn.game.event.EventManager
import com.phoenix.luminacn.game.event.EventPacketInbound
import com.phoenix.luminacn.game.registry.BlockMapping
import com.phoenix.luminacn.game.registry.BlockMappingProvider
import com.phoenix.luminacn.game.world.Level
import com.phoenix.luminacn.game.world.World
import com.phoenix.luminacn.overlay.mods.MiniMapOverlay
import com.phoenix.luminacn.overlay.mods.OverlayModuleList
import com.phoenix.luminacn.overlay.mods.PacketNotificationOverlay
import com.phoenix.luminacn.overlay.mods.Position
import com.phoenix.luminacn.overlay.mods.SessionStatsOverlay
import com.phoenix.luminacn.overlay.mods.SpeedometerOverlay
import com.project.lumina.relay.LuminaRelaySession
import com.phoenix.luminacn.game.registry.ItemMapping
import com.phoenix.luminacn.game.registry.ItemMappingProvider
import com.phoenix.luminacn.game.registry.LegacyBlockMapping
import com.phoenix.luminacn.game.registry.LegacyBlockMappingProvider
import com.phoenix.luminacn.overlay.mods.KeystrokesOverlay
import com.phoenix.luminacn.overlay.mods.TargetHudOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket
import org.cloudburstmc.protocol.bedrock.packet.TextPacket
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket
import org.cloudburstmc.protocol.bedrock.packet.SetTimePacket
import java.util.Collections
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate")
class NetBound(val luminaRelaySession: LuminaRelaySession) : ComposedPacketHandler, com.phoenix.luminacn.game.event.Listenable {

    override val eventManager = EventManager()

    val world = World(this)
    val level = Level(this)
    val localPlayer = LocalPlayer(this)

    private val proxyPlayerNames: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    
    val gameDataManager = GameDataManager()

    val protocolVersion: Int
        get() = luminaRelaySession.server.codec.protocolVersion


    private var welcomeTipShown = false

    private val mappingProviderContext = AppContext.instance

    private val blockMappingProvider = BlockMappingProvider(mappingProviderContext)
    private val itemMappingProvider = ItemMappingProvider(mappingProviderContext)
    private val legacyBlockMappingProvider = LegacyBlockMappingProvider(mappingProviderContext)

    lateinit var blockMapping: BlockMapping
    lateinit var itemMapping: ItemMapping
    lateinit var legacyBlockMapping: LegacyBlockMapping

    private var startGameReceived = false
    private val pendingPackets = mutableListOf<BedrockPacket>()

    private val mainScope = CoroutineScope(Dispatchers.Main)

    private var playerPosition = Position(0f, 0f)
    private var playerRotation = 0f
    private val entityPositions = mutableMapOf<Long, Position>()
    private var minimapEnabled = false
    private var minimapUpdateScheduled = false
    private var minimapSize = 100f
    private var minimapZoom = 1.0f
    private var minimapDotSize = 5
    private var tracersEnabled = false

    private val versionName by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppContext.instance.packageManager.getPackageInfo(
            AppContext.instance.packageName, 0
        ).versionName
    }

    fun clientBound(packet: BedrockPacket) {
        luminaRelaySession.clientBound(packet)
    }

    fun serverBound(packet: BedrockPacket) {
        luminaRelaySession.serverBound(packet)
    }




    override fun beforePacketBound(packet: BedrockPacket): Boolean {
        GameManager.setNetBound(this)
        if (packet is TextPacket && packet.type == TextPacket.Type.CHAT) {
            proxyPlayerNames.add(packet.sourceName)
        }

        when (packet) {
            is StartGamePacket -> {
                gameDataManager.storeStartGamePacket(packet)

                if (!startGameReceived) {
                    startGameReceived = true
                    Log.e("StartGamePacket", packet.toString())
                    Log.i("GameSession", "Seed: ${gameDataManager.getSeed()}")
                    Log.i("GameSession", "Game Mode: ${gameDataManager.getGameMode()}")
                    Log.i("GameSession", "LevelName: ${gameDataManager.getLevelName()}")

                    /**
                    try {
                    blockMapping = blockMappingProvider.craftMapping(protocolVersion)
                    itemMapping = itemMappingProvider.craftMapping(protocolVersion)
                    legacyBlockMapping = legacyBlockMappingProvider.craftMapping(protocolVersion)

                    Log.i("GameSession", "Loaded mappings for protocol $protocolVersion")
                    } catch (e: Exception) {
                    Log.e("GameSession", "Failed to load mappings for protocol $protocolVersion", e)
                    }
                     */
                }
            }

            is PlayerListPacket -> {
                gameDataManager.handlePlayerListPacket(packet)
            }





        }

        localPlayer.onPacketBound(packet)
        world.onPacket(packet)
        level.onPacketBound(packet)

        val event = EventPacketInbound(this, packet)
        eventManager.emit(event)

        if (event.isCanceled()) return true

        val interceptablePacket = InterceptablePacket(packet)
        for (module in GameManager.elements) {
            module.beforePacketBound(interceptablePacket)
            if (interceptablePacket.isIntercepted) return true
        }

        if (!welcomeTipShown && packet is StartGamePacket) {
    welcomeTipShown = true
    displayClientMessage("§lLuminaCN", TextPacket.Type.TIP)
}

        return false
    }

    override fun afterPacketBound(packet: BedrockPacket) {
        for (module in GameManager.elements) {
            module.afterPacketBound(packet)
        }
    }

    override fun onDisconnect(reason: String) {
        localPlayer.onDisconnect()
        level.onDisconnect()
        proxyPlayerNames.clear()
        GameManager.clearNetBound()
        gameDataManager.clearAllData()
        startGameReceived = false

        for (module in GameManager.elements) {
            module.onDisconnect(reason)
        }
    }

    fun getStartGameData(): Map<String, Any?> = gameDataManager.getStartGameData()
    fun getStartGameField(fieldName: String): Any? = gameDataManager.getStartGameField(fieldName)
    fun hasStartGameData(): Boolean = gameDataManager.hasStartGameData()
    fun getWorldName(): String? = gameDataManager.getWorldName()
    fun getPlayerSpawnPosition(): Vector3f? = gameDataManager.getPlayerPosition()
    fun getWorldSeed(): Long? = gameDataManager.getSeed()
    fun getLevelId(): String? = gameDataManager.getLevelId()
    fun getPacketData(packetType: String): Map<String, Any?> = gameDataManager.getPacketData(packetType)
    fun getPacketField(packetType: String, fieldName: String): Any? = gameDataManager.getPacketField(packetType, fieldName)
    fun getGameDataStats(): String = gameDataManager.getDataStats()
    fun getCurrentPlayers(): List<GameDataManager.PlayerInfo> = gameDataManager.getCurrentPlayerList()
    fun getPlayerByName(name: String): GameDataManager.PlayerInfo? = gameDataManager.getPlayerByName(name)
    fun getPlayerByUUID(uuid: UUID): GameDataManager.PlayerInfo? = gameDataManager.getPlayerByUUID(uuid)
    fun getPlayerByEntityId(entityId: Long): GameDataManager.PlayerInfo? = gameDataManager.getPlayerByEntityId(entityId)
    fun getPlayerCount(): Int = gameDataManager.getPlayerCount()
    fun getPlayerListStats(): String = gameDataManager.getPlayerListStats()
    fun getPlayersWithRole(): Map<String, List<GameDataManager.PlayerInfo>> = gameDataManager.getPlayersWithRole()
    fun isPlayerOnline(playerName: String): Boolean = getPlayerByName(playerName) != null
    fun isPlayerOnlineByUUID(uuid: UUID): Boolean = getPlayerByUUID(uuid) != null
    fun getHosts(): List<GameDataManager.PlayerInfo> = getPlayersWithRole()["hosts"] ?: emptyList()
    fun getTeachers(): List<GameDataManager.PlayerInfo> = getPlayersWithRole()["teachers"] ?: emptyList()
    fun logCurrentPlayerList() {
        Log.i("NetBound", "=== Current Player List ===")
        Log.i("NetBound", getPlayerListStats())
        Log.i("NetBound", "========================")
    }


    fun displayClientMessage(message: String, type: TextPacket.Type = TextPacket.Type.RAW) {
        val textPacket = TextPacket()
        textPacket.type = type
        textPacket.isNeedsTranslation = false
        textPacket.sourceName = ""
        textPacket.message = message
        textPacket.xuid = ""
        textPacket.platformChatId = ""
        textPacket.filteredMessage = ""
        clientBound(textPacket)
    }

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit) {
        mainScope.launch {
            block()
        }
    }

    suspend fun showSessionStatsOverlay(initialStats: List<String>): SessionStatsOverlay =
        withContext(Dispatchers.Main) {
            try {
                val overlay = SessionStatsOverlay.showSessionStats(initialStats)
                overlay
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

    fun showNotification(title: String, subtitle: String, ResId: Int) {
        mainScope.launch {
            try {
                PacketNotificationOverlay.showNotification(
                    title = title,
                    subtitle = subtitle,
                    iconRes = ResId,
                    duration = 1000L
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showSpeedometer(position: Vector3f) {
        mainScope.launch {
            try {
                SpeedometerOverlay.showOverlay()
                SpeedometerOverlay.updatePosition(position)
            } catch (e: Exception) {
            }
        }
    }

    fun updatePlayerPosition(x: Float, z: Float) {
        playerPosition = Position(x, z)
        if (minimapEnabled) {
            scheduleMinimapUpdate()
        }
    }

    fun updatePlayerRotation(yaw: Float) {
        playerRotation = yaw
        if (minimapEnabled) {
            scheduleMinimapUpdate()
        }
    }

    fun updateEntityPosition(entityId: Long, x: Float, z: Float) {
        entityPositions[entityId] = Position(x, z)
        if (minimapEnabled && !minimapUpdateScheduled) {
            scheduleMinimapUpdate()
        }
    }

    fun updateMinimapSize(size: Float) {
        minimapSize = size
        if (minimapEnabled) {
            mainScope.launch {
                MiniMapOverlay.setMinimapSize(size)
                updateMinimap()
            }
        }
    }

    fun updateMinimapZoom(zoom: Float) {
        minimapZoom = zoom
        if (minimapEnabled) {
            mainScope.launch {
                MiniMapOverlay.overlayInstance.minimapZoom = zoom
                updateMinimap()
            }
        }
    }

    fun updateDotSize(dotSize: Int) {
        minimapDotSize = dotSize
        if (minimapEnabled) {
            mainScope.launch {
                MiniMapOverlay.overlayInstance.minimapDotSize = dotSize
                updateMinimap()
            }
        }
    }

    private fun scheduleMinimapUpdate() {
        if (!minimapUpdateScheduled) {
            minimapUpdateScheduled = true
            mainScope.launch {
                updateMinimap()
                minimapUpdateScheduled = false
            }
        }
    }

    fun enableMinimap(enable: Boolean) {
        if (enable != minimapEnabled) {
            minimapEnabled = enable
            if (enable) {
                mainScope.launch {
                    MiniMapOverlay.setOverlayEnabled(true)
                    MiniMapOverlay.setMinimapSize(minimapSize)
                    updateMinimap()
                }
            } else {
                mainScope.launch {
                    MiniMapOverlay.setOverlayEnabled(false)
                }
            }
        }
    }

    private fun updateMinimap() {
        try {
            MiniMapOverlay.setCenter(playerPosition.x, playerPosition.y)
            MiniMapOverlay.setPlayerRotation(playerRotation)

            MiniMapOverlay.overlayInstance.minimapZoom = minimapZoom
            MiniMapOverlay.overlayInstance.minimapDotSize = minimapDotSize

            val targets = entityPositions.values.toList()

            val finalTargets = if (targets.isEmpty()) {
                targets
            } else {
                targets
            }

            MiniMapOverlay.setTargets(finalTargets)
            MiniMapOverlay.showOverlay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearEntityPositions() {
        entityPositions.clear()
        if (minimapEnabled) {
            scheduleMinimapUpdate()
        }
    }

    fun showMinimap(centerX: Float, centerZ: Float, targets: List<Position>) {
        mainScope.launch {
            MiniMapOverlay.setOverlayEnabled(true)
            MiniMapOverlay.setMinimapSize(minimapSize)
            MiniMapOverlay.setCenter(centerX, centerZ)
            MiniMapOverlay.setTargets(targets)
            MiniMapOverlay.showOverlay()

            minimapEnabled = true
            playerPosition = Position(centerX, centerZ)
        }
    }

    fun enableArrayList(boolean: Boolean) {
        OverlayModuleList.setOverlayEnabled(enabled = boolean)
    }

    fun setArrayListMode(boolean: Boolean) {
        OverlayModuleList.setCapitalizeAndMerge(boolean)
    }

    fun arrayListUi(string: String) {
        OverlayModuleList.setDisplayMode(string)
    }

    fun isProxyPlayer(playerName: String): Boolean {
        return proxyPlayerNames.contains(playerName)
    }

    fun toggleSounds(bool: Boolean) {
        ArrayListManager.setSoundEnabled(bool)
    }

    fun soundList(set: ArrayListManager.SoundSet) {
        ArrayListManager.setCurrentSoundSet(set)
    }

    fun keyPress(key: String, pressed: Boolean) {
        KeystrokesOverlay.setKeyState(key, pressed)
    }

    fun targetHud(user: String, distance: Float, maxdistance: Float, hurtTime: Float) {
        mainScope.launch {
            TargetHudOverlay.showTargetHud(user, null, distance, maxdistance, hurtTime)
        }
    }
}