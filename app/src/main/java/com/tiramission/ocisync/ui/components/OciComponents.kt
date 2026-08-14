package com.tiramission.ocisync.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 设计稿圆角 tokens */
val RadiusSmall = 4.dp
val RadiusMedium = 8.dp
val RadiusLarge = 16.dp
val RadiusFull = 9999.dp

/**
 * 设计稿卡片:白底 + 1px 边框(--oci-line)+ 16dp 圆角 + 轻阴影(--oci-shadow-1)。
 * 对应 ui-design/ 目录下 HTML 的 .rounded-2xl bg-card border border-line shadow-sm。
 */
@Composable
fun OciCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(RadiusLarge)
    var base = Modifier
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
    if (onClick != null) {
        base = base.clickable(
            onClick = onClick,
            indication = ripple(),
            interactionSource = remember { MutableInteractionSource() },
        )
    }
    Box(modifier = modifier.then(base)) {
        content()
    }
}

/** 设计稿「图标块」:40x40,圆角 12px,浅主色底 + 主色图标(首页快捷仓库/仓库页 artifact 行)。 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    corner: Dp = 12.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** 设计稿「圆形图标」:历史页活动类型 44x44 圆形。 */
@Composable
fun IconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 设计稿「筛选 chip」:选中=实色主底;未选=白底 + 边框 + 次要文字。 */
@Composable
fun FilterChipOci(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(RadiusFull)
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .clickable(
                onClick = onClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

/** 设计稿「区块标题」:小节标题(快捷仓库/快速操作/TAG 列表等)。 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        trailing?.invoke()
    }
}

/** 设计稿「卡片内分隔线」:1px 高,边框色。 */
@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}
