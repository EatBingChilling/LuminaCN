// File: com/phoenix/luminacn/viewmodel/MainScreenViewModel.kt

package com.phoenix.luminacn.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.util.fastFilter
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val musicModeEnabled: Boolean = true
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
    val captureModeModel = _captureMode_model.asStateFlow()

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

    // ✅ FIXED: Safely handle nullable packageName input.
    fun selectGame(packageName: String?) {
        // If the incoming package name is null, fall back to the default.
        // This makes the assignment to the non-nullable `_selectedGame.value` safe.
        _selectedGame.value = packageName ?: "com.mojang.minecraftpe"
        
        // When saving to SharedPreferences, we can save the actual value (which could be null).
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
                musicModeEnabled = settingsPrefs.getBoolean("musicModeEnabled", true)
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