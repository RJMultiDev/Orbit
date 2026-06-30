package com.qx.orbit.bili.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qx.orbit.bili.data.api.RecommendApi
import com.qx.orbit.bili.data.model.VideoCard
import com.qx.orbit.bili.data.api.UserInfoApi
import com.qx.orbit.bili.data.api.UserInfoApi.NavInfoData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TabMode(val title: String) {
    RECOMMEND("推荐"),
    POPULAR("热门"),
    DYNAMIC("动态")
}

class MainViewModel : ViewModel() {

    private val _currentTab = MutableStateFlow(TabMode.RECOMMEND)
    val currentTab: StateFlow<TabMode> = _currentTab.asStateFlow()

    private val _videoList = MutableStateFlow<List<VideoCard>>(emptyList())
    val videoList: StateFlow<List<VideoCard>> = _videoList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _navInfo = MutableStateFlow<NavInfoData?>(null)
    val navInfo: StateFlow<NavInfoData?> = _navInfo.asStateFlow()

    private var recommendPage = 1
    private var popularPage = 1

    init {
        viewModelScope.launch {
            try {
                if (!com.qx.orbit.bili.data.remote.CookieManager.getCookie().contains("buvid3")) {
                    com.qx.orbit.bili.data.api.CookiesApi.checkCookies()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fetchNavInfo()
            loadMore()
        }
    }

    fun fetchNavInfo() {
        viewModelScope.launch {
            try {
                _navInfo.value = UserInfoApi.getNavInfo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchTab(tab: TabMode) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        _videoList.value = emptyList() // clear current list
        loadMore(reset = true)
    }

    fun loadMore(reset: Boolean = false) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (reset) {
                    if (_navInfo.value == null || !_navInfo.value!!.isLogin) {
                        fetchNavInfo()
                    }
                    if (_currentTab.value == TabMode.RECOMMEND) recommendPage = 1
                    else popularPage = 1
                }

                val newItems = when (_currentTab.value) {
                    TabMode.RECOMMEND -> RecommendApi.getRecommend(recommendPage++)
                    TabMode.POPULAR -> RecommendApi.getPopular(popularPage++)
                    TabMode.DYNAMIC -> emptyList()
                }

                _videoList.value = if (reset) {
                    newItems
                } else {
                    _videoList.value + newItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
