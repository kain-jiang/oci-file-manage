package com.tiramission.ocisync.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tiramission.ocisync.core.config.ConfigLoader
import com.tiramission.ocisync.core.config.RegistryAuth
import com.tiramission.ocisync.core.config.Shortcut
import com.tiramission.ocisync.core.oci.AuthCheckResult
import com.tiramission.ocisync.core.oci.Credential
import com.tiramission.ocisync.core.oci.OciClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 设置页状态:凭据 + 快捷仓库管理(含编辑)+ 关于,ui-design/ 设置.html。 */
class SettingsViewModel(
    private val configLoader: ConfigLoader,
    private val ociClient: OciClient,
) : ViewModel() {

    data class UiState(
        val auths: List<Pair<String, RegistryAuth>> = emptyList(),
        val shortcuts: List<Pair<String, Shortcut>> = emptyList(),
        val credentialHost: String = "",
        val credentialUsername: String = "",
        val credentialPassword: String = "",
        val credentialVerifying: Boolean = false,
        val shortcutName: String = "",
        val shortcutRepo: String = "",
        val editingShortcut: String? = null,   // 正在编辑的 shortcut 名
        val message: String? = null,           // 提示(Snackbar)
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                auths = configLoader.load().auths.toList(),
                shortcuts = configLoader.getAllShortcuts(),
            )
        }
    }

    fun onCredentialHostChange(v: String) = _uiState.update { it.copy(credentialHost = v) }
    fun onCredentialUsernameChange(v: String) = _uiState.update { it.copy(credentialUsername = v) }
    fun onCredentialPasswordChange(v: String) = _uiState.update { it.copy(credentialPassword = v) }

    fun onShortcutNameChange(v: String) = _uiState.update { it.copy(shortcutName = v) }
    fun onShortcutRepoChange(v: String) = _uiState.update { it.copy(shortcutRepo = v) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** 编辑 shortcut:预填表单进入编辑态。 */
    fun startEditShortcut(name: String, repo: String) {
        _uiState.update {
            it.copy(editingShortcut = name, shortcutName = name, shortcutRepo = repo)
        }
    }

    fun cancelEditShortcut() {
        _uiState.update { it.copy(editingShortcut = null, shortcutName = "", shortcutRepo = "") }
    }

    /** 保存 shortcut(新增或编辑)。返回是否成功。 */
    fun saveShortcut(): Boolean {
        val s = _uiState.value
        if (s.shortcutName.isBlank() || s.shortcutRepo.isBlank()) return false
        val name = s.shortcutName.trim()
        val repo = s.shortcutRepo.trim()
        val result = if (s.editingShortcut != null) {
            configLoader.updateShortcut(name, repo)
        } else {
            configLoader.addShortcut(name, repo)
        }
        if (result.isFailure) return false
        _uiState.update { it.copy(editingShortcut = null, shortcutName = "", shortcutRepo = "") }
        refresh()
        return true
    }

    fun removeShortcut(name: String) {
        configLoader.removeShortcut(name)
        refresh()
    }

    /**
     * 添加凭据:先向 registry 验证凭据有效性,通过才保存。
     * 结果经 [UiState.message] 提示。
     */
    fun addCredential() {
        val s = _uiState.value
        if (s.credentialHost.isBlank()) return
        val host = s.credentialHost.trim()
        // 凭据统一 trim(用户可能从登录指令复制带空格)
        val credential = Credential(s.credentialUsername.trim(), s.credentialPassword.trim())
        viewModelScope.launch {
            _uiState.update { it.copy(credentialVerifying = true) }
            val result = ociClient.checkCredential(host, credential)
            when (result) {
                AuthCheckResult.VALID -> {
                    configLoader.addAuth(host, RegistryAuth(credential.username, credential.password))
                    _uiState.update {
                        it.copy(
                            credentialVerifying = false,
                            credentialHost = "",
                            credentialUsername = "",
                            credentialPassword = "",
                            message = CRED_ADDED,
                        )
                    }
                    refresh()
                }
                AuthCheckResult.INVALID -> {
                    _uiState.update { it.copy(credentialVerifying = false, message = CRED_INVALID) }
                    refresh()
                }
                AuthCheckResult.NETWORK_ERROR -> {
                    _uiState.update { it.copy(credentialVerifying = false, message = CRED_NETWORK) }
                    refresh()
                }
            }
        }
    }

    fun removeCredential(host: String) {
        configLoader.removeAuth(host)
        refresh()
    }

    class Factory(
        private val configLoader: ConfigLoader,
        private val ociClient: OciClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(configLoader, ociClient) as T
    }

    companion object {
        // 消息标记,UI 层映射为本地化文案
        const val CRED_ADDED = "cred_added"
        const val CRED_INVALID = "cred_invalid"
        const val CRED_NETWORK = "cred_network"
    }
}
