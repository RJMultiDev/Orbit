package com.qx.orbit.bili.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qx.orbit.bili.data.api.SearchApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchTab(val title: String, val type: String) {
    VIDEO("视频", "video"),
    LIVE("直播", "live"),
    USER("用户", "bili_user"),
    ARTICLE("图文", "article")
}

class SearchViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentTab = MutableStateFlow(SearchTab.VIDEO)
    val currentTab: StateFlow<SearchTab> = _currentTab.asStateFlow()

    private val _results = MutableStateFlow<List<Any>>(emptyList())
    val results: StateFlow<List<Any>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private var isEnd = false

    fun performSearch(query: String) {
        if (_searchQuery.value == query && _results.value.isNotEmpty()) return
        _searchQuery.value = query
        currentPage = 1
        isEnd = false
        _results.value = emptyList()
        fetchData()
    }

    fun switchTab(tab: SearchTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        if (_searchQuery.value.isNotEmpty()) {
            currentPage = 1
            isEnd = false
            _results.value = emptyList()
            fetchData()
        }
    }

    fun loadMore() {
        if (_isLoading.value || isEnd || _searchQuery.value.isEmpty()) return
        currentPage++
        fetchData()
    }

    private fun fetchData() {
        val query = _searchQuery.value
        val tab = _currentTab.value
        if (query.isEmpty()) return

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (tab == SearchTab.VIDEO) {
                    val res = SearchApi.search(query, currentPage)
                    if (res != null) {
                        val items = SearchApi.getVideosFromSearchResult(res, currentPage == 1)
                        if (items.isEmpty()) isEnd = true
                        _results.value = _results.value + items
                    } else {
                        isEnd = true
                    }
                } else {
                    val res = SearchApi.searchType(query, currentPage, tab.type)
                    if (res != null) {
                        val arr = if (tab == SearchTab.LIVE) {
                            if (res.isJsonObject) res.asJsonObject.get("live_room")?.asJsonArray
                            else if (res.isJsonArray) res.asJsonArray else null
                        } else {
                            if (res.isJsonArray) res.asJsonArray else null
                        }

                        if (arr != null) {
                            val jsonArray = org.json.JSONArray(arr.toString())
                            val items = when (tab) {
                                SearchTab.LIVE -> SearchApi.getLiveFromSearchResult(jsonArray)
                                SearchTab.USER -> SearchApi.getUsersFromSearchResult(jsonArray)
                                SearchTab.ARTICLE -> SearchApi.getArticlesFromSearchResult(jsonArray)
                                else -> emptyList<Any>()
                            }
                            if (items.isEmpty()) isEnd = true
                            _results.value = _results.value + items
                        } else {
                            isEnd = true
                        }
                    } else {
                        isEnd = true
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
