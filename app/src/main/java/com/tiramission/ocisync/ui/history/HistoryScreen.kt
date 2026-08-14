package com.tiramission.ocisync.ui.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tiramission.ocisync.OciSyncApp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.core.cache.Activity
import com.tiramission.ocisync.core.cache.ActivityType
import com.tiramission.ocisync.ui.components.FilterChipOci
import com.tiramission.ocisync.ui.components.IconCircle
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.theme.ErrorContainerLight
import com.tiramission.ocisync.ui.theme.ErrorLight
import com.tiramission.ocisync.ui.theme.SuccessContainerLight
import com.tiramission.ocisync.ui.theme.SuccessLight
import com.tiramission.ocisync.ui.theme.WarningContainerLight
import com.tiramission.ocisync.ui.theme.WarningLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史页(ui-design/ 历史.html):
 * 顶部标题 + 清空,筛选 chips(全部/推送/拉取/删除/标签),
 * 活动卡片:圆形类型图标 + ref + 成功/失败状态 + 类型·时间。
 */
@Composable
fun HistoryScreen() {
    val app = LocalContext.current.applicationContext as OciSyncApp
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(app.container.activityStore))
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 设计稿:标题 + 清空(text 按钮)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = { showClearConfirm = true }) {
                Text(
                    text = stringResource(R.string.history_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // 筛选 chips(设计稿:全部=主色实底,其他=白底边框)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChipOci(
                    label = stringResource(R.string.history_filter_all),
                    selected = uiState.filter == null,
                    onClick = { viewModel.setFilter(null) },
                )
            }
            items(ActivityType.entries) { type ->
                val label = when (type) {
                    ActivityType.PUSH -> stringResource(R.string.history_type_push)
                    ActivityType.PULL -> stringResource(R.string.history_type_pull)
                    ActivityType.DELETE -> stringResource(R.string.history_type_delete)
                    ActivityType.LABEL -> stringResource(R.string.history_type_label)
                }
                FilterChipOci(
                    label = label,
                    selected = uiState.filter == type,
                    onClick = { viewModel.setFilter(type) },
                )
            }
        }

        if (uiState.activities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.activities, key = { "${it.timestamp}-${it.remoteRef}" }) { activity ->
                    HistoryCard(activity)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    showClearConfirm = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

/** 活动卡片:圆形类型图标 + ref + 状态 + 类型·时间(设计稿 历史.html)。 */
@Composable
private fun HistoryCard(activity: Activity) {
    val (icon, container, content) = activityVisual(activity.type)
    val typeLabel = when (activity.type) {
        ActivityType.PUSH -> stringResource(R.string.history_type_push)
        ActivityType.PULL -> stringResource(R.string.history_type_pull)
        ActivityType.DELETE -> stringResource(R.string.history_type_delete)
        ActivityType.LABEL -> stringResource(R.string.history_type_label)
    }
    val time = remember(activity.timestamp) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(activity.timestamp))
    }

    OciCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(
                icon = icon,
                containerColor = container,
                contentColor = content,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = activity.remoteRef,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (activity.success) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircleOutline,
                                contentDescription = null,
                                tint = SuccessLight,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.common_success),
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessLight,
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorLight,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.common_failed),
                                style = MaterialTheme.typography.labelMedium,
                                color = ErrorLight,
                            )
                        }
                    }
                }
                activity.error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                CircleShape,
                            ),
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 活动类型 → 图标/容器色/内容色(设计稿:推送/拉取=primary,删除=error,标签=warning)。 */
private fun activityVisual(type: ActivityType): Triple<ImageVector, Color, Color> = when (type) {
    ActivityType.PUSH -> Triple(Icons.Filled.ArrowUpward, SuccessContainerLight, SuccessLight)
    ActivityType.PULL -> Triple(Icons.Filled.ArrowDownward, SuccessContainerLight, SuccessLight)
    ActivityType.DELETE -> Triple(Icons.Filled.Delete, ErrorContainerLight, ErrorLight)
    ActivityType.LABEL -> Triple(Icons.Filled.Label, WarningContainerLight, WarningLight)
}
