package com.tiramission.ocisync.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tiramission.ocisync.core.config.Shortcut
import com.tiramission.ocisync.ui.components.IconTile
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.RadiusLarge
import com.tiramission.ocisync.ui.components.SectionTitle
import com.tiramission.ocisync.ui.theme.OciSyncTheme

/**
 * 首页(ui-design/ 首页.html):
 * 1) 顶部 header:左对齐标题 "OCI Sync" + 设置齿轮
 * 2) 快捷仓库区:卡片(图标块 + 名称 + repo + chevron)
 * 3) 快速操作区:两个大按钮(推送文件/拉取文件,主色实底 + 图标 + 文字)
 */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenPush: () -> Unit,
    onOpenPull: () -> Unit,
    onOpenShortcut: (name: String, repo: String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as OciSyncApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.container.configLoader))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── header:左对齐标题 + 设置(设计稿:sticky header, border-b)──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── 快捷仓库区 ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_shortcuts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.home_add_shortcut),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (uiState.shortcuts.isEmpty()) {
                        OciCard {
                            Text(
                                text = stringResource(R.string.home_no_shortcuts),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            uiState.shortcuts.forEach { (name, shortcut) ->
                                ShortcutCard(
                                    name = name,
                                    shortcut = shortcut,
                                    onClick = { onOpenShortcut(name, shortcut.repo) },
                                )
                            }
                        }
                    }
                }
            }

            // ── 快速操作区 ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.home_quick_actions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QuickActionButton(
                            label = stringResource(R.string.home_push),
                            icon = Icons.Filled.Upload,
                            onClick = onOpenPush,
                            modifier = Modifier.weight(1f),
                        )
                        QuickActionButton(
                            label = stringResource(R.string.home_pull),
                            icon = Icons.Filled.Download,
                            onClick = onOpenPull,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

/** 快捷仓库卡片:图标块 + 名称 + repo(mono)+ chevron(设计稿 首页.html)。 */
@Composable
private fun ShortcutCard(
    name: String,
    shortcut: Shortcut,
    onClick: () -> Unit,
) {
    OciCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = Icons.Filled.Work,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = shortcut.repo,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 快速操作大按钮:主色实底 + 圆角 16dp + 图标 + 文字(纵向,设计稿 首页.html)。 */
@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusLarge))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    OciSyncTheme {
        HomeScreen(onOpenSettings = {}, onOpenPush = {}, onOpenPull = {}, onOpenShortcut = { _, _ -> })
    }
}
