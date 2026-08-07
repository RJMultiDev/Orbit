package com.qx.orbit.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreMarkerTest {

    @Test
    fun moduleIsWired() {
        assertEquals("orbit-core", CoreMarker.NAME)
    }
}
