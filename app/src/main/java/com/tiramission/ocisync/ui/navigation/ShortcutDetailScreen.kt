package com.tiramission.ocisync.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tiramission.ocisync.R
import com.tiramission.ocisync.ui.components.OciCard
import com.tiramission.ocisync.ui.components.SectionTitle
import com.tiramission.ocisync.ui.list.ListContent
import com.tiramission.ocisync.ui.push.PrimaryActionButton

/**
 * 快捷仓库操作台(ui-design/ 快捷仓库.html):
 * 身份卡片(名称 + repo + 编辑)+ TAG 列表(复用 ListContent)+ 底部「推送新版本」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutDetailScreen(
    name: String,
    repo: String,
    onBack: () -> Unit,
    onPullArtifact: (String) -> Unit,
    onPushNew: () -> Unit,
    onEditShortcut: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shortcut_title),
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
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                PrimaryActionButton(
                    label = stringResource(R.string.shortcut_push_new),
                    icon = Icons.Filled.Upload,
                    onClick = onPushNew,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // ── 身份卡片 ──
            OciCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = repo,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onEditShortcut) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.settings_edit),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(text = stringResource(R.string.shortcut_tags_title))
            Spacer(modifier = Modifier.height(12.dp))

            // ── TAG 列表(查询 + artifact 卡片)──
            ListContent(
                initialRef = repo,
                onPullArtifact = onPullArtifact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
