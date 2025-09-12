// com/phoenix/luminacn/viewmodel/MainScreenViewModel.kt

package com.phoenix.luminacn.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.util.fastFilter
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.luminacn.WallpaperUtils
import com.phoenix.luminacn.application.AppContext
import com.phoenix.luminacn.model.CaptureModeModel
import com.phoenix.luminacn.router.main.MainScreenPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data class to hold all settings from the settings page.
data class SettingsState(
    val optimizeNetworkEnabled: Boolean = false,
    val priorityThreadsEnabled: Boolean = false,
    val fastDnsEnabled: Boolean = false,
    val injectNekoPackEnabled: Boolean = false,
    val disableOverlay: Boolean = false,
    val selectedGUI: String = "ProtohaxUi",
    val dynamicIslandUsername: String = "User",
    val dynamicIslandYOffset: Float = 20f,
    val dynamicIslandScale: Float = 0.7f,
    val musicModeEnabled: Boolean = true,
    // [新增] 壁纸设置状态
    val wallpaperEnabled: Boolean = false,
    val wallpaperBlurEnabled: Boolean = true,
    val wallpaperBlurRadius: Float = 20f
)

class MainScreenViewModel : ViewModel() {

    enum class PackageInfoState {
        Loading, Done
    }

    private val gameSettingsSharedPreferences by lazy {
        AppContext.instance.getSharedPreferences("game_settings", Context.MODE_PRIVATE)
    }

    private val settingsPrefs by lazy {
        AppContext.instance.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)
    }

    private val packageManager by lazy { AppContext.instance.packageManager }

    private val _selectedPage = MutableStateFlow(MainScreenPages.HomePage)
    val selectedPage = _selectedPage.asStateFlow()

    private val _captureModeModel = MutableStateFlow(initialCaptureModeModel())
    val captureModeModel = _captureModeModel.asStateFlow()

    private val _packageInfos = MutableStateFlow<List<PackageInfo>>(emptyList())
    val packageInfos = _packageInfos.asStateFlow()

    private val _packageInfoState = MutableStateFlow(PackageInfoState.Loading)
    val packageInfoState = _packageInfoState.asStateFlow()

    private val _selectedGame = MutableStateFlow(initialSelectedGame())
    val selectedGame = _selectedGame.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState = _settingsState.asStateFlow()

    init {
        loadSettingsFromPrefs()
        fetchPackageInfos()
    }

    fun selectPage(page: MainScreenPages) {
        _selectedPage.value = page
    }

    fun selectCaptureModeModel(captureModeModel: CaptureModeModel) {
        _captureModeModel.value = captureModeModel
        captureModeModel.to(gameSettingsSharedPreferences)
    }

    fun selectGame(packageName: String?) {
        _selectedGame.value = packageName ?: "com.mojang.minecraftpe"
        
        gameSettingsSharedPreferences.edit {
            putString("selected_game", packageName)
        }
        settingsPrefs.edit {
            putString("selectedAppPackage", packageName)
        }
    }

    fun fetchPackageInfos() {
        viewModelScope.launch(Dispatchers.IO) {
            _packageInfoState.value = PackageInfoState.Loading
            try {
                _packageInfos.value = packageManager.getInstalledPackages(0)
                    .fastFilter {
                        val appInfo = it.applicationInfo
                        appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                                packageManager.getLaunchIntentForPackage(it.packageName) != null
                    }
                    .sortedBy { appName(it.packageName) }
            } finally {
                _packageInfoState.value = PackageInfoState.Done
            }
        }
    }

    fun appName(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) { pkg }

    fun appVer(pkg: String): String = try {
        packageManager.getPackageInfo(pkg, 0).versionName ?: "?"
    } catch (e: Exception) { "?" }

    private fun loadSettingsFromPrefs() {
        val context = AppContext.instance
        // [修改] 从WallpaperUtils加载壁纸配置
        val wallpaperConfig = WallpaperUtils.getBlurConfig(context)

        _settingsState.update {
            it.copy(
                optimizeNetworkEnabled = settingsPrefs.getBoolean("optimizeNetworkEnabled", false),
                priorityThreadsEnabled = settingsPrefs.getBoolean("priorityThreadsEnabled", false),
                fastDnsEnabled = settingsPrefs.getBoolean("fastDnsEnabled", false),
                injectNekoPackEnabled = settingsPrefs.getBoolean("injectNekoPackEnabled", false),
                disableOverlay = settingsPrefs.getBoolean("disableConnectionInfoOverlay", false),
                selectedGUI = settingsPrefs.getString("selectedGUI", "ProtohaxUi") ?: "ProtohaxUi",
                dynamicIslandUsername = settingsPrefs.getString("dynamicIslandUsername", "User") ?: "User",
                dynamicIslandYOffset = settingsPrefs.getFloat("dynamicIslandYOffset", 20f),
                dynamicIslandScale = settingsPrefs.getFloat("dynamicIslandScale", 0.7f),
                musicModeEnabled = settingsPrefs.getBoolean("musicModeEnabled", true),
                // [新增] 初始化壁纸状态
                wallpaperEnabled = settingsPrefs.getBoolean("wallpaperEnabled", WallpaperUtils.hasCustomWallpaper(context)),
                wallpaperBlurEnabled = wallpaperConfig.enableBlur,
                wallpaperBlurRadius = wallpaperConfig.blurRadius
            )
        }
    }
    
    // --- Public update functions for every setting ---

    fun updateOptimizeNetwork(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("optimizeNetworkEnabled", enabled).apply()
        _settingsState.update { it.copy(optimizeNetworkEnabled = enabled) }
    }

    fun updatePriorityThreads(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("priorityThreadsEnabled", enabled).apply()
        _settingsState.update { it.copy(priorityThreadsEnabled = enabled) }
    }

    fun updateFastDns(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("fastDnsEnabled", enabled).apply()
        _settingsState.update { it.copy(fastDnsEnabled = enabled) }
    }

    fun updateInjectNekoPack(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("injectNekoPackEnabled", enabled).apply()
        _settingsState.update { it.copy(injectNekoPackEnabled = enabled) }
    }

    fun updateDisableOverlay(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("disableConnectionInfoOverlay", enabled).apply()
        _settingsState.update { it.copy(disableOverlay = enabled) }
    }

    fun updateSelectedGui(gui: String) {
        settingsPrefs.edit().putString("selectedGUI", gui).apply()
        _settingsState.update { it.copy(selectedGUI = gui) }
    }

    fun updateDynamicIslandUsername(name: String) {
        settingsPrefs.edit().putString("dynamicIslandUsername", name).apply()
        _settingsState.update { it.copy(dynamicIslandUsername = name) }
    }

    fun updateDynamicIslandYOffset(offset: Float) {
        _settingsState.update { it.copy(dynamicIslandYOffset = offset) }
    }

    fun saveDynamicIslandYOffset() {
        settingsPrefs.edit().putFloat("dynamicIslandYOffset", _settingsState.value.dynamicIslandYOffset).apply()
    }

    fun updateDynamicIslandScale(scale: Float) {
        _settingsState.update { it.copy(dynamicIslandScale = scale) }
    }

    fun saveDynamicIslandScale() {
        settingsPrefs.edit().putFloat("dynamicIslandScale", _settingsState.value.dynamicIslandScale).apply()
    }

    fun updateMusicMode(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("musicModeEnabled", enabled).apply()
        _settingsState.update { it.copy(musicModeEnabled = enabled) }
    }

    // --- [新增] 壁纸设置更新方法 ---
    /**
     * 更新是否启用壁纸
     * @param enabled 是否启用
     * @param solidBackgroundColor 禁用时使用的纯色背景颜色
     */
    fun updateWallpaperEnabled(enabled: Boolean, solidBackgroundColor: Int) {
        settingsPrefs.edit().putBoolean("wallpaperEnabled", enabled).apply()
        _settingsState.update { it.copy(wallpaperEnabled = enabled) }

        if (!enabled) {
            // 如果禁用壁纸，则设置一个纯色背景
            WallpaperUtils.setSolidColorWallpaper(AppContext.instance, solidBackgroundColor)
        }
        // 如果启用，用户需要通过UI选择一张图片。如果之前已有图片则会自动显示。
    }
    
    /**
     * 当用户成功选择新壁纸后调用
     */
    fun onWallpaperSelected() {
        // 确保启用状态为true
        if (!_settingsState.value.wallpaperEnabled) {
            settingsPrefs.edit().putBoolean("wallpaperEnabled", true).apply()
            _settingsState.update { it.copy(wallpaperEnabled = true) }
        }
    }

    /**
     * 更新壁纸模糊配置
     * @param blurEnabled 是否启用模糊
     * @param blurRadius 模糊半径
     */
    fun updateWallpaperConfig(blurEnabled: Boolean? = null, blurRadius: Float? = null) {
        val currentState = _settingsState.value
        val newBlurEnabled = blurEnabled ?: currentState.wallpaperBlurEnabled
        val newBlurRadius = blurRadius ?: currentState.wallpaperBlurRadius

        _settingsState.update { it.copy(
            wallpaperBlurEnabled = newBlurEnabled,
            wallpaperBlurRadius = newBlurRadius
        )}
        
        // 调用工具类更新实际配置
        WallpaperUtils.setBlurConfig(
            context = AppContext.instance,
            enableBlur = newBlurEnabled,
            blurRadius = newBlurRadius
        )
    }

    // --- Initializers ---
    private fun initialCaptureModeModel(): CaptureModeModel {
        return CaptureModeModel.from(gameSettingsSharedPreferences)
    }

    private fun initialSelectedGame(): String {
        return settingsPrefs.getString("selectedAppPackage", null)
            ?: gameSettingsSharedPreferences.getString("selected_game", null)
            ?: "com.mojang.minecraftpe"
    }
}