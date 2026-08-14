package com.tiramission.ocisync.ui.push

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiramission.ocisync.OciSyncApp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.core.model.Stage
import com.tiramission.ocisync.ui.components.CardDivider
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.RadiusLarge
import com.tiramission.ocisync.ui.components.RadiusMedium
import com.tiramission.ocisync.ui.components.SectionTitle

/**
 * 推送页(ui-design/ 推送.html):
 * 卡片分区(本地路径/仓库引用/加密口令/标签)+ 底部固定操作区(开始推送 + 进度条)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreen(
    initialRef: String = "",
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as OciSyncApp
    val viewModel: PushViewModel = viewModel(factory = PushViewModel.Factory(app.container.syncService, context))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialRef) {
        if (initialRef.isNotBlank()) viewModel.onRemoteRefChange(initialRef)
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFilePicked(it) }
    }
    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onDirectoryPicked(it) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val stageLabel = when (state.stage) {
        Stage.PACKING -> stringResource(R.string.stage_packing)
        Stage.ENCRYPTING -> stringResource(R.string.stage_encrypting)
        Stage.UPLOADING -> stringResource(R.string.stage_uploading)
        Stage.DONE -> stringResource(R.string.stage_done)
        else -> ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.push_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
        bottomBar = {
            PushActionArea(
                isRunning = state.isRunning,
                progress = state.progress,
                stageLabel = stageLabel,
                enabled = state.selectedFile != null && state.remoteRef.isNotBlank(),
                onStart = viewModel::startPush,
                onCancel = viewModel::cancel,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 本地路径 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle(text = stringResource(R.string.push_local_path))
                    Spacer(modifier = Modifier.height(12.dp))
                    TonalButton(
                        label = stringResource(R.string.push_select_file_dir),
                        icon = Icons.Filled.FolderOpen,
                        enabled = !state.isRunning,
                        onClick = {
                            // 设计稿为单一入口;保留文件/目录选择弹窗由系统决定
                            fileLauncher.launch(arrayOf("*/*"))
                        },
                    )
                    state.selectedName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // ── 仓库引用 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = state.remoteRef,
                        onValueChange = viewModel::onRemoteRefChange,
                        label = { Text(stringResource(R.string.push_ref_hint)) },
                        enabled = !state.isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMedium),
                    )
                }
            }

            // ── 加密口令 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = state.passphrase,
                        onValueChange = viewModel::onPassphraseChange,
                        label = { Text(stringResource(R.string.push_passphrase)) },
                        enabled = !state.isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMedium),
                        visualTransformation = if (state.showPassphrase) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = viewModel::togglePassphrase) {
                                Text(
                                    if (state.showPassphrase) stringResource(R.string.common_hide)
                                    else stringResource(R.string.common_show)
                                )
                            }
                        },
                    )
                }
            }

            // ── 标签 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle(text = stringResource(R.string.push_labels))
                    if (state.labels.isNotEmpty()) {
                        state.labels.forEachIndexed { index, (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$k=$v",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { viewModel.removeLabel(index) },
                                    enabled = !state.isRunning,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            if (index < state.labels.lastIndex) CardDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TonalButton(
                        label = stringResource(R.string.push_add_label),
                        icon = Icons.Filled.Add,
                        enabled = !state.isRunning,
                        onClick = viewModel::addLabel,
                    )
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** 设计稿 .oci-btn--tonal:浅色底 + 边框 + 主色图标/文字。 */
@Composable
fun TonalButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(RadiusMedium)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 底部固定操作区:开始推送(主色大按钮)+ 进度条 + 状态(设计稿 .action-area)。 */
@Composable
private fun PushActionArea(
    isRunning: Boolean,
    progress: Float,
    stageLabel: String,
    enabled: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(16.dp),
    ) {
        if (isRunning) {
            PrimaryActionButton(
                label = stringResource(R.string.push_cancel),
                onClick = onCancel,
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            PrimaryActionButton(
                label = stringResource(R.string.push_start),
                icon = Icons.Filled.CloudUpload,
                onClick = onStart,
                enabled = enabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        // 进度条
        ProgressBar(progress = if (isRunning) progress else 0f)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRunning) stageLabel else stringResource(R.string.progress_waiting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 主色大按钮(圆角 16dp,设计稿 .oci-btn--primary)。 */
@Composable
fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
) {
    val background = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(RadiusLarge)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 进度条(设计稿 .progress-track/.progress-fill):6dp 高,圆角全,主色填充。 */
@Composable
fun ProgressBar(progress: Float) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(9999.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(fillColor),
        )
    }
}
