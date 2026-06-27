package com.qx.orbit.bili.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qx.orbit.bili.data.api.OpusApi
import com.qx.orbit.bili.data.api.ReplyApi
import com.qx.orbit.bili.data.model.Opus
import com.qx.orbit.bili.data.model.Reply
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpusDetailViewModel : ViewModel() {
    private val _opus = MutableStateFlow<Opus?>(null)
    val opus: StateFlow<Opus?> = _opus.asStateFlow()

    private val _replies = MutableStateFlow<List<Reply>>(emptyList())
    val replies: StateFlow<List<Reply>> = _replies.asStateFlow()

    private var replyNext: String? = null
    private var hasMoreReplies = true

    private val _isReplyLoading = MutableStateFlow(false)
    val isReplyLoading: StateFlow<Boolean> = _isReplyLoading.asStateFlow()

    fun loadOpus(id: Long) {
        viewModelScope.launch {
            try {
                val data = OpusApi.getOpus(id)
                _opus.value = data
                if (data != null) {
                    loadReplies()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadReplies() {
        if (!hasMoreReplies || _isReplyLoading.value) return
        _isReplyLoading.value = true
        viewModelScope.launch {
            try {
                val data = _opus.value ?: return@launch
                val result = ReplyApi.getRepliesLazy(data.commentId, 0, replyNext, data.commentType, 1)
                replyNext = result.second
                hasMoreReplies = result.second != null && result.second?.isNotBlank() == true
                val newReplies = _replies.value.toMutableList().apply { addAll(result.third) }
                _replies.value = newReplies
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isReplyLoading.value = false
            }
        }
    }

    fun likeReply(rpid: Long) {
        val data = _opus.value ?: return
        viewModelScope.launch {
            try {
                val reply = _replies.value.find { it.rpid == rpid } ?: return@launch
                val isLiked = reply.liked
                val action = if (isLiked) 0 else 1
                val resp = ReplyApi.likeReply(data.commentId, rpid, action, data.commentType)
                if (resp == 0) {
                    val newReplies = _replies.value.map {
                        if (it.rpid == rpid) {
                            it.copy(
                                liked = !isLiked,
                                likeCount = it.likeCount + (if (isLiked) -1 else 1)
                            )
                        } else it
                    }
                    _replies.value = newReplies
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLike() {
        val data = _opus.value ?: return
        viewModelScope.launch {
            try {
                val isLiked = data.stats?.liked == true
                val action = !isLiked
                val resp = OpusApi.likeOpus(data.id, action)
                if (resp == 0) {
                    val newStats = data.stats?.copy(
                        liked = !isLiked,
                        like = data.stats.like + (if (isLiked) -1 else 1)
                    )
                    _opus.value = data.copy(stats = newStats)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
