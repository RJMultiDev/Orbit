package com.qx.orbit.core.net

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IpFilterTest {

    @Test
    fun isIpv4_valid() {
        assertTrue(IpFilter.isIpv4("1.2.3.4"))
        assertTrue(IpFilter.isIpv4("255.255.255.255"))
        assertTrue(IpFilter.isIpv4("0.0.0.0"))
        assertTrue(IpFilter.isIpv4("192.168.001.1")) // 前导零仍可解析
    }

    @Test
    fun isIpv4_invalid() {
        assertFalse(IpFilter.isIpv4(""))
        assertFalse(IpFilter.isIpv4("256.1.1.1"))
        assertFalse(IpFilter.isIpv4("1.2.3"))
        assertFalse(IpFilter.isIpv4("1.2.3.4.5"))
        assertFalse(IpFilter.isIpv4("1.2.3.a"))
        assertFalse(IpFilter.isIpv4("1.2.3.4:5"))
    }

    @Test
    fun isIpv4_rejectsIpv6Forms() {
        assertFalse(IpFilter.isIpv4("2001:db8::1"))
        assertFalse(IpFilter.isIpv4("::1"))
        assertFalse(IpFilter.isIpv4("::ffff:1.2.3.4"))
        assertFalse(IpFilter.isIpv4("fe80::1%lo0"))
    }

    @Test
    fun onlyIpv4_filtersMixedList() {
        val mixed = listOf("10.0.0.1", "::1", "172.16.0.2", "2001:db8::2", "bad")
        assertEquals(listOf("10.0.0.1", "172.16.0.2"), IpFilter.onlyIpv4(mixed))
    }
}
