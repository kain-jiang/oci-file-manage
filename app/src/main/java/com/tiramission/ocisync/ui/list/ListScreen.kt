package com.tiramission.ocisync.ui.list

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiramission.ocisync.OciSyncApp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.core.model.ArtifactInfo
import com.tiramission.ocisync.ui.components.FilterChipOci
import com.tiramission.ocisync.ui.components.IconTile
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.RadiusMedium
import java.util.Locale

/** 仓库浏览页外壳(带标题栏),ui-design/ 仓库.html。 */
@Composable
fun ListScreen(
    initialRef: String? = null,
    onPullArtifact: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 设计稿标题栏:左对齐大标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.browse_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        ListContent(
            initialRef = initialRef,
            onPullArtifact = onPullArtifact,
        )
    }
}

/** 仓库浏览内容层:搜索栏 + 筛选 chips + artifact 卡片列表(设计稿 仓库.html)。 */
@Composable
fun ListContent(
    initialRef: String? = null,
    onPullArtifact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as OciSyncApp
    val viewModel: ListViewModel = viewModel(factory = ListViewModel.Factory(app.container.syncService))
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialRef) {
        if (initialRef != null) {
            viewModel.onRefChange(initialRef)
            viewModel.search()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── 搜索栏:输入框(带搜索图标)+ 查询按钮(设计稿 仓库.html)──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                value = state.ref,
                onValueChange = viewModel::onRefChange,
                placeholder = stringResource(R.string.browse_hint),
                modifier = Modifier.weight(1f),
            )
            QueryButton(onClick = viewModel::search)
        }

        // ── 筛选 chips(全部 + 结果标签)──
        if (state.filterChips.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChipOci(
                        label = stringResource(R.string.history_filter_all),
                        selected = state.activeFilter == null,
                        onClick = { viewModel.setFilter(null) },
                    )
                }
                items(state.filterChips) { chip ->
                    FilterChipOci(
                        label = chip,
                        selected = state.activeFilter == chip,
                        onClick = { viewModel.setFilter(chip) },
                    )
                }
            }
        }

        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        // ── 结果区 ──
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.artifacts.isEmpty() && state.error.isNullOrEmpty() -> Text(
                    text = stringResource(R.string.browse_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.artifacts, key = { it.digest }) { artifact ->
                        ArtifactCard(
                            artifact = artifact,
                            onPull = { onPullArtifact(artifact.fullName) },
                            onDelete = { viewModel.requestDelete(artifact) },
                            onLabels = { viewModel.openLabelDialog(artifact) },
                        )
                    }
                }
            }
        }
    }

    // 删除确认
    state.deletingRef?.let { ref ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.common_confirm_delete_title)) },
            text = { Text(ref) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    // 标签管理弹窗
    state.labelingArtifact?.let { artifact ->
        LabelDialog(
            artifact = artifact,
            onDismiss = viewModel::dismissLabelDialog,
            onApply = { updates, removeKeys -> viewModel.applyLabels(artifact, updates, removeKeys) },
        )
    }
}

/** 设计稿搜索框:白底 + 圆角 8px + 边框 + 左侧搜索图标(仓库.html)。 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(RadiusMedium))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
    }
}

/** 设计稿「查询」按钮:主色实底 + 圆角 8px(仓库.html)。 */
@Composable
private fun QueryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(RadiusMedium))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.browse_search),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** artifact 卡片:图标块 + 名称+lock + 标签chip + 大小/版本 + 三按钮行(设计稿 仓库.html)。 */
@Composable
private fun ArtifactCard(
    artifact: ArtifactInfo,
    onPull: () -> Unit,
    onDelete: () -> Unit,
    onLabels: () -> Unit,
) {
    OciCard {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconTile(
                    icon = Icons.Filled.Inventory,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = artifact.repo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (artifact.encrypted) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = stringResource(R.string.list_encrypted),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Text(
                        text = "tag: ${artifact.tag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 第一个标签 chip(设计稿右上角,primary-50 底 + primary 字)
                artifact.labels.entries.firstOrNull()?.let { (k, v) ->
                    LabelChip("$k=$v")
                }
            }

            // 大小 + 版本(设计稿:hard-drive + tag 图标行)
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaItem(Icons.Filled.Storage, formatSize(artifact.size))
                MetaItem(Icons.Filled.Tag, "v${artifact.version}")
            }

            // 行操作:拉取(主色实底)/ 删除 / 标签(设计稿三按钮等宽)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArtifactActionButton(
                    label = stringResource(R.string.list_action_pull),
                    primary = true,
                    onClick = onPull,
                    modifier = Modifier.weight(1f),
                )
                ArtifactActionButton(
                    label = stringResource(R.string.list_action_delete),
                    primary = false,
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                )
                ArtifactActionButton(
                    label = stringResource(R.string.list_action_labels),
                    primary = false,
                    onClick = onLabels,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 标签 chip(设计稿:primary-50 底 + primary 字,圆角全,mono 字体)。 */
@Composable
private fun LabelChip(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** 行操作按钮:主色实底 / 浅色底+边框(设计稿 仓库.html 行按钮)。 */
@Composable
private fun ArtifactActionButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(RadiusMedium)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(
                width = if (primary) 0.dp else 1.dp,
                color = if (primary) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

@Composable
private fun LabelDialog(
    artifact: ArtifactInfo,
    onDismiss: () -> Unit,
    onApply: (Map<String, String>, List<String>) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var removeKeys by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_label_dialog_title, artifact.tag)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.list_label_dialog_current),
                    style = MaterialTheme.typography.bodySmall,
                )
                artifact.labels.forEach { (k, v) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("$k=$v", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { removeKeys = removeKeys + k }) {
                            Text(stringResource(R.string.list_label_remove), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                androidx.compose.material3.OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.push_label_key)) },
                    singleLine = true,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.push_label_value)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updates = if (key.isNotBlank()) mapOf(key.trim() to value) else emptyMap()
                onApply(updates, removeKeys)
            }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.1fGB", bytes.toDouble() / (1L shl 30))
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1fMB", bytes.toDouble() / (1L shl 20))
    bytes >= 1L shl 10 -> String.format(Locale.US, "%.1fKB", bytes.toDouble() / (1L shl 10))
    else -> "$bytes B"
}
