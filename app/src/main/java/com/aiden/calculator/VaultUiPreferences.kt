package com.aiden.calculator

import android.content.Context

enum class VaultContentTab { PHOTOS, VIDEOS, FILES }

data class TabScrollPosition(val index: Int = 0, val offset: Int = 0)

interface VaultTabMemory {
    fun lastContentTab(vaultId: VaultId): VaultContentTab
    fun saveLastContentTab(vaultId: VaultId, tab: VaultContentTab)
}

class VaultUiPreferences(context: Context) : VaultTabMemory {
    private val preferences = context.getSharedPreferences("vault_ui", Context.MODE_PRIVATE)

    override fun lastContentTab(vaultId: VaultId): VaultContentTab = runCatching {
        VaultContentTab.valueOf(preferences.getString("${vaultId.name}.lastContentTab", null) ?: "")
    }.getOrDefault(VaultContentTab.PHOTOS)

    override fun saveLastContentTab(vaultId: VaultId, tab: VaultContentTab) {
        preferences.edit().putString("${vaultId.name}.lastContentTab", tab.name).apply()
    }
}

class VaultNavigationState(private val memory: VaultTabMemory) {
    private val scrollPositions = mutableMapOf<Pair<VaultId, VaultContentTab>, TabScrollPosition>()
    private var sessionActive = false

    fun beginSession() {
        sessionActive = true
    }

    fun lastContentTab(vaultId: VaultId) = memory.lastContentTab(vaultId)

    fun select(vaultId: VaultId, tab: VaultContentTab) {
        memory.saveLastContentTab(vaultId, tab)
    }

    fun scrollPosition(vaultId: VaultId, tab: VaultContentTab) =
        scrollPositions[vaultId to tab] ?: TabScrollPosition()

    fun saveScrollPosition(vaultId: VaultId, tab: VaultContentTab, position: TabScrollPosition) {
        if (sessionActive) scrollPositions[vaultId to tab] = position
    }

    fun clearScrollPositions() {
        sessionActive = false
        scrollPositions.clear()
    }
}
