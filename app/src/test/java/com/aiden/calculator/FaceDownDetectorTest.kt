package com.aiden.calculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceDownDetectorTest {
    private var now = 0L
    private val detector = FaceDownDetector(now = { now })

    @Test fun `stable face down triggers after debounce`() {
        assertFalse(detector.update(0f, 0f, -9.8f))
        now = 399
        assertFalse(detector.update(0f, 0f, -9.8f))
        now = 400
        assertTrue(detector.update(0f, 0f, -9.8f))
        now = 800
        assertFalse(detector.update(0f, 0f, -9.8f))
    }

    @Test fun `ordinary rotation does not trigger`() {
        assertFalse(detector.update(0f, 9.8f, 0f))
        now = 500
        assertFalse(detector.update(0f, 9.8f, 0f))
    }

    @Test fun `movement resets face down debounce`() {
        assertFalse(detector.update(0f, 0f, -9.8f))
        now = 300
        assertFalse(detector.update(12f, 0f, -9.8f))
        now = 700
        assertFalse(detector.update(0f, 0f, -9.8f))
        now = 1_100
        assertTrue(detector.update(0f, 0f, -9.8f))
    }
}
