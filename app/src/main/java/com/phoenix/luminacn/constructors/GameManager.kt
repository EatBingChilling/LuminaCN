package com.phoenix.luminacn.constructors


import com.phoenix.luminacn.application.AppContext
import com.phoenix.luminacn.game.module.impl.combat.AntiCrystalElement
import com.phoenix.luminacn.game.module.impl.combat.CritBotElement
import com.phoenix.luminacn.game.module.impl.combat.InfiniteAuraElement
import com.phoenix.luminacn.game.module.impl.combat.KillauraElement
import com.phoenix.luminacn.game.module.impl.combat.KillauraABElement
import com.phoenix.luminacn.game.module.impl.combat.QuickAttackElement
import com.phoenix.luminacn.game.module.impl.world.StrafeElement
import com.phoenix.luminacn.game.module.impl.combat.TPAuraElement
import com.phoenix.luminacn.game.module.impl.combat.TriggerBotElement
import com.phoenix.luminacn.game.module.impl.combat.VelocityElement
import com.phoenix.luminacn.game.module.impl.combat.ReachElement
import com.phoenix.luminacn.game.module.api.config.ConfigManagerElement
import com.phoenix.luminacn.game.module.impl.world.HasteElement
import com.phoenix.luminacn.game.module.impl.misc.AntiKickElement
import com.phoenix.luminacn.game.module.impl.misc.ArrayListElement
import com.phoenix.luminacn.game.module.impl.misc.CrasherElement
import com.phoenix.luminacn.game.module.impl.misc.DesyncElement
import com.phoenix.luminacn.game.module.impl.world.NoClipElement
import com.phoenix.luminacn.game.module.impl.misc.PositionLoggerElement
import com.phoenix.luminacn.game.module.impl.misc.SessionInfoElement
import com.phoenix.luminacn.game.module.impl.misc.SpeedoMeterElement
import com.phoenix.luminacn.game.module.impl.misc.WaterMarkElement
import com.phoenix.luminacn.game.module.impl.motion.AirJumpElement
import com.phoenix.luminacn.game.module.impl.motion.AntiAFKElement
import com.phoenix.luminacn.game.module.impl.world.AutoWalkElement
import com.phoenix.luminacn.game.module.impl.motion.BhopElement
//import com.phoenix.luminacn.game.module.impl.combat.CrystalAuraElement
import com.phoenix.luminacn.game.module.impl.combat.AntiBotElement
import com.phoenix.luminacn.game.module.impl.combat.DamageBoostElement
import com.phoenix.luminacn.game.module.impl.combat.HitboxElement
import com.phoenix.luminacn.game.module.impl.motion.FlyElement
import com.phoenix.luminacn.game.module.impl.world.FollowBotElement
import com.phoenix.luminacn.game.module.impl.motion.FullStopElement
import com.phoenix.luminacn.game.module.impl.motion.GlideElement
import com.phoenix.luminacn.game.module.impl.motion.HighJumpElement
import com.phoenix.luminacn.game.module.impl.motion.JetPackElement
import com.phoenix.luminacn.game.module.impl.motion.JitterFlyElement
import com.phoenix.luminacn.game.module.impl.motion.LongJumpElement
import com.phoenix.luminacn.game.module.impl.motion.MotionFlyElement
import com.phoenix.luminacn.game.module.impl.combat.OpFightBotElement
import com.phoenix.luminacn.game.module.api.commands.CmdListener
import com.phoenix.luminacn.game.module.impl.misc.KeyStrokes
import com.phoenix.luminacn.game.module.impl.misc.TargetHud
import com.phoenix.luminacn.game.module.impl.visual.NoFireElement
import com.phoenix.luminacn.game.module.impl.misc.ToggleSound
import com.phoenix.luminacn.game.module.impl.motion.AntiACFly
import com.phoenix.luminacn.game.module.impl.world.PhaseElement
import com.phoenix.luminacn.game.module.impl.motion.SpeedElement
import com.phoenix.luminacn.game.module.impl.motion.SpiderElement
import com.phoenix.luminacn.game.module.impl.motion.StepElement
import com.phoenix.luminacn.game.module.impl.motion.SprintElement
import com.phoenix.luminacn.game.module.impl.visual.AntiBlindElement
import com.phoenix.luminacn.game.module.impl.visual.FreeCameraElement
import com.phoenix.luminacn.game.module.impl.visual.FullBrightElement
import com.phoenix.luminacn.game.module.impl.visual.NameTagElement
import com.phoenix.luminacn.game.module.impl.visual.NoHurtCameraElement
import com.phoenix.luminacn.game.module.impl.visual.TextSpoofElement
import com.phoenix.luminacn.game.module.impl.visual.ZoomElement
import com.phoenix.luminacn.game.module.impl.visual.EspElement
import com.phoenix.luminacn.game.module.impl.world.AutoNavigatorElement
import com.phoenix.luminacn.game.module.impl.world.MinimapElement
import com.phoenix.luminacn.game.module.impl.misc.PingSpoofElement
import com.phoenix.luminacn.game.module.impl.motion.FarSightElement
import com.phoenix.luminacn.game.module.impl.visual.DamageTextElement
import com.phoenix.luminacn.game.module.impl.world.JesusElement
import com.phoenix.luminacn.game.module.impl.misc.PlayerTracerElement
import com.phoenix.luminacn.game.module.impl.misc.ReplayElement
import com.phoenix.luminacn.game.module.impl.misc.TimeShiftElement
import com.phoenix.luminacn.game.module.impl.misc.WeatherControllerElement

import com.phoenix.luminacn.game.module.impl.combat.LockHeedElement
import com.phoenix.luminacn.game.module.impl.combat.KillauraCDElement
import com.phoenix.luminacn.game.module.impl.combat.RotationAuraElement

import com.phoenix.luminacn.game.module.impl.combat.AutoTotemElement


import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import com.phoenix.luminacn.service.Services
import java.io.File

object GameManager {

    private val _elements: MutableList<Element> = ArrayList()

    val elements: List<Element> = _elements

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        with(_elements) {
            add(KillauraCDElement())
            add(LockHeedElement())
            add(RotationAuraElement())
            add(FlyElement())
            add(ZoomElement())
            add(AirJumpElement())
            add(AutoWalkElement())
            add(NoClipElement())
            add(AutoTotemElement())
            add(HasteElement())
            add(SpeedElement())
            add(JetPackElement())
            add(HighJumpElement())
            add(BhopElement())
            add(NoHurtCameraElement())
            add(AntiAFKElement())
            add(PositionLoggerElement())
            add(MotionFlyElement())
            add(FreeCameraElement())
            add(KillauraElement())
            add(KillauraABElement())
            add(GlideElement())
            add(StepElement())
            add(LongJumpElement())
            add(SpiderElement())
            add(TPAuraElement())
            add(StrafeElement())
            add(FullStopElement())
            add(JitterFlyElement())
            add(PhaseElement())
            add(EspElement())
            add(ReachElement())
            //add(MaceAuraElement())
            //add(CrystalAuraElement())
            add(AntiBotElement())
            add(TriggerBotElement())
            add(CritBotElement())
            add(InfiniteAuraElement())
            add(DamageBoostElement())
            add(FullBrightElement())
            add(OpFightBotElement())
            add(FollowBotElement())
            add(VelocityElement())
            add(AntiKickElement())
            add(QuickAttackElement())
            add(AutoNavigatorElement())
            add(ConfigManagerElement())
            add(AntiCrystalElement())
            add(CrasherElement())
            add(NoFireElement())
            add(AntiACFly())
            add(FarSightElement())
            add(DamageTextElement())
            add(TextSpoofElement())
            add(PingSpoofElement())
            add(CmdListener(this@GameManager))

            if (Services.RemisOnline == false){
                add(SpeedoMeterElement())
                add(SessionInfoElement())
                add(ArrayListElement())
                add(WaterMarkElement())
                add(MinimapElement())
                add(ToggleSound())
                add(DesyncElement())
                add(HitboxElement())
                add(KeyStrokes())
                add(TargetHud())
                add(NameTagElement())
                add(AntiBlindElement())
                add(SprintElement())
                add(JesusElement())
                add(PlayerTracerElement())
                add(ReplayElement())
                add(TimeShiftElement())
                add(WeatherControllerElement())

            }
        }


     
    }


    var netBound: NetBound? = null
        private set

    fun setNetBound(netBound: NetBound) {
        this.netBound = netBound
    }

    fun clearNetBound() {
        this.netBound = null
    }

    fun getCurrentPlayers(): List<GameDataManager.PlayerInfo> {
        return netBound?.getCurrentPlayers() ?: emptyList()
    }

    fun getPlayerCount(): Int {
        return netBound?.getPlayerCount() ?: 0
    }

    fun isConnected(): Boolean {
        return netBound != null
    }


    fun getModule(name: String): Element? {
        return elements.find { it.name.equals(name, ignoreCase = true) }
    }

    fun saveConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        saveConfigToFile(config)
    }

    fun saveConfigToFile(configFile: File) {
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _elements.forEach {
                    put(it.name, it.toJson())
                }
            })
        }

        configFile.writeText(json.encodeToString(jsonObject))
    }

    fun loadConfig() {
        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        if (!config.exists()) return


        loadConfigFromFile(config)
    }

    fun loadConfigFromFile(configFile: File) {
        if (!configFile.exists()) {
            return
        }

        val jsonString = configFile.readText()
        if (jsonString.isEmpty()) {
            return
        }

        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        val modules = jsonObject["modules"]!!.jsonObject
        _elements.forEach { module ->
            (modules[module.name] as? JsonObject)?.let {
                module.fromJson(it)
            }
        }
    }

}
