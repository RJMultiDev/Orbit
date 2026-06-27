package com.qx.orbit.bili.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import com.qx.orbit.bili.data.model.Reply
import com.qx.orbit.bili.presentation.viewmodel.ReplyDetailViewModel
import com.qx.orbit.bili.presentation.component.WysTimeText
import com.qx.orbit.bili.util.formatCount

@Composable
fun ReplyDetailScreen(
    reply: Reply,
    viewModel: ReplyDetailViewModel,
    navController: NavHostController
) {
    LaunchedEffect(reply) {
        viewModel.initData(reply)
    }

    val rootReply by viewModel.rootReply.collectAsState()
    val childReplies by viewModel.childReplies.collectAsState()
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScreenScaffold(
        timeText = { WysTimeText() },
        scrollState = listState,
        modifier = Modifier.focusRequester(focusRequester)
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            if (rootReply != null) {
                val root = rootReply!!
                item {
                    ReplyCard(
                        reply = root,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                        navController = navController,
                        onLikeClick = { viewModel.likeRootReply(root.liked) },
                        onReplyClick = {}
                    )
                }
            }

            if (childReplies.isNotEmpty() || (rootReply?.childCount ?: 0) > 0) {
                item {
                    ListHeader {
                        val count = if (childReplies.size > (rootReply?.childCount ?: 0)) childReplies.size else rootReply?.childCount ?: 0
                        Text(
                            text = "相关回复(${formatCount(count)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                items(childReplies.size) { index ->
                    if (index == childReplies.size - 1) {
                        LaunchedEffect(index) { viewModel.loadMore() }
                    }
                    ReplyCard(
                        reply = childReplies[index],
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                        navController = navController,
                        onLikeClick = { viewModel.likeChildReply(childReplies[index].rpid, childReplies[index].liked) },
                        onReplyClick = {}
                    )
                }
            }
        }
    }
}
