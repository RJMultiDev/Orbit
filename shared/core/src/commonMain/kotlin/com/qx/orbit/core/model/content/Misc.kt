package com.qx.orbit.core.model.content

/** 杂项模型（对应 Android `data/model/Misc.kt`）。 */

data class Announcement(
    val id: Int = 0,
    val ctime: String = "",
    val title: String = "",
    val content: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Announcement = Announcement(
            id = Wire.int(fields, "id"),
            ctime = Wire.str(fields, "ctime"),
            title = Wire.str(fields, "title"),
            content = Wire.str(fields, "content")
        )
    }
}

data class Tutorial(
    val name: String = "",
    val description: String = "",
    val imgid: String = "",
    val type: Int = 0,
    val content: List<CustomText> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Tutorial = Tutorial(
            name = Wire.str(fields, "name"),
            description = Wire.str(fields, "description", "desc"),
            imgid = Wire.str(fields, "imgid", "img_id"),
            type = Wire.int(fields, "type"),
            content = Wire.mapList(fields, "content").map { CustomText.fromWire(it) }
        )
    }
}

data class CustomText(
    val type: Int = 0,
    val text: String = "",
    val style: String = "",
    val color: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): CustomText = CustomText(
            type = Wire.int(fields, "type"),
            text = Wire.str(fields, "text"),
            style = Wire.str(fields, "style"),
            color = Wire.str(fields, "color")
        )
    }
}

data class SettingSection(
    val type: String = "",
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val defaultValue: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): SettingSection = SettingSection(
            type = Wire.str(fields, "type"),
            id = Wire.str(fields, "id"),
            name = Wire.str(fields, "name"),
            desc = Wire.str(fields, "desc"),
            defaultValue = Wire.str(fields, "defaultValue", "default_value")
        )
    }
}

data class LocalVideo(
    val cover: String = "",
    val title: String = "",
    val pageList: List<String> = emptyList(),
    val videoFileList: List<String> = emptyList(),
    val danmakuFileList: List<String> = emptyList(),
    val sizeList: List<Long> = emptyList(),
    val size: Long = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): LocalVideo = LocalVideo(
            cover = Wire.str(fields, "cover", "pic"),
            title = Wire.str(fields, "title"),
            pageList = Wire.strList(fields, "pageList", "page_list"),
            videoFileList = Wire.strList(fields, "videoFileList", "video_file_list"),
            danmakuFileList = Wire.strList(fields, "danmakuFileList", "danmaku_file_list"),
            sizeList = Wire.longList(fields, "sizeList", "size_list"),
            size = Wire.long(fields, "size")
        )
    }
}

data class DownloadSection(
    val id: Long = 0,
    val type: String = "",
    val aid: Long = 0,
    val cid: Long = 0,
    val qn: Int = 0,
    val url_cover: String = "",
    val title: String = "",
    val child: String = "",
    val state: String = "",
    val downloadType: String = "",
    val audioUrl: String = ""
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): DownloadSection = DownloadSection(
            id = Wire.long(fields, "id"),
            type = Wire.str(fields, "type"),
            aid = Wire.long(fields, "aid"),
            cid = Wire.long(fields, "cid"),
            qn = Wire.int(fields, "qn"),
            url_cover = Wire.str(fields, "url_cover"),
            title = Wire.str(fields, "title"),
            child = Wire.str(fields, "child"),
            state = Wire.str(fields, "state"),
            downloadType = Wire.str(fields, "downloadType", "download_type"),
            audioUrl = Wire.str(fields, "audioUrl", "audio_url")
        )
    }
}

data class PageInfo(
    val page_num: Int = 0,
    val require_ps: Int = 0,
    val total: Int = 0,
    val return_ps: Int = 0
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): PageInfo = PageInfo(
            page_num = Wire.int(fields, "page_num"),
            require_ps = Wire.int(fields, "require_ps"),
            total = Wire.int(fields, "total"),
            return_ps = Wire.int(fields, "return_ps")
        )
    }
}

data class ApiResult(
    val code: Int = -1,
    val message: String = "",
    val offset: Long = 0,
    val timestamp: Long = 0,
    val business: String = "",
    val isBottom: Boolean = false
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): ApiResult = ApiResult(
            code = Wire.int(fields, "code").let { if (fields.containsKey("code")) it else -1 },
            message = Wire.str(fields, "message", "msg"),
            offset = Wire.long(fields, "offset"),
            timestamp = Wire.long(fields, "timestamp"),
            business = Wire.str(fields, "business"),
            isBottom = Wire.bool(fields, "isBottom", "is_bottom")
        )
    }
}
