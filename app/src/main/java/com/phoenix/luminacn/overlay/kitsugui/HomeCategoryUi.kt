package com.phoenix.luminacn.overlay.kitsugui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phoenix.luminacn.constructors.GameManager
import kotlinx.coroutines.delay
import org.cloudburstmc.math.vector.Vector2f
import org.cloudburstmc.nbt.NbtList
import org.cloudburstmc.nbt.NbtMap
import org.cloudburstmc.protocol.bedrock.data.*
import org.cloudburstmc.protocol.common.util.OptionalBoolean
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCategoryUi() {
    /* ===================== 所有状态变量（与旧代码一一对应） ===================== */
    var levelName by remember { mutableStateOf<String?>(null) }
    var levelId by remember { mutableStateOf<String?>(null) }
    var gameMode by remember { mutableStateOf<String?>(null) }
    var vanillaVersion by remember { mutableStateOf<String?>(null) }
    var uniqueEntityId by remember { mutableStateOf<Long?>(null) }
    var runtimeEntityId by remember { mutableStateOf<Long?>(null) }
    var playerPosition by remember { mutableStateOf<String?>(null) }
    var rotation by remember { mutableStateOf<Vector2f?>(null) }
    var seed by remember { mutableStateOf<Long?>(null) }
    var spawnBiomeType by remember { mutableStateOf<SpawnBiomeType?>(null) }
    var customBiomeName by remember { mutableStateOf<String?>(null) }
    var dimensionId by remember { mutableStateOf<Int?>(null) }
    var generatorId by remember { mutableStateOf<Int?>(null) }
    var levelGameType by remember { mutableStateOf<GameType?>(null) }
    var difficulty by remember { mutableStateOf<Int?>(null) }
    var defaultSpawn by remember { mutableStateOf<String?>(null) }
    var achievementsDisabled by remember { mutableStateOf<Boolean?>(null) }
    var dayCycleStopTime by remember { mutableStateOf<Int?>(null) }
    var eduEditionOffers by remember { mutableStateOf<Int?>(null) }
    var eduFeaturesEnabled by remember { mutableStateOf<Boolean?>(null) }
    var educationProductionId by remember { mutableStateOf<String?>(null) }
    var rainLevel by remember { mutableStateOf<Float?>(null) }
    var lightningLevel by remember { mutableStateOf<Float?>(null) }
    var platformLockedContentConfirmed by remember { mutableStateOf<Boolean?>(null) }
    var multiplayerGame by remember { mutableStateOf<Boolean?>(null) }
    var broadcastingToLan by remember { mutableStateOf<Boolean?>(null) }
    var xblBroadcastMode by remember { mutableStateOf<GamePublishSetting?>(null) }
    var platformBroadcastMode by remember { mutableStateOf<GamePublishSetting?>(null) }
    var commandsEnabled by remember { mutableStateOf<Boolean?>(null) }
    var texturePacksRequired by remember { mutableStateOf<Boolean?>(null) }
    var experiments by remember { mutableStateOf<List<*>?>(null) }
    var experimentsPreviouslyToggled by remember { mutableStateOf<Boolean?>(null) }
    var bonusChestEnabled by remember { mutableStateOf<Boolean?>(null) }
    var startingWithMap by remember { mutableStateOf<Boolean?>(null) }
    var trustingPlayers by remember { mutableStateOf<Boolean?>(null) }
    var defaultPlayerPermission by remember { mutableStateOf<PlayerPermission?>(null) }
    var serverChunkTickRange by remember { mutableStateOf<Int?>(null) }
    var behaviorPackLocked by remember { mutableStateOf<Boolean?>(null) }
    var resourcePackLocked by remember { mutableStateOf<Boolean?>(null) }
    var fromLockedWorldTemplate by remember { mutableStateOf<Boolean?>(null) }
    var usingMsaGamertagsOnly by remember { mutableStateOf<Boolean?>(null) }
    var fromWorldTemplate by remember { mutableStateOf<Boolean?>(null) }
    // ================= FIX START =================
    // Corrected the typo from mutableStateStateOf to mutableStateOf
    var worldTemplateOptionLocked by remember { mutableStateOf<Boolean?>(null) }
    // ================== FIX END ==================
    var onlySpawningV1Villagers by remember { mutableStateOf<Boolean?>(null) }
    var limitedWorldWidth by remember { mutableStateOf<Int?>(null) }
    var limitedWorldHeight by remember { mutableStateOf<Int?>(null) }
    var netherType by remember { mutableStateOf<Boolean?>(null) }
    var eduSharedUriResource by remember { mutableStateOf<EduSharedUriResource?>(null) }
    var forceExperimentalGameplay by remember { mutableStateOf<OptionalBoolean?>(null) }
    var chatRestrictionLevel by remember { mutableStateOf<ChatRestrictionLevel?>(null) }
    var disablingPlayerInteractions by remember { mutableStateOf<Boolean?>(null) }
    var disablingPersonas by remember { mutableStateOf<Boolean?>(null) }
    var disablingCustomSkins by remember { mutableStateOf<Boolean?>(null) }
    var premiumWorldTemplateId by remember { mutableStateOf<String?>(null) }
    var trial by remember { mutableStateOf<Boolean?>(null) }
    var authoritativeMovementMode by remember { mutableStateOf<AuthoritativeMovementMode?>(null) }
    var rewindHistorySize by remember { mutableStateOf<Int?>(null) }
    var serverAuthoritativeBlockBreaking by remember { mutableStateOf<Boolean?>(null) }
    var currentTick by remember { mutableStateOf<Long?>(null) }
    var enchantmentSeed by remember { mutableStateOf<Int?>(null) }
    var blockPalette by remember { mutableStateOf<NbtList<*>?>(null) }
    var blockProperties by remember { mutableStateOf<List<*>?>(null) }
    var itemDefinitions by remember { mutableStateOf<List<*>?>(null) }
    var multiplayerCorrelationId by remember { mutableStateOf<String?>(null) }
    var inventoriesServerAuthoritative by remember { mutableStateOf<Boolean?>(null) }
    var playerPropertyData by remember { mutableStateOf<NbtMap?>(null) }
    var blockRegistryChecksum by remember { mutableStateOf<Long?>(null) }
    var worldTemplateId by remember { mutableStateOf<UUID?>(null) }
    var worldEditor by remember { mutableStateOf<Boolean?>(null) }
    var clientSideGenerationEnabled by remember { mutableStateOf<Boolean?>(null) }
    var emoteChatMuted by remember { mutableStateOf<Boolean?>(null) }
    var blockNetworkIdsHashed by remember { mutableStateOf<Boolean?>(null) }
    var createdInEditor by remember { mutableStateOf<Boolean?>(null) }
    var exportedFromEditor by remember { mutableStateOf<Boolean?>(null) }
    var networkPermissions by remember { mutableStateOf<NetworkPermissions?>(null) }
    var hardcore by remember { mutableStateOf<Boolean?>(null) }
    var serverId by remember { mutableStateOf<String?>(null) }
    var worldId by remember { mutableStateOf<String?>(null) }
    var scenarioId by remember { mutableStateOf<String?>(null) }
    var gamerules by remember { mutableStateOf<List<*>?>(null) }
    var serverEngine by remember { mutableStateOf<String?>(null) }
    var worldSpawn by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var hasData by remember { mutableStateOf(false) }

    /* ===================== 数据拉取协程（与旧代码一致） ===================== */
    LaunchedEffect(Unit) {
        while (true) {
            runCatching {
                val netBound = GameManager.netBound
                if (netBound != null) {
                    val dm = netBound.gameDataManager
                    if (dm.hasStartGameData()) {
                        levelName = dm.getLevelName()
                        levelId = dm.getLevelId()
                        gameMode = dm.getGameMode()?.toString()
                        vanillaVersion = dm.getVanillaVersion()
                        uniqueEntityId = dm.getUniqueEntityId()
                        runtimeEntityId = dm.getRuntimeEntityId()
                        playerPosition = dm.getPlayerPosition()?.let {
                            "X: ${it.x.toInt()}, Y: ${it.y.toInt()}, Z: ${it.z.toInt()}"
                        }
                        rotation = dm.getRotation()
                        seed = dm.getSeed()
                        spawnBiomeType = dm.getSpawnBiomeType()
                        customBiomeName = dm.getCustomBiomeName()
                        dimensionId = dm.getDimensionId()
                        generatorId = dm.getGeneratorId()
                        levelGameType = dm.getLevelGameType()
                        difficulty = dm.getDifficulty()
                        defaultSpawn = dm.getDefaultSpawn()?.let {
                            "X: ${it.x}, Y: ${it.y}, Z: ${it.z}"
                        }
                        achievementsDisabled = dm.getAchievementsDisabled()
                        dayCycleStopTime = dm.getDayCycleStopTime()
                        eduEditionOffers = dm.getEduEditionOffers()
                        eduFeaturesEnabled = dm.getEduFeaturesEnabled()
                        educationProductionId = dm.getEducationProductionId()
                        rainLevel = dm.getRainLevel()
                        lightningLevel = dm.getLightningLevel()
                        platformLockedContentConfirmed = dm.getPlatformLockedContentConfirmed()
                        multiplayerGame = dm.getMultiplayerGame()
                        broadcastingToLan = dm.getBroadcastingToLan()
                        xblBroadcastMode = dm.getXblBroadcastMode()
                        platformBroadcastMode = dm.getPlatformBroadcastMode()
                        commandsEnabled = dm.getCommandsEnabled()
                        texturePacksRequired = dm.getTexturePacksRequired()
                        experiments = dm.getExperiments()
                        experimentsPreviouslyToggled = dm.getExperimentsPreviouslyToggled()
                        bonusChestEnabled = dm.getBonusChestEnabled()
                        startingWithMap = dm.getStartingWithMap()
                        trustingPlayers = dm.getTrustingPlayers()
                        defaultPlayerPermission = dm.getDefaultPlayerPermission()
                        serverChunkTickRange = dm.getServerChunkTickRange()
                        behaviorPackLocked = dm.getBehaviorPackLocked()
                        resourcePackLocked = dm.getResourcePackLocked()
                        fromLockedWorldTemplate = dm.getFromLockedWorldTemplate()
                        usingMsaGamertagsOnly = dm.getUsingMsaGamertagsOnly()
                        fromWorldTemplate = dm.getFromWorldTemplate()
                        worldTemplateOptionLocked = dm.getWorldTemplateOptionLocked()
                        onlySpawningV1Villagers = dm.getOnlySpawningV1Villagers()
                        limitedWorldWidth = dm.getLimitedWorldWidth()
                        limitedWorldHeight = dm.getLimitedWorldHeight()
                        netherType = dm.getNetherType()
                        eduSharedUriResource = dm.getEduSharedUriResource()
                        forceExperimentalGameplay = dm.getForceExperimentalGameplay()
                        chatRestrictionLevel = dm.getChatRestrictionLevel()
                        disablingPlayerInteractions = dm.getDisablingPlayerInteractions()
                        disablingPersonas = dm.getDisablingPersonas()
                        disablingCustomSkins = dm.getDisablingCustomSkins()
                        premiumWorldTemplateId = dm.getPremiumWorldTemplateId()
                        trial = dm.getTrial()
                        authoritativeMovementMode = dm.getAuthoritativeMovementMode()
                        rewindHistorySize = dm.getRewindHistorySize()
                        serverAuthoritativeBlockBreaking = dm.getServerAuthoritativeBlockBreaking()
                        currentTick = dm.getCurrentTick()
                        enchantmentSeed = dm.getEnchantmentSeed()
                        blockPalette = dm.getBlockPalette()
                        blockProperties = dm.getBlockProperties()
                        itemDefinitions = dm.getItemDefinitions()
                        multiplayerCorrelationId = dm.getMultiplayerCorrelationId()
                        inventoriesServerAuthoritative = dm.getInventoriesServerAuthoritative()
                        playerPropertyData = dm.getPlayerPropertyData()
                        blockRegistryChecksum = dm.getBlockRegistryChecksum()
                        worldTemplateId = dm.getWorldTemplateId()
                        worldEditor = dm.getWorldEditor()
                        clientSideGenerationEnabled = dm.getClientSideGenerationEnabled()
                        emoteChatMuted = dm.getEmoteChatMuted()
                        blockNetworkIdsHashed = dm.getBlockNetworkIdsHashed()
                        createdInEditor = dm.getCreatedInEditor()
                        exportedFromEditor = dm.getExportedFromEditor()
                        networkPermissions = dm.getNetworkPermissions()
                        hardcore = dm.getHardcore()
                        serverId = dm.getServerId()
                        worldId = dm.getWorldId()
                        scenarioId = dm.getScenarioId()
                        gamerules = dm.getGamerules()
                        serverEngine = dm.getServerEngine()
                        worldSpawn = dm.getWorldSpawn()?.let {
                            "X: ${it.x}, Y: ${it.y}, Z: ${it.z}"
                        }
                        hasData = true
                        isLoading = false
                    } else {
                        hasData = false
                        isLoading = false
                    }
                } else {
                    hasData = false
                    isLoading = false
                }
            }.onFailure {
                hasData = false
                isLoading = false
            }
            delay(2.seconds)
        }
    }

    /* ===================== 布局（已迁移到 M3） ===================== */
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /* 网络状态 */
            ElevatedCard(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hasData) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                        contentDescription = null,
                        tint = if (hasData) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (hasData) "已连接" else "未连接",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = if (hasData) "游戏记录可用" else "无游戏记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            /* 版本详情 */
            ElevatedCard(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "游戏版本",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when {
                                    isLoading -> "加载中…"
                                    !hasData -> "无数据"
                                    vanillaVersion.isNullOrEmpty() -> "未知"
                                    else -> vanillaVersion!!
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(Icons.Outlined.Gamepad, "模式", gameMode ?: "未知")
                        InfoRow(Icons.Outlined.Badge, "生物 ID", uniqueEntityId?.toString() ?: "N/A")

                        val hasSufficientPermission = defaultPlayerPermission != null && defaultPlayerPermission != PlayerPermission.VISITOR
                        InfoRow(
                            icon = if (hasSufficientPermission) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                            label = "权限",
                            value = if (hasSufficientPermission) "开启" else "关闭"
                        )
                        
                        InfoRow(Icons.Outlined.Code, "命令", if (commandsEnabled == true) "开启" else "关闭")
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /* 致谢卡片 */
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "您正在使用 LuminaCN B23",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "致谢名单:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "开发者: Phoen1x_, 十一\n技术支持: 一剪沐橙\n灵动岛美化: QP (鲨鱼)\n\n请根据教程后进入 127.0.0.1:19132 再使用功能，否则无效！\n不按照教程进行社区提问的，一律移出",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            /* 当前存档 */
            ElevatedCard(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前存档",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                isLoading -> "加载中…"
                                !hasData -> "无存档记录"
                                levelName.isNullOrEmpty() -> "未知存档"
                                else -> levelName!!
                            },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/* ===================== 工具组件 ===================== */
@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}