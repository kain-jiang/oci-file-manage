package com.tiramission.ocisync.ui.pull

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiramission.ocisync.OciSyncApp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.core.model.Stage
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.RadiusMedium
import com.tiramission.ocisync.ui.components.SectionTitle
import com.tiramission.ocisync.ui.push.PrimaryActionButton
import com.tiramission.ocisync.ui.push.ProgressBar
import com.tiramission.ocisync.ui.push.TonalButton

/**
 * 拉取页(ui-design/ 拉取.html):
 * 卡片分区(仓库引用/目标目录/解密口令)+ 底部固定操作区(开始拉取 + 进度条 + 百分比)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullScreen(
    initialRef: String = "",
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as OciSyncApp
    val viewModel: PullViewModel = viewModel(factory = PullViewModel.Factory(app.container.syncService, context))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialRef) {
        if (initialRef.isNotBlank()) viewModel.onRemoteRefChange(initialRef)
    }

    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.onDestPicked(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val stageLabel = when (state.stage) {
        Stage.DOWNLOADING -> stringResource(R.string.stage_downloading)
        Stage.DECRYPTING -> stringResource(R.string.stage_decrypting)
        Stage.UNPACKING -> stringResource(R.string.stage_unpacking)
        Stage.DONE -> stringResource(R.string.stage_done)
        else -> ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pull_title),
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
            PullActionArea(
                isRunning = state.isRunning,
                progress = state.progress,
                stageLabel = stageLabel,
                enabled = state.destTreeUri != null && state.remoteRef.isNotBlank(),
                onStart = viewModel::startPull,
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
            // ── 仓库引用 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle(text = stringResource(R.string.pull_ref_title))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.remoteRef,
                        onValueChange = viewModel::onRemoteRefChange,
                        label = { Text(stringResource(R.string.pull_ref_hint)) },
                        enabled = !state.isRunning,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMedium),
                    )
                }
            }

            // ── 目标目录 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle(text = stringResource(R.string.pull_dest_title))
                    Spacer(modifier = Modifier.height(12.dp))
                    TonalButton(
                        label = stringResource(R.string.pull_select_dest),
                        icon = Icons.Filled.Folder,
                        enabled = !state.isRunning,
                        onClick = { dirLauncher.launch(null) },
                    )
                    state.destName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // ── 解密口令 ──
            OciCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = state.passphrase,
                        onValueChange = viewModel::onPassphraseChange,
                        label = { Text(stringResource(R.string.pull_passphrase)) },
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
                    Text(
                        text = stringResource(R.string.pull_passphrase_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
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

/** 底部固定操作区:开始拉取 + 进度条 + 百分比(设计稿 拉取.html 底部 section)。 */
@Composable
private fun PullActionArea(
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
            .padding(16.dp),
    ) {
        if (isRunning) {
            PrimaryActionButton(
                label = stringResource(R.string.push_cancel),
                onClick = onCancel,
            )
        } else {
            PrimaryActionButton(
                label = stringResource(R.string.pull_start),
                icon = Icons.Filled.CloudDownload,
                onClick = onStart,
                enabled = enabled,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ProgressBar(progress = if (isRunning) progress else 0f)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (isRunning) stageLabel else stringResource(R.string.progress_waiting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
