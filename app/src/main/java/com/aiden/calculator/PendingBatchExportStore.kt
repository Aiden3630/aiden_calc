package com.aiden.calculator

import android.content.Context
import android.net.Uri

data class PendingBatchExportState(
    val vaultId: VaultId,
    val itemIds: Set<String>,
    val uri: Uri?,
)

class PendingBatchExportStore(context: Context) {
    private val preferences = context.getSharedPreferences("pending_batch_export", Context.MODE_PRIVATE)

    fun save(vaultId: VaultId?, itemIds: Collection<String>, uri: Uri?) {
        preferences.edit()
            .putString("vaultId", vaultId?.name)
            .putStringSet("itemIds", itemIds.toSet())
            .apply {
                if (uri == null) remove("uri") else putString("uri", uri.toString())
            }
            .apply()
    }

    fun restore(): PendingBatchExportState? {
        val vaultId = runCatching {
            preferences.getString("vaultId", null)?.let(VaultId::valueOf)
        }.getOrNull() ?: return null
        val itemIds = preferences.getStringSet("itemIds", emptySet()).orEmpty()
        if (itemIds.isEmpty()) return null
        return PendingBatchExportState(
            vaultId = vaultId,
            itemIds = itemIds,
            uri = preferences.getString("uri", null)?.let(Uri::parse),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
