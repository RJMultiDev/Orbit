package com.qx.orbit.core.net

/**
 * IPv4 偏好过滤（纯算法）。
 *
 * 对齐 app 侧 HttpClient.ipv4Dns：`Dns.SYSTEM.lookup(hostname).filter { it.address.size == 4 }`，
 * 即 DNS 结果仅保留 IPv4、剔除 IPv6。此处对地址文本做等价判定，不依赖 java.net。
 */
object IpFilter {

    /**
     * 是否为 IPv4 文本：四段点分十进制，每段 0..255。
     * 含 `:`（IPv6 / IPv6 映射 / zone index）或解析失败必然返回 false。
     */
    fun isIpv4(address: String): Boolean {
        if (address.isEmpty()) return false
        val parts = address.split('.')
        if (parts.size != 4) return false
        for (part in parts) {
            if (part.isEmpty()) return false
            val v = part.toIntOrNull() ?: return false
            if (v !in 0..255) return false
        }
        return true
    }

    /** 仅保留 IPv4 地址，顺序不变。 */
    fun onlyIpv4(addresses: List<String>): List<String> = addresses.filter(::isIpv4)
}
