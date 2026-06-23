package com.qx.orbit.bili.data.api

import com.qx.orbit.bili.data.model.*
import com.qx.orbit.bili.data.remote.CookieManager
import com.qx.orbit.bili.data.remote.GsonConfig
import com.qx.orbit.bili.data.remote.HttpClient
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object SeriesApi {

    internal data class SeriesListData(
        @SerializedName("items_lists") val items_lists: SeriesItems? = null
    )

    internal data class SeriesItems(
        @SerializedName("series_list") val series_list: List<SeriesMeta>? = null,
        @SerializedName("page") val page: PageData? = null
    )

    internal data class SeriesMeta(
        @SerializedName("meta") val meta: SeriesMetaInfo? = null
    )

    internal data class SeriesMetaInfo(
        @SerializedName("series_id") val series_id: Int = 0,
        @SerializedName("name") val name: String? = null,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("description") val description: String? = null,
        @SerializedName("total") val total: Int = 0
    )

    internal data class PageData(
        @SerializedName("page_num") val page_num: Int = 0,
        @SerializedName("page_size") val page_size: Int = 0,
        @SerializedName("total") val total: Int = 0
    )

    internal data class SeriesArchivesData(
        @SerializedName("archives") val archives: List<SeriesArchive>? = null,
        @SerializedName("page") val page: PageData? = null
    )

    internal data class SeriesArchive(
        @SerializedName("aid") val aid: Long = 0,
        @SerializedName("bvid") val bvid: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("pic") val pic: String? = null,
        @SerializedName("stat") val stat: SeriesStat? = null,
        @SerializedName("owner") val owner: SeriesOwner? = null
    )

    internal data class SeriesStat(
        @SerializedName("view") val view: Int = 0
    )

    internal data class SeriesOwner(
        @SerializedName("name") val name: String? = null
    )

    suspend fun getUserSeries(mid: Long, page: Int): Pair<List<Series>, PageInfo?> = withContext(Dispatchers.IO) {
        val rawUrl = "https://api.bilibili.com/x/polymer/web-space/seasons_series_list?mid=$mid&page_num=$page&page_size=20"
        val url = WbiSigner.signUrl(rawUrl)
        val json = httpGet(url)
        val type = object : TypeToken<ApiResponse<SeriesListData>>() {}.type
        val resp: ApiResponse<SeriesListData>? = GsonConfig.gson.fromJson(json, type)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext Pair(emptyList(), null)
        val items = resp.data.items_lists
        val seriesList = items?.series_list?.map { meta ->
            Series(
                type = "series",
                id = meta.meta?.series_id ?: 0,
                title = meta.meta?.name ?: "",
                cover = meta.meta?.cover ?: "",
                intro = meta.meta?.description ?: "",
                mid = mid,
                total = (meta.meta?.total ?: 0).toString()
            )
        } ?: emptyList()
        val pageInfo = items?.page?.let {
            PageInfo(page_num = it.page_num, total = it.total, return_ps = it.page_size)
        }
        Pair(seriesList, pageInfo)
    }

    suspend fun getSeriesInfo(type: String, mid: Long, id: Int, page: Int): Pair<List<VideoCard>, PageInfo?> = withContext(Dispatchers.IO) {
        val rawUrl = if (type == "series") {
            "https://api.bilibili.com/x/series/archives?mid=$mid&series_id=$id&pn=$page&ps=20"
        } else {
            "https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?mid=$mid&season_id=$id&page_num=$page&page_size=20"
        }
        val url = WbiSigner.signUrl(rawUrl)
        val json = httpGet(url)
        val type2 = object : TypeToken<ApiResponse<SeriesArchivesData>>() {}.type
        val resp: ApiResponse<SeriesArchivesData>? = GsonConfig.gson.fromJson(json, type2)
        if (resp == null || !resp.isSuccess || resp.data == null) return@withContext Pair(emptyList(), null)
        val archives = resp.data.archives
        val cards = archives?.map { archive ->
            VideoCard(
                title = archive.title ?: "",
                upName = archive.owner?.name ?: "",
                view = StringUtil.toWan(archive.stat?.view?.toLong() ?: 0),
                cover = archive.pic ?: "",
                aid = archive.aid,
                bvid = archive.bvid ?: ""
            )
        } ?: emptyList()
        val pageInfo = resp.data.page?.let {
            PageInfo(page_num = it.page_num, total = it.total, return_ps = it.page_size)
        }
        Pair(cards, pageInfo)
    }

    private fun httpGet(url: String): String {
        val request = Request.Builder().url(url)
            .addHeader("Cookie", CookieManager.getCookie())
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Referer", "https://www.bilibili.com/")
            .build()
        return HttpClient.client.newCall(request).execute().body?.string() ?: ""
    }

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.95 Safari/537.36"
}
