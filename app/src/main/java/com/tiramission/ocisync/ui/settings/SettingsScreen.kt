package com.tiramission.ocisync.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiramission.ocisync.BuildConfig
import com.tiramission.ocisync.OciSyncApp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.core.config.RegistryAuth
import com.tiramission.ocisync.core.config.Shortcut
import com.tiramission.ocisync.ui.components.CardDivider
import com.tiramission.ocisync.ui.components.IconTile
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.RadiusMedium
import kotlinx.coroutines.launch

/**
 * 设置页(ui-design/ 设置.html):
 * 凭据管理(卡片列表 + 底部添加按钮 + 添加表单)+ 快捷仓库管理 + 关于,分组卡片风格。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as OciSyncApp
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.container.configLoader, app.container.ociClient)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCredentialForm by remember { mutableStateOf(false) }
    var showShortcutForm by remember { mutableStateOf(false) }

    val msgCredentialAdded = stringResource(R.string.settings_credential_added)
    val msgCredentialRemoved = stringResource(R.string.settings_credential_removed)
    val msgShortcutSaved = stringResource(R.string.settings_shortcut_saved)
    val msgShortcutRemoved = stringResource(R.string.settings_shortcut_removed)
    val msgInvalid = stringResource(R.string.settings_error_invalid)
    val msgCredInvalid = stringResource(R.string.settings_credential_invalid)
    val msgCredNetwork = stringResource(R.string.settings_credential_network)

    LaunchedEffect(uiState.message) {
        val text = when (uiState.message) {
            SettingsViewModel.CRED_ADDED -> msgCredentialAdded
            SettingsViewModel.CRED_INVALID -> msgCredInvalid
            SettingsViewModel.CRED_NETWORK -> msgCredNetwork
            null -> null
            else -> uiState.message
        }
        text?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
            if (uiState.message == SettingsViewModel.CRED_ADDED) showCredentialForm = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 设计稿:顶部返回 + 标题
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── 凭据管理 ──
                item {
                    Text(
                        text = stringResource(R.string.settings_credentials),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                item {
                    CredentialCard(
                        auths = uiState.auths,
                        onDelete = { host -> pendingDelete = "auth" to host },
                        onAddClick = { showCredentialForm = !showCredentialForm },
                    )
                }
                if (showCredentialForm) {
                    item {
                        CredentialForm(
                            host = uiState.credentialHost,
                            username = uiState.credentialUsername,
                            password = uiState.credentialPassword,
                            verifying = uiState.credentialVerifying,
                            onHostChange = viewModel::onCredentialHostChange,
                            onUsernameChange = viewModel::onCredentialUsernameChange,
                            onPasswordChange = viewModel::onCredentialPasswordChange,
                            onAdd = { viewModel.addCredential() },
                        )
                    }
                }

                // ── 快捷仓库管理 ──
                item {
                    Text(
                        text = stringResource(R.string.settings_shortcuts),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                item {
                    ShortcutCard(
                        shortcuts = uiState.shortcuts,
                        onEdit = { name, repo ->
                            viewModel.startEditShortcut(name, repo)
                            showShortcutForm = true
                        },
                        onDelete = { name -> pendingDelete = "shortcut" to name },
                        onAddClick = { showShortcutForm = !showShortcutForm },
                    )
                }
                if (showShortcutForm || uiState.editingShortcut != null) {
                    item {
                        ShortcutForm(
                            name = uiState.shortcutName,
                            repo = uiState.shortcutRepo,
                            editing = uiState.editingShortcut,
                            onNameChange = viewModel::onShortcutNameChange,
                            onRepoChange = viewModel::onShortcutRepoChange,
                            onSave = {
                                val ok = viewModel.saveShortcut()
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (ok) msgShortcutSaved else msgInvalid)
                                }
                                if (ok) showShortcutForm = false
                            },
                            onCancelEdit = {
                                viewModel.cancelEditShortcut()
                                showShortcutForm = false
                            },
                        )
                    }
                }

                // ── 关于 ──
                item {
                    Text(
                        text = stringResource(R.string.settings_about),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                item { AboutCard() }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    pendingDelete?.let { (type, key) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.settings_confirm_delete)) },
            text = { Text(key) },
            confirmButton = {
                TextButton(onClick = {
                    if (type == "auth") viewModel.removeCredential(key) else viewModel.removeShortcut(key)
                    pendingDelete = null
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (type == "auth") msgCredentialRemoved else msgShortcutRemoved
                        )
                    }
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** 凭据卡片:列表 + 底部"添加凭据"按钮(设计稿 设置.html)。 */
@Composable
private fun CredentialCard(
    auths: List<Pair<String, RegistryAuth>>,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (auths.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_credentials),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                auths.forEachIndexed { index, (host, auth) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(
                            icon = Icons.Filled.Dns,
                            size = 36.dp,
                            corner = 10.dp,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = host,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "用户:${auth.username} · ${"•".repeat(auth.password.length.coerceAtMost(8))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(host) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.settings_confirm_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < auths.lastIndex) {
                        CardDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            CardDivider()
            // 底部"添加凭据"按钮(设计稿:主色 + 加号)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onAddClick,
                        indication = ripple(),
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_add_credential),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 凭据添加表单(设计稿:展开后显示输入框 + 验证按钮)。 */
@Composable
private fun CredentialForm(
    host: String,
    username: String,
    password: String,
    verifying: Boolean,
    onHostChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text(stringResource(R.string.settings_host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.settings_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.settings_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
                visualTransformation = PasswordVisualTransformation(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                label = if (verifying) stringResource(R.string.settings_credential_verifying)
                else stringResource(R.string.settings_add_credential),
                enabled = !verifying,
                onClick = onAdd,
                loading = verifying,
            )
        }
    }
}

/** 快捷仓库卡片:列表 + 底部"添加快捷仓库"按钮(设计稿 设置.html)。 */
@Composable
private fun ShortcutCard(
    shortcuts: List<Pair<String, Shortcut>>,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (shortcuts.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_shortcuts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                shortcuts.forEachIndexed { index, (name, shortcut) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = shortcut.repo,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { onEdit(name, shortcut.repo) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.settings_edit),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(name) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.settings_confirm_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < shortcuts.lastIndex) {
                        CardDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            CardDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onAddClick,
                        indication = ripple(),
                        interactionSource = remember { MutableInteractionSource() },
                    )
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.settings_add_shortcut),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** 快捷仓库添加/编辑表单。 */
@Composable
private fun ShortcutForm(
    name: String,
    repo: String,
    editing: String?,
    onNameChange: (String) -> Unit,
    onRepoChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.settings_shortcut_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = repo,
                onValueChange = onRepoChange,
                label = { Text(stringResource(R.string.settings_shortcut_repo)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMedium),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    label = if (editing != null) stringResource(R.string.settings_shortcut_update)
                    else stringResource(R.string.settings_add_shortcut),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
                if (editing != null) {
                    TextButton(onClick = onCancelEdit) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    }
}

/** 关于卡片:版本 / 上游 CLI / 开源许可(设计稿 设置.html)。 */
@Composable
private fun AboutCard() {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            AboutRow(
                label = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                trailing = null,
            )
            CardDivider()
            AboutRow(
                label = stringResource(R.string.settings_upstream),
                trailing = { OpenLinkIcon() },
            )
            CardDivider()
            AboutRow(
                label = stringResource(R.string.settings_license),
                trailing = { OpenLinkIcon() },
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, trailing: (@Composable () -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
        trailing?.invoke()
    }
}

@Composable
private fun OpenLinkIcon() {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
    )
}

/** 主色实底按钮(圆角 8px,设计稿 .oci-btn--primary 缩略版)。 */
@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val background = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(RadiusMedium)
    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
