package com.qx.orbit.bili.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text as Material3Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.qx.orbit.bili.data.api.EmoteApi
import kotlinx.coroutines.delay

@Composable
fun WriteReplyScreen(
    visible: Boolean,
    targetName: String?,
    emotes: List<EmoteApi.EmotePackage>?,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    Dialog(
        visible = visible,
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        val listState = rememberTransformingLazyColumnState()
        val pagerState = rememberPagerState(pageCount = { emotes?.size ?: 0 })
        val focusRequester = remember { FocusRequester() }

        // Request focus after dialog is fully rendered
        LaunchedEffect(Unit) {
            delay(300)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }

        // Preload emote images after dialog animation completes
        LaunchedEffect(emotes) {
            delay(400)
            val imageLoader = context.imageLoader
            emotes?.forEach { pkg ->
                if (pkg.type != 4) {
                    pkg.emotes.forEach { emote ->
                        if (emote.url.isNotBlank()) {
                            val request = ImageRequest.Builder(context)
                                .data(emote.url.replace("http://", "https://"))
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }
            }
        }

        // Re-request focus whenever pager page changes so crown always scrolls the outer list
        LaunchedEffect(pagerState.currentPage) {
            delay(100)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }

        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.focusRequester(focusRequester)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                TransformingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    item {
                        ListHeader {
                            Text(if (targetName.isNullOrEmpty()) "发布评论" else "回复 @$targetName", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = {
                                    Material3Text(
                                        "输入回复...",
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.background,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    if (text.isNotEmpty()) {
                                        isSending = true
                                        onSend(text)
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = CircleShape
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Emote panel with fade-in transition
                        AnimatedContent(
                            targetState = emotes,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(150))
                            },
                            label = "EmotePanel"
                        ) { currentEmotes ->
                            if (currentEmotes == null) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else if (currentEmotes.isEmpty()) {
                                Text(
                                    "暂无表情包",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                    Text(
                                        text = currentEmotes[pagerState.currentPage].text,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                    ) { page ->
                                        val pkg = currentEmotes[page]
                                        val chunkSize = if (pkg.type == 4) 1 else 3
                                        val chunkedEmotes = pkg.emotes.chunked(chunkSize)

                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            chunkedEmotes.forEach { rowItems ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly
                                                ) {
                                                    rowItems.forEach { emote ->
                                                        if (pkg.type == 4 || emote.url.isBlank()) {
                                                            Text(
                                                                text = emote.name,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                modifier = Modifier
                                                                    .height(40.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .clickable { text += emote.name }
                                                                    .padding(4.dp),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        } else {
                                                            AsyncImage(
                                                                model = emote.url.replace("http://", "https://"),
                                                                contentDescription = emote.name,
                                                                contentScale = ContentScale.Fit,
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .clickable { text += emote.name }
                                                                    .padding(4.dp)
                                                            )
                                                        }
                                                    }
                                                    // Fill empty spots if row has fewer items than chunkSize
                                                    repeat(chunkSize - rowItems.size) {
                                                        Spacer(modifier = Modifier.size(40.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
                if (emotes != null && emotes.isNotEmpty()) {
                    HorizontalPageIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}
