package com.qx.orbit.core.model.content

/** 番剧/影视（对应 Android `data/model/Bangumi.kt`）。 */
data class Bangumi(
    val info: Info? = null,
    val sectionList: List<Section> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Bangumi = Bangumi(
            info = Wire.map(fields, "info", "media", "result").let { it?.let(Info::fromWire) },
            sectionList = Wire.mapList(fields, "sections", "section_list", "sectionList").map { Section.fromWire(it) }
        )
    }

    data class Info(
        val media_id: Long = 0,
        val season_id: Long = 0,
        val type: Int = 0,
        val count: Int = 0,
        val score: Float = 0f,
        val title: String = "",
        val cover: String = "",
        val cover_horizontal: String = "",
        val type_name: String = "",
        val area_name: String = "",
        val indexShow: String = "",
        val evaluate: String = "",
        val staff: String = "",
        val record: String = "",
        val subtitle: String = "",
        val publish: Publish? = null,
        val styles: List<String> = emptyList(),
        val stat: Stat? = null,
        val up_info: UpInfo? = null,
        val series: Series? = null,
        val seasons: List<Season> = emptyList(),
        val user_status: UserStatus? = null
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Info = Info(
                media_id = Wire.long(fields, "media_id"),
                season_id = Wire.long(fields, "season_id"),
                type = Wire.int(fields, "type"),
                count = Wire.int(fields, "count"),
                score = Wire.float(fields, "score"),
                title = Wire.str(fields, "title"),
                cover = Wire.str(fields, "cover", "pic"),
                cover_horizontal = Wire.str(fields, "horizontal_picture", "cover_horizontal"),
                type_name = Wire.str(fields, "type_name"),
                area_name = Wire.str(fields, "area_name"),
                indexShow = Wire.str(fields, "index_show", "indexShow"),
                evaluate = Wire.str(fields, "evaluate"),
                staff = Wire.str(fields, "staff"),
                record = Wire.str(fields, "record"),
                subtitle = Wire.str(fields, "subtitle"),
                publish = Wire.map(fields, "publish").let { it?.let(Publish::fromWire) },
                styles = Wire.strList(fields, "styles"),
                stat = Wire.map(fields, "stat").let { it?.let(Stat::fromWire) },
                up_info = Wire.map(fields, "up_info", "upInfo").let { it?.let(UpInfo::fromWire) },
                series = Wire.map(fields, "series").let { it?.let(Series::fromWire) },
                seasons = Wire.mapList(fields, "seasons").map { Season.fromWire(it) },
                user_status = Wire.map(fields, "user_status").let { it?.let(UserStatus::fromWire) }
            )
        }
    }

    data class UserStatus(
        val progress: Progress? = null,
        val follow: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): UserStatus = UserStatus(
                progress = Wire.map(fields, "progress").let { it?.let(Progress::fromWire) },
                follow = Wire.int(fields, "follow")
            )
        }
    }

    data class Progress(
        val last_ep_id: Long = 0,
        val last_ep_index: String = "",
        val last_time: Long = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Progress = Progress(
                last_ep_id = Wire.long(fields, "last_ep_id"),
                last_ep_index = Wire.str(fields, "last_ep_index"),
                last_time = Wire.long(fields, "last_time")
            )
        }
    }

    data class Publish(
        val is_finish: Int = 0,
        val is_started: Int = 0,
        val pub_time: String = "",
        val pub_time_show: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Publish = Publish(
                is_finish = Wire.int(fields, "is_finish"),
                is_started = Wire.int(fields, "is_started"),
                pub_time = Wire.str(fields, "pub_time"),
                pub_time_show = Wire.str(fields, "pub_time_show")
            )
        }
    }

    data class Stat(
        val favorites: Int = 0,
        val series_follow: Int = 0,
        val views: Int = 0,
        val vt: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Stat = Stat(
                favorites = Wire.int(fields, "favorites"),
                series_follow = Wire.int(fields, "series_follow"),
                views = Wire.int(fields, "views"),
                vt = Wire.int(fields, "vt")
            )
        }
    }

    data class UpInfo(
        val mid: Long = 0,
        val name: String = "",
        val avatar: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): UpInfo = UpInfo(
                mid = Wire.long(fields, "mid"),
                name = Wire.str(fields, "name", "uname"),
                avatar = Wire.str(fields, "avatar", "face")
            )
        }
    }

    data class Series(
        val series_id: Long = 0,
        val series_title: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Series = Series(
                series_id = Wire.long(fields, "series_id"),
                series_title = Wire.str(fields, "series_title")
            )
        }
    }

    data class Season(
        val media_id: Long = 0,
        val season_id: Long = 0,
        val season_title: String = "",
        val cover: String = "",
        val badge: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Season = Season(
                media_id = Wire.long(fields, "media_id"),
                season_id = Wire.long(fields, "season_id"),
                season_title = Wire.str(fields, "season_title"),
                cover = Wire.str(fields, "cover", "pic"),
                badge = Wire.str(fields, "badge")
            )
        }
    }

    data class Section(
        val id: Long = 0,
        val type: Int = 0,
        val title: String = "",
        val episodes: List<Episode> = emptyList()
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Section = Section(
                id = Wire.long(fields, "id"),
                type = Wire.int(fields, "type"),
                title = Wire.str(fields, "title"),
                episodes = Wire.mapList(fields, "episodes").map { Episode.fromWire(it) }
            )
        }
    }

    data class Episode(
        val id: Long = 0,
        val aid: Long = 0,
        val cid: Long = 0,
        val title: String = "",
        val title_long: String = "",
        val cover: String = "",
        val badge: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Episode = Episode(
                id = Wire.long(fields, "id"),
                aid = Wire.long(fields, "aid"),
                cid = Wire.long(fields, "cid"),
                title = Wire.str(fields, "title"),
                title_long = Wire.str(fields, "long_title", "title_long"),
                cover = Wire.str(fields, "cover", "pic"),
                badge = Wire.str(fields, "badge")
            )
        }
    }
}

/** 番剧时间表（对应 Android `data/model/Bangumi.kt` 的 `Timeline`）。 */
data class Timeline(
    val dayList: List<DayInfo> = emptyList()
) {
    companion object {
        fun fromWire(fields: Map<String, Any?>): Timeline = Timeline(
            dayList = Wire.mapList(fields, "dayList", "day_list").map { DayInfo.fromWire(it) }
        )
    }

    data class DayInfo(
        val date: String = "",
        val date_ts: Long = 0,
        val day_of_week: Int = 0,
        val episodes: List<Episode> = emptyList(),
        val is_today: Int = 0
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): DayInfo = DayInfo(
                date = Wire.str(fields, "date"),
                date_ts = Wire.long(fields, "date_ts"),
                day_of_week = Wire.int(fields, "day_of_week"),
                episodes = Wire.mapList(fields, "episodes").map { Episode.fromWire(it) },
                is_today = Wire.int(fields, "is_today")
            )
        }
    }

    data class Episode(
        val cover: String = "",
        val delay: Int = 0,
        val episode_id: Long = 0,
        val pub_index: String = "",
        val pub_time: String = "",
        val pub_ts: Long = 0,
        val published: Int = 0,
        val follows: String = "",
        val plays: String = "",
        val season_id: Long = 0,
        val title: String = ""
    ) {
        companion object {
            fun fromWire(fields: Map<String, Any?>): Episode = Episode(
                cover = Wire.str(fields, "cover", "pic"),
                delay = Wire.int(fields, "delay"),
                episode_id = Wire.long(fields, "episode_id"),
                pub_index = Wire.str(fields, "pub_index"),
                pub_time = Wire.str(fields, "pub_time"),
                pub_ts = Wire.long(fields, "pub_ts"),
                published = Wire.int(fields, "published"),
                follows = Wire.str(fields, "follows"),
                plays = Wire.str(fields, "plays"),
                season_id = Wire.long(fields, "season_id"),
                title = Wire.str(fields, "title")
            )
        }
    }
}
