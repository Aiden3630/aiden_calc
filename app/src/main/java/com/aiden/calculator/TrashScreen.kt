package com.aiden.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrashScreen(
    items: List<VaultItem>,
    topBar: @Composable () -> Unit,
    restore: (List<String>) -> Unit,
    deleteForever: (List<String>) -> Unit,
    itemName: @Composable (VaultItem) -> Unit,
    itemSize: @Composable (VaultItem) -> Unit,
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var pendingDeleteIds by remember { mutableStateOf(emptyList<String>()) }
    val itemIds = remember(items) { items.map { it.id }.toSet() }

    LaunchedEffect(itemIds) {
        selectedIds = selectedIds.filterTo(mutableSetOf()) { it in itemIds }
    }

    Scaffold(topBar = topBar) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (items.isNotEmpty()) item {
                val allSelected = itemIds.isNotEmpty() && itemIds.all { it in selectedIds }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton({ selectedIds = if (allSelected) emptySet() else itemIds }) {
                        Text(stringResource(if (allSelected) R.string.clear_selection else R.string.select_all))
                    }
                    if (selectedIds.isNotEmpty()) {
                        Text(stringResource(R.string.selected_count, selectedIds.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (selectedIds.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton({
                            restore(selectedIds.toList())
                            selectedIds = emptySet()
                        }) { Text("${stringResource(R.string.restore)} (${selectedIds.size})") }
                        TextButton({ pendingDeleteIds = selectedIds.toList() }) {
                            Text("${stringResource(R.string.delete_forever)} (${selectedIds.size})")
                        }
                    }
                }
            }
            if (items.isEmpty()) item {
                Text(stringResource(R.string.empty_trash), modifier = Modifier.padding(top = 28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(items, key = { it.id }) { item ->
                val checked = item.id in selectedIds
                fun toggleSelection() {
                    selectedIds = if (checked) selectedIds - item.id else selectedIds + item.id
                }
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).combinedClickable(
                        onClick = { if (selectedIds.isNotEmpty()) toggleSelection() },
                        onLongClick = ::toggleSelection,
                    ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        if (selectedIds.isNotEmpty()) {
                            Checkbox(checked, onCheckedChange = { toggleSelection() })
                            Spacer(Modifier.size(8.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            itemName(item)
                            Text(item.type.name.lowercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            itemSize(item)
                            Row {
                                TextButton({ restore(listOf(item.id)) }) { Text(stringResource(R.string.restore)) }
                                TextButton({ pendingDeleteIds = listOf(item.id) }) { Text(stringResource(R.string.delete_forever)) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pendingDeleteIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = emptyList() },
            text = {
                Text(
                    if (pendingDeleteIds.size == 1) {
                        stringResource(R.string.confirm_delete_forever)
                    } else {
                        stringResource(R.string.confirm_delete_selected_forever, pendingDeleteIds.size)
                    },
                )
            },
            dismissButton = { TextButton({ pendingDeleteIds = emptyList() }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {
                TextButton({
                    deleteForever(pendingDeleteIds)
                    selectedIds = selectedIds - pendingDeleteIds.toSet()
                    pendingDeleteIds = emptyList()
                }) { Text(stringResource(R.string.delete_forever)) }
            },
        )
    }
}
