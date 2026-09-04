package com.aiden.calculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingSystemActionTest {
    @Test fun `system picker preserves session only before deadline`() {
        val action = PendingSystemAction(SystemActionType.IMPORT, VaultId.ONE, expiresAtElapsed = 60_000)

        assertTrue(action.isValid(59_999))
        assertFalse(action.isValid(60_000))
        assertFalse(action.isValid(60_001))
    }

    @Test fun `ordinary lifecycle stop has no system picker window`() {
        val action: PendingSystemAction? = null

        assertFalse(action?.isValid(0) == true)
    }

    @Test fun `coordinator expiration clears key independently of activity lifecycle`() {
        var now = 100L
        var expiration: (() -> Unit)? = null
        val session = VaultSession().apply { unlock(VaultId.ONE, ByteArray(32) { 7 }) }
        val coordinator = VaultSessionCoordinator(
            session,
            ElapsedRealtimeClock { now },
            scheduleExpiration = { _, callback -> expiration = callback },
        )

        coordinator.beginSystemAction(SystemActionType.IMPORT)
        now += VaultSessionCoordinator.SYSTEM_ACTION_WINDOW_MS
        expiration!!.invoke()

        assertTrue(session.vaultId == null)
    }

    @Test fun `wall clock changes do not affect transient window`() {
        var elapsed = 500L
        val session = VaultSession().apply { unlock(VaultId.ONE, ByteArray(32) { 3 }) }
        val coordinator = VaultSessionCoordinator(session, ElapsedRealtimeClock { elapsed }, { _, _ -> })
        val action = coordinator.beginSystemAction(SystemActionType.EXPORT)

        @Suppress("UNUSED_VARIABLE")
        val unrelatedWallClock = Long.MAX_VALUE
        elapsed += VaultSessionCoordinator.SYSTEM_ACTION_WINDOW_MS - 1

        assertTrue(coordinator.isValid(action))
    }
}
