package com.project.lumina.client.remlink


import com.project.lumina.client.application.AppContext
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.module.impl.combat.AntiCrystalElement
import com.project.lumina.client.game.module.impl.combat.CritBotElement
import com.project.lumina.client.game.module.impl.combat.InfiniteAuraElement
import com.project.lumina.client.game.module.impl.combat.KillauraElement
import com.project.lumina.client.game.module.impl.combat.KillauraABElement
import com.project.lumina.client.game.module.impl.combat.MaceAuraElement
import com.project.lumina.client.game.module.impl.combat.QuickAttackElement
import com.project.lumina.client.game.module.impl.world.StrafeElement
import com.project.lumina.client.game.module.impl.combat.TPAuraElement
import com.project.lumina.client.game.module.impl.combat.TriggerBotElement
import com.project.lumina.client.game.module.impl.combat.VelocityElement
import com.project.lumina.client.game.module.impl.world.HasteElement
import com.project.lumina.client.game.module.impl.misc.AntiKickElement
import com.project.lumina.client.game.module.impl.misc.CrasherElement
import com.project.lumina.client.game.module.impl.misc.DesyncElement
import com.project.lumina.client.game.module.impl.world.NoClipElement
import com.project.lumina.client.game.module.impl.misc.PositionLoggerElement
import com.project.lumina.client.game.module.impl.motion.AirJumpElement
import com.project.lumina.client.game.module.impl.motion.AntiAFKElement
import com.project.lumina.client.game.module.impl.world.AutoWalkElement
import com.project.lumina.client.game.module.impl.motion.BhopElement
import com.project.lumina.client.game.module.impl.combat.DamageBoostElement
import com.project.lumina.client.game.module.impl.motion.FlyElement
import com.project.lumina.client.game.module.impl.world.FollowBotElement
import com.project.lumina.client.game.module.impl.motion.FullStopElement
import com.project.lumina.client.game.module.impl.motion.GlideElement
import com.project.lumina.client.game.module.impl.motion.HighJumpElement
import com.project.lumina.client.game.module.impl.combat.AntiBotElement
import com.project.lumina.client.game.module.impl.world.JesusElement
import com.project.lumina.client.game.module.impl.motion.JetPackElement
//import com.project.lumina.client.game.module.impl.combat.CrystalAuraElement
import com.project.lumina.client.game.module.impl.motion.JitterFlyElement
import com.project.lumina.client.game.module.impl.motion.LongJumpElement
import com.project.lumina.client.game.module.impl.motion.MotionFlyElement
import com.project.lumina.client.game.module.impl.combat.OpFightBotElement
import com.project.lumina.client.game.module.impl.world.PhaseElement
import com.project.lumina.client.game.module.impl.motion.SpeedElement
import com.project.lumina.client.game.module.impl.motion.SpiderElement

import com.project.lumina.client.game.module.impl.motion.StepElement
import com.project.lumina.client.game.module.impl.visual.FreeCameraElement
import com.project.lumina.client.game.module.impl.visual.FullBrightElement
import com.project.lumina.client.game.module.impl.visual.NoHurtCameraElement
import com.project.lumina.client.game.module.impl.visual.TextSpoofElement
import com.project.lumina.client.game.module.impl.visual.ZoomElement
import com.project.lumina.client.game.module.impl.world.AutoNavigatorElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

object RemModuleManager {

    private val _elements: MutableList<Element> = ArrayList()

    val elements: List<Element> = _elements

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        with(_elements) {
            add(FlyElement())
            add(ZoomElement())
            add(AirJumpElement())
            add(AutoWalkElement())
            add(NoClipElement())
            add(HasteElement())
            add(SpeedElement())
            add(JetPackElement())
            add(HighJumpElement())
            add(BhopElement())
            add(NoHurtCameraElement())
            add(AntiAFKElement())
            add(DesyncElement())
            add(PositionLoggerElement())
            add(MotionFlyElement())
            add(FreeCameraElement())
            add(KillauraElement())
            add(KillauraABElement())
            add(AntiBotElement())
            add(GlideElement())
            add(StepElement())
            add(LongJumpElement())
            add(SpiderElement())
            add(JesusElement())
            add(TPAuraElement())
            add(StrafeElement())
            add(FullStopElement())
            add(JitterFlyElement())
            //add(CrystalAuraElement())
            add(PhaseElement())
            add(MaceAuraElement())
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
            add(AntiCrystalElement())
            add(CrasherElement())
            add(TextSpoofElement())
        }
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

        val config = configsDir.resolve("luna.json")
        if (!config.exists()) {
            return
        }

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