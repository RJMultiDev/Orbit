package com.qx.orbit.core.model.media

import com.qx.orbit.core.model.content.Wire

/**
 * 直播播放地址树（对应 Android `data/model/Live.kt` 的 `Playurl` 及其子模型）。
 * 直播 playurl 接口 `x/live/playurl` 返回 `playurl_info.playurl`，树形结构逐层解析。
 */
data class Playurl(
    val cid: Long = 0,
    val qnDesc: List<QnDesc> = emptyList(),
    val stream: List<ProtocolInfo> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Playurl = Playurl(
            cid = Wire.long(fields, "cid"),
            qnDesc = Wire.mapList(fields, "g_qn_desc", "qn_desc").map { QnDesc.fromWire(it) },
            stream = Wire.mapList(fields, "stream").map { ProtocolInfo.fromWire(it) }
        )
    }
}

/** 清晰度描述（对应 Android `QnDesc`）。 */
data class QnDesc(
    val qn: Int = 0,
    val desc: String = "",
    val hdr_desc: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): QnDesc = QnDesc(
            qn = Wire.int(fields, "qn"),
            desc = Wire.str(fields, "desc"),
            hdr_desc = Wire.str(fields, "hdr_desc", "hdrDesc")
        )
    }
}

/** 拉流协议（http_stream/hls 等，对应 Android `ProtocolInfo`）。 */
data class ProtocolInfo(
    val protocol_name: String = "",
    val format: List<FormatInfo> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ProtocolInfo = ProtocolInfo(
            protocol_name = Wire.str(fields, "protocol_name", "protocolName"),
            format = Wire.mapList(fields, "format").map { FormatInfo.fromWire(it) }
        )
    }
}

/** 封装格式（fmp4/flv 等，对应 Android `FormatInfo`）。 */
data class FormatInfo(
    val format_name: String = "",
    val codec: List<CodecInfo> = emptyList(),
    val master_url: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): FormatInfo = FormatInfo(
            format_name = Wire.str(fields, "format_name", "formatName"),
            codec = Wire.mapList(fields, "codec").map { CodecInfo.fromWire(it) },
            master_url = Wire.str(fields, "master_url", "masterUrl")
        )
    }
}

/** 编码流信息（对应 Android `CodecInfo`）。 */
data class CodecInfo(
    val codec_name: String = "",
    val current_qn: Int = 0,
    val acceptQn: List<Int> = emptyList(),
    val base_url: String = "",
    val urlInfo: List<UrlInfo> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): CodecInfo = CodecInfo(
            codec_name = Wire.str(fields, "codec_name", "codecName"),
            current_qn = Wire.int(fields, "current_qn", "currentQn"),
            acceptQn = Wire.intList(fields, "accept_qn", "acceptQn"),
            base_url = Wire.str(fields, "base_url", "baseUrl"),
            urlInfo = Wire.mapList(fields, "url_info", "urlInfo").map { UrlInfo.fromWire(it) }
        )
    }
}

/** 分片地址（对应 Android `UrlInfo`）。 */
data class UrlInfo(
    val host: String = "",
    val extra: String = "",
    val stream_ttl: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): UrlInfo = UrlInfo(
            host = Wire.str(fields, "host"),
            extra = Wire.str(fields, "extra"),
            stream_ttl = Wire.int(fields, "stream_ttl", "streamTtl")
        )
    }
}
