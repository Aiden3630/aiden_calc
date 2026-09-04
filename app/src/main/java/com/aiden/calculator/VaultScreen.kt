package com.aiden.calculator

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class VaultNestedScreen {
    TRASH,
    REPAIR,
    RECOVERY,
    DECOY,
    PRIVACY_POLICY,
    BROWSER,
    CLOUD,
    WIFI_TRANSFER,
    FILE_TRANSFER,
    SHARE,
    LANGUAGE,
    ABOUT,
}

internal enum class VaultTab(val label: Int, val icon: ImageVector, val contentTab: VaultContentTab? = null) {
    PHOTOS(R.string.photos, Icons.Default.Photo, VaultContentTab.PHOTOS),
    VIDEOS(R.string.videos, Icons.Default.Videocam, VaultContentTab.VIDEOS),
    FILES(R.string.files, Icons.Default.Description, VaultContentTab.FILES),
    TOOLS(R.string.tools, Icons.Default.Build),
    SETTINGS(R.string.settings, Icons.Default.Settings),
}

internal fun VaultContentTab.toVaultTab() = when (this) {
    VaultContentTab.PHOTOS -> VaultTab.PHOTOS
    VaultContentTab.VIDEOS -> VaultTab.VIDEOS
    VaultContentTab.FILES -> VaultTab.FILES
}

internal fun vaultTabTransition(initial: VaultTab, target: VaultTab): ContentTransform {
    val direction = vaultTabSlideDirection(initial.ordinal, target.ordinal)
    return (slideInHorizontally(tween(220)) { -direction * it / 4 } + fadeIn(tween(220))) togetherWith
        (slideOutHorizontally(tween(220)) { direction * it / 4 } + fadeOut(tween(220)))
}

internal fun vaultTabSlideDirection(initialIndex: Int, targetIndex: Int) =
    if (targetIndex > initialIndex) -1 else 1

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun VaultTopBar(vaultId: VaultId, title: String = stringResource(R.string.vault_title), back: (() -> Unit)? = null) {
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (vaultId == VaultId.ONE) R.string.primary_vault else R.string.decoy_vault),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        navigationIcon = {
            if (back != null) IconButton(back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
    )
}

@Composable
internal fun VaultBottomNavigation(selected: VaultTab, select: (VaultTab) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VaultTab.entries.forEach { tab ->
                val active = selected == tab
                val scale by animateFloatAsState(if (active) 1.04f else 1f, tween(150), label = "tab-scale")
                val background by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    tween(180),
                    label = "tab-bg",
                )
                val content by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(180),
                    label = "tab-content",
                )
                Column(
                    Modifier.weight(1f)
                        .height(58.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(18.dp))
                        .background(background, RoundedCornerShape(18.dp))
                        .clickable { select(tab) }
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                    Text(
                        stringResource(tab.label),
                        color = content,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        Modifier.padding(top = 4.dp)
                            .width(if (active) 18.dp else 4.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (active) content else background),
                    )
                }
            }
        }
    }
}
