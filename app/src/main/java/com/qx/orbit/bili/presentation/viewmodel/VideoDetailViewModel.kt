package com.qx.orbit.bili.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qx.orbit.bili.data.api.LikeCoinFavApi
import com.qx.orbit.bili.data.api.RecommendApi
import com.qx.orbit.bili.data.api.ReplyApi
import com.qx.orbit.bili.data.api.VideoInfoApi
import com.qx.orbit.bili.data.model.Reply
import com.qx.orbit.bili.data.model.VideoCard
import com.qx.orbit.bili.data.model.VideoInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoDetailViewModel : ViewModel() {
    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _tags = MutableStateFlow<String>("")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _replies = MutableStateFlow<List<Reply>>(emptyList())
    val replies: StateFlow<List<Reply>> = _replies.asStateFlow()

    private val _relatedVideos = MutableStateFlow<List<VideoCard>>(emptyList())
    val relatedVideos: StateFlow<List<VideoCard>> = _relatedVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isReplyLoading = MutableStateFlow(false)
    val isReplyLoading: StateFlow<Boolean> = _isReplyLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    var bvid: String = ""
    var aid: Long = 0
    private var replyPage = 1

    fun loadData(bvid: String, aid: Long) {
        if (_isLoading.value) return
        this.bvid = bvid
        this.aid = aid

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val infoDeferred = async { VideoInfoApi.getVideoInfo(bvid) }
                val tagsDeferred = async { VideoInfoApi.getTags(bvid) }
                val relatedDeferred = async { RecommendApi.getRelated(aid) }
                
                _videoInfo.value = infoDeferred.await()
                _tags.value = tagsDeferred.await()
                _relatedVideos.value = relatedDeferred.await()
                
                loadReplies(reset = true)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = e.localizedMessage ?: "加载详情失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadReplies(reset: Boolean = false) {
        if (_isReplyLoading.value) return
        viewModelScope.launch {
            _isReplyLoading.value = true
            try {
                if (reset) replyPage = 1
                val newReplies = ReplyApi.getReplies(oid = aid, pageNumber = replyPage)
                if (reset) {
                    _replies.value = newReplies
                } else {
                    _replies.value = _replies.value + newReplies
                }
                replyPage++
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isReplyLoading.value = false
            }
        }
    }

    fun toggleLike() {
        val info = _videoInfo.value ?: return
        viewModelScope.launch {
            try {
                val isLiked = info.stats?.liked == true
                val newAction = if (isLiked) 2 else 1 // 1=like, 2=cancel
                val result = LikeCoinFavApi.like(aid, newAction)
                if (result == 0) {
                    // Update local state
                    val newStats = info.stats?.copy(
                        liked = !isLiked,
                        like = info.stats.like + (if (isLiked) -1 else 1)
                    )
                    _videoInfo.value = info.copy(stats = newStats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun toggleCoin() {
        val info = _videoInfo.value ?: return
        viewModelScope.launch {
            try {
                val isCoined = (info.stats?.coined ?: 0) > 0
                if (isCoined) return@launch // Bilibili cannot cancel coin easily via simple API
                val result = LikeCoinFavApi.coin(aid, 1) // throw 1 coin
                if (result == 0) {
                    val newStats = info.stats?.copy(
                        coined = 1,
                        coin = info.stats.coin + 1
                    )
                    _videoInfo.value = info.copy(stats = newStats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun toggleFavorite() {
        // Implement favorite if needed, requiring folder IDs.
        // For simplicity, we can leave it as a stub or implement default folder.
    }
}
