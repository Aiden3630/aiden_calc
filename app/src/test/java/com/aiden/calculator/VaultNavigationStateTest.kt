package com.aiden.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class VaultNavigationStateTest {
    private val memory = FakeVaultTabMemory()
    private val navigation = VaultNavigationState(memory)

    @Test fun `each vault remembers its own content tab`() {
        navigation.select(VaultId.ONE, VaultContentTab.VIDEOS)
        navigation.select(VaultId.TWO, VaultContentTab.FILES)

        assertEquals(VaultContentTab.VIDEOS, navigation.lastContentTab(VaultId.ONE))
        assertEquals(VaultContentTab.FILES, navigation.lastContentTab(VaultId.TWO))
    }

    @Test fun `scroll positions stay in memory until cleared`() {
        navigation.beginSession()
        navigation.saveScrollPosition(VaultId.ONE, VaultContentTab.PHOTOS, TabScrollPosition(12, 48))
        assertEquals(TabScrollPosition(12, 48), navigation.scrollPosition(VaultId.ONE, VaultContentTab.PHOTOS))

        navigation.clearScrollPositions()
        assertEquals(TabScrollPosition(), navigation.scrollPosition(VaultId.ONE, VaultContentTab.PHOTOS))
    }

    @Test fun `late scroll save after lock is ignored`() {
        navigation.beginSession()
        navigation.clearScrollPositions()
        navigation.saveScrollPosition(VaultId.ONE, VaultContentTab.PHOTOS, TabScrollPosition(3, 20))

        assertEquals(TabScrollPosition(), navigation.scrollPosition(VaultId.ONE, VaultContentTab.PHOTOS))
    }

    @Test fun `slide direction follows tab order`() {
        assertEquals(-1, vaultTabSlideDirection(0, 2))
        assertEquals(1, vaultTabSlideDirection(2, 0))
    }

    private class FakeVaultTabMemory : VaultTabMemory {
        private val tabs = mutableMapOf<VaultId, VaultContentTab>()

        override fun lastContentTab(vaultId: VaultId) = tabs[vaultId] ?: VaultContentTab.PHOTOS

        override fun saveLastContentTab(vaultId: VaultId, tab: VaultContentTab) {
            tabs[vaultId] = tab
        }
    }
}
