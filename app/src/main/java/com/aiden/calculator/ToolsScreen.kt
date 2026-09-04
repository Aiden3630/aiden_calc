package com.aiden.calculator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class Tool(
    val title: Int,
    val hint: Int,
    val icon: ImageVector,
    val tint: Color,
    val action: () -> Unit,
)

@Composable
internal fun ToolsScreen(
    modifier: Modifier = Modifier,
    browser: () -> Unit,
    cloud: () -> Unit,
    accent: () -> Unit,
    icons: () -> Unit,
    decoy: () -> Unit,
    trash: () -> Unit,
) {
    val tools = listOf(
        Tool(R.string.personal_browser, R.string.tool_browser_hint, Icons.Default.Web, Color(0xFF78B99E), browser),
        Tool(R.string.cloud_file_protection, R.string.tool_cloud_hint, Icons.Default.Cloud, Color(0xFF85AEDB), cloud),
        Tool(R.string.theme_color, R.string.tool_accent_hint, Icons.Default.ColorLens, Color(0xFFB59CE2), accent),
        Tool(R.string.app_icon, R.string.tool_icon_hint, Icons.Default.Security, Color(0xFFE0B477), icons),
        Tool(R.string.decoy_vault, R.string.tool_decoy_hint, Icons.Default.Lock, Color(0xFFB1B8B5), decoy),
        Tool(R.string.trash, R.string.tool_trash_hint, Icons.Default.Delete, Color(0xFFD99090), trash),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(tools) { ToolTile(it) }
    }
}

@Composable
private fun ToolTile(tool: Tool) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tool-scale")
    Card(
        modifier = Modifier.height(154.dp).scale(scale).clickable(interactionSource = interactionSource, indication = null, onClick = tool.action),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(46.dp).background(tool.tint.copy(alpha = 0.18f), RoundedCornerShape(16.dp)).padding(9.dp)) {
                Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.fillMaxSize())
            }
            Column {
                Text(stringResource(tool.title), fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(tool.hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
