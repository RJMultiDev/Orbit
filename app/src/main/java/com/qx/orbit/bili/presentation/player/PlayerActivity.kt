package com.qx.orbit.bili.presentation.player

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.danmaku.parser.android.BiliDanmukuParser
import master.flame.danmaku.danmaku.parser.android.BiliProtobufDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import com.qx.orbit.bili.data.api.DanmakuApi
import com.qx.orbit.bili.data.api.LiveApi
import com.qx.orbit.bili.data.api.PlayerApi
import com.qx.orbit.bili.data.model.DanmakuElem
import com.qx.orbit.bili.data.model.DashData
import com.qx.orbit.bili.data.model.HighEnergyData
import com.qx.orbit.bili.data.model.LivePlayInfo
import com.qx.orbit.bili.data.model.PlayerData
import com.qx.orbit.bili.data.model.Subtitle
import com.qx.orbit.bili.data.model.SubtitleLink
import com.qx.orbit.bili.data.model.ViewPoint

@Suppress("DEPRECATION")
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_DANMAKU = "danmaku"
        const val EXTRA_TITLE = "title"
        const val EXTRA_AID = "aid"
        const val EXTRA_CID = "cid"
        const val EXTRA_MID = "mid"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_LIVE_MODE = "live_mode"
        const val EXTRA_PAGENAMES = "pagenames"
        const val EXTRA_CIDS = "cids"
        const val EXTRA_QN_STR_LIST = "qnStrList"
        const val EXTRA_QN_VALUE_LIST = "qnValueList"

        private const val SEEK_SECONDS = 10
        private const val SPEED_LONG_PRESS = 3.0f
        private const val CONTROLS_HIDE_DELAY = 4000L
        private const val PROGRESS_UPDATE_INTERVAL = 500L
        private const val DANMAKU_SEGMENT_COUNT = 6
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var danmakuView: DanmakuView
    private lateinit var controlsOverlay: FrameLayout
    private lateinit var topBar: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var batteryText: TextView
    private lateinit var onlineCountText: TextView
    private lateinit var bottomBar: LinearLayout
    private lateinit var playPauseBtn: ImageButton
    private lateinit var currentTimeText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var totalTimeText: TextView
    private lateinit var speedBtn: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var subtitleText: TextView
    private lateinit var gestureHintText: TextView

    private var mediaPlayer: IjkMediaPlayer? = null
    private var danmakuContext: DanmakuContext? = null
    private var danmakuParser: BaseDanmakuParser? = null

    private lateinit var playerData: PlayerData
    private var isLiveMode = false
    private var videoUrl = ""
    private var danmakuUrl = ""
    private var title = ""
    private var aid = 0L
    private var cid = 0L
    private var mid = 0L
    private var initialProgress = 0
    private var pagenames: List<String> = emptyList()
    private var cids: List<Long> = emptyList()
    private var qnStrList: Array<String>? = null
    private var qnValueList: IntArray? = null
    private var currentQuality = -1
    private var currentPageIndex = 0

    private var isPlaying = false
    private var isPrepared = false
    private var isControlsVisible = true
    private var isDanmakuVisible = true
    private var isLongPressing = false
    private var savedSpeed = 1.0f
    private var isLooping = false
    private var isAudioOnly = false
    private var surfaceReady = false
    private var isSeeking = false
    private var dashData: DashData? = null
    private var subtitleLinks: Array<SubtitleLink> = emptyArray()
    private var subtitles: Array<Subtitle> = emptyArray()
    private var viewPoints: List<ViewPoint> = emptyList()
    private var highEnergyData: HighEnergyData? = null
    private var livePlayInfo: LivePlayInfo? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private var pivotX = 0f
    private var pivotY = 0f
    private var gestureHintRunnable: Runnable? = null

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (isPrepared && isPlaying && !isSeeking) {
                updateProgress()
            }
            handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
        }
    }

    private val controlsHideRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFormat(PixelFormat.TRANSLUCENT)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        parseIntent()
        buildUI()
        initGestureDetectors()
        setupSurface()
        setupDanmaku()
        loadVideoData()
    }

    private fun parseIntent() {
        videoUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        danmakuUrl = intent.getStringExtra(EXTRA_DANMAKU) ?: ""
        title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        aid = intent.getLongExtra(EXTRA_AID, 0)
        cid = intent.getLongExtra(EXTRA_CID, 0)
        mid = intent.getLongExtra(EXTRA_MID, 0)
        initialProgress = intent.getIntExtra(EXTRA_PROGRESS, 0)
        isLiveMode = intent.getBooleanExtra(EXTRA_LIVE_MODE, false)
        pagenames = intent.getStringArrayListExtra(EXTRA_PAGENAMES) ?: emptyList()
        cids = intent.getLongArrayExtra(EXTRA_CIDS)?.toList() ?: emptyList()
        qnStrList = intent.getStringArrayExtra(EXTRA_QN_STR_LIST)
        qnValueList = intent.getIntArrayExtra(EXTRA_QN_VALUE_LIST)
        currentQuality = intent.getIntExtra("qn", -1)

        if (cids.isNotEmpty()) {
            currentPageIndex = cids.indexOf(cid).coerceAtLeast(0)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildUI() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        surfaceView = SurfaceView(this).apply {
            holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        rootLayout.addView(surfaceView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        ))

        danmakuView = DanmakuView(this)
        rootLayout.addView(danmakuView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        controlsOverlay = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        buildTopBar()
        buildBottomBar()
        buildLoadingIndicator()
        buildSubtitleText()
        buildGestureHintText()
        rootLayout.addView(controlsOverlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        controlsOverlay.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            scaleGestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP && isLongPressing) {
                onLongPressEnd()
            }
            true
        }

        setContentView(rootLayout)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun buildTopBar() {
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setBackgroundColor(0x80000000.toInt())
        }

        titleText = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(titleText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        batteryText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }
        topBar.addView(batteryText)

        onlineCountText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 10f
            visibility = View.GONE
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(onlineCountText)

        val topParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        )
        controlsOverlay.addView(topBar, topParams)
    }

    private fun buildBottomBar() {
        bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setBackgroundColor(0x80000000.toInt())
        }

        val seekRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        playPauseBtn = ImageButton(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(android.R.drawable.ic_media_pause)
            setOnClickListener { togglePlayPause() }
        }
        seekRow.addView(playPauseBtn, LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)))

        currentTimeText = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }
        seekRow.addView(currentTimeText)

        seekBar = SeekBar(this).apply {
            max = 1000
            progress = 0
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentTimeText.text = formatTime(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {
                    isSeeking = true
                    removeControlsHideCallback()
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    isSeeking = false
                    val duration = getDuration()
                    if (duration > 0) {
                        val targetMs = (sb!!.progress.toLong() * duration / 1000)
                        seekTo(targetMs)
                    }
                    scheduleControlsHide()
                }
            })
        }
        seekRow.addView(seekBar, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        totalTimeText = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
        }
        seekRow.addView(totalTimeText)

        bottomBar.addView(seekRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        speedBtn = TextView(this).apply {
            text = "1.0x"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { showSpeedSelector() }
        }
        btnRow.addView(speedBtn)

        val qualityBtn = TextView(this).apply {
            text = "画质"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { showQualitySelector() }
        }
        btnRow.addView(qualityBtn)

        val danmakuBtn = TextView(this).apply {
            text = "弹幕"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { toggleDanmaku() }
        }
        btnRow.addView(danmakuBtn)

        if (pagenames.size > 1) {
            val pageBtn = TextView(this).apply {
                text = "分P"
                setTextColor(Color.WHITE)
                textSize = 10f
                setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
                setOnClickListener { showPageSelector() }
            }
            btnRow.addView(pageBtn)
        }

        val loopBtn = TextView(this).apply {
            text = "循环"
            setTextColor(Color.WHITE)
            textSize = 10f
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener {
                isLooping = !isLooping
                setTextColor(if (isLooping) Color.GREEN else Color.WHITE)
            }
        }
        btnRow.addView(loopBtn)

        bottomBar.addView(btnRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val bottomParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        controlsOverlay.addView(bottomBar, bottomParams)
    }

    private fun buildLoadingIndicator() {
        loadingIndicator = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }
        val lp = FrameLayout.LayoutParams(dpToPx(32), dpToPx(32), Gravity.CENTER)
        controlsOverlay.addView(loadingIndicator, lp)
    }

    private fun buildSubtitleText() {
        subtitleText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = dpToPx(60)
        }
        controlsOverlay.addView(subtitleText, lp)
    }

    private fun buildGestureHintText() {
        gestureHintText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundColor(0x80000000.toInt())
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            visibility = View.GONE
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        controlsOverlay.addView(gestureHintText, lp)
    }

    private fun initGestureDetectors() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleControls()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val viewWidth = controlsOverlay.width
                if (e.x < viewWidth / 2) {
                    seekRelative(-SEEK_SECONDS * 1000L)
                    showGestureHint("-${SEEK_SECONDS}s")
                } else {
                    seekRelative(SEEK_SECONDS * 1000L)
                    showGestureHint("+${SEEK_SECONDS}s")
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                isLongPressing = true
                savedSpeed = mediaPlayer?.getSpeed(0f) ?: 1.0f
                setSpeed(SPEED_LONG_PRESS)
                showGestureHint("${SPEED_LONG_PRESS}x")
            }
        })

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
                pivotX = detector.focusX
                pivotY = detector.focusY
                surfaceView.scaleX = scaleFactor
                surfaceView.scaleY = scaleFactor
                surfaceView.pivotX = pivotX
                surfaceView.pivotY = pivotY
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (scaleFactor < 0.8f) {
                    scaleFactor = 1.0f
                    surfaceView.scaleX = 1f
                    surfaceView.scaleY = 1f
                }
            }
        })
    }

    private fun onLongPressEnd() {
        isLongPressing = false
        setSpeed(savedSpeed)
        hideGestureHint()
    }

    private fun setupSurface() {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceReady = true
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
                releasePlayer()
            }
        })
    }

    private fun setupDanmaku() {
        danmakuContext = DanmakuContext.create().apply {
            setDanmakuTransparency(0.9f)
            setScaleTextSize(0.8f)
            setMaximumVisibleSizeInScreen(60)
            setDuplicateMergingEnabled(true)
        }

        danmakuView.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                handler.post {
                    if (isPrepared) {
                        danmakuView.start(initialProgress.toLong())
                    }
                }
            }

            override fun updateTimer(timer: DanmakuTimer?) {}

            override fun danmakuShown(danmaku: BaseDanmaku?) {}

            override fun drawingFinished() {}
        })

        danmakuView.enableDanmakuDrawingCache(true)
    }

    private fun loadVideoData() {
        lifecycleScope.launch {
            try {
                if (isLiveMode) {
                    loadLiveStream()
                } else {
                    if (aid > 0 && cid > 0) {
                        playerData = PlayerData(
                            title = title,
                            videoUrl = videoUrl,
                            danmakuUrl = danmakuUrl,
                            aid = aid,
                            cid = cid,
                            mid = mid,
                            progress = initialProgress,
                            qn = currentQuality,
                            pagenames = pagenames,
                            cids = cids,
                            currentPageIndex = currentPageIndex
                        )
                        val dashResult = PlayerApi.getVideoDash(playerData)
                        playerData = dashResult
                        dashData = dashResult.dashData
                        videoUrl = dashResult.videoUrl
                        currentQuality = dashResult.progress
                        qnStrList = dashResult.qnStrList
                        qnValueList = dashResult.qnValueList
                    }

                    if (videoUrl.isEmpty()) {
                        showError("无法获取视频地址")
                        return@launch
                    }

                    preparePlayer()
                    loadDanmakuData()
                    loadSubtitles()
                    loadViewPoints()
                    loadHighEnergyData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("加载失败: ${e.message}")
            }
        }
    }

    private suspend fun loadLiveStream() {
        val roomId = aid
        val roomInfo = LiveApi.getRoomInfo(roomId)
        val playInfo = LiveApi.getRoomPlayInfo(roomId, currentQuality)
        livePlayInfo = playInfo

        handler.post {
            titleText.text = roomInfo?.title ?: title
            if (roomInfo != null && roomInfo.online > 0) {
                onlineCountText.text = "${roomInfo.online}人观看"
                onlineCountText.visibility = View.VISIBLE
            }
        }

        val stream = playInfo?.playurl_info?.playurl?.stream?.firstOrNull()
        val format = stream?.format?.firstOrNull()
        val codec = format?.codec?.firstOrNull()
        val urlInfo = codec?.urlInfo?.firstOrNull()

        if (codec != null && urlInfo != null) {
            videoUrl = "${urlInfo.host}${codec.base_url}${urlInfo.extra}"
        } else {
            videoUrl = format?.master_url ?: ""
        }

        if (videoUrl.isEmpty()) {
            showError("无法获取直播地址")
            return
        }

        preparePlayer()
    }

    private fun preparePlayer() {
        if (!surfaceReady) return

        releasePlayer()

        mediaPlayer = IjkMediaPlayer().apply {
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", 0x32315652)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "analyzemaxduration", 100)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "probesize", 1024 * 10)
            setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "flush_packets", 1)
            setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)

            if (isLiveMode) {
                setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "live直播延时", 1)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 0)
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "infbuf", 1)
            } else {
                setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "packet-buffering", 1)
            }

            setAudioStreamType(AudioManager.STREAM_MUSIC)

            setOnPreparedListener {
                isPrepared = true
                handler.post {
                    loadingIndicator.visibility = View.GONE
                    val duration = getDuration()
                    totalTimeText.text = formatTime(duration)
                    seekBar.max = 1000

                    if (!isLiveMode && initialProgress > 0) {
                        seekTo(initialProgress.toLong())
                    }
                    start()
                    handler.post(progressUpdateRunnable)
                    scheduleControlsHide()
                }
            }

            setOnCompletionListener {
                handler.post {
                    this@PlayerActivity.isPlaying = false
                    playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                    if (isLooping) {
                        seekTo(0)
                        start()
                    } else if (!isLiveMode && currentPageIndex < cids.size - 1) {
                        playNextPage()
                    }
                }
            }

            setOnErrorListener { _, what, extra ->
                handler.post {
                    showError("播放错误: what=$what, extra=$extra")
                }
                true
            }

            setOnInfoListener { _, what, _ ->
                when (what) {
                    IjkMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        handler.post { loadingIndicator.visibility = View.VISIBLE }
                    }
                    IjkMediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        handler.post { loadingIndicator.visibility = View.GONE }
                    }
                }
                false
            }

            setOnVideoSizeChangedListener { _, width, height, _, _ ->
                if (width > 0 && height > 0) {
                    adjustAspectRatio(width, height)
                }
            }

            surfaceView.holder.let { holder ->
                setDisplay(holder)
            }

            try {
                setDataSource(videoUrl)
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                showError("播放器初始化失败: ${e.message}")
            }
        }
    }

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val viewWidth = surfaceView.width
        val viewHeight = surfaceView.height
        if (viewWidth == 0 || viewHeight == 0) return

        val videoRatio = videoWidth.toFloat() / videoHeight
        val viewRatio = viewWidth.toFloat() / viewHeight

        val scaleX: Float
        val scaleY: Float
        if (videoRatio > viewRatio) {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        } else {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        }

        surfaceView.post {
            surfaceView.scaleX = scaleX
            surfaceView.scaleY = scaleY
        }
    }

    private suspend fun loadDanmakuData() {
        withContext(Dispatchers.IO) {
            try {
                if (danmakuUrl.isNotEmpty()) {
                    loadXmlDanmaku()
                } else {
                    loadProtobufDanmaku()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadXmlDanmaku() {
        try {
            val url = if (danmakuUrl.startsWith("http")) danmakuUrl else "https:$danmakuUrl"
            val request = okhttp3.Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .addHeader("Referer", "https://www.bilibili.com/")
                .build()
            val response = com.qx.orbit.bili.data.remote.HttpClient.client.newCall(request).execute()
            val inputStream = response.body?.byteStream() ?: return

            val parser = BiliDanmukuParser()
            val source = master.flame.danmaku.danmaku.parser.android.AndroidFileSource(inputStream)
            source.data()

            handler.post {
                danmakuParser = parser
                val timer = DanmakuTimer()
                parser.setConfig(danmakuContext).setTimer(timer).load(source)
                danmakuView.prepare(parser, danmakuContext)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            lifecycleScope.launch { loadProtobufDanmaku() }
        }
    }

    private suspend fun loadProtobufDanmaku() {
        val segments = mutableListOf<com.qx.orbit.bili.data.model.DmSegMobileReply>()
        for (i in 1..DANMAKU_SEGMENT_COUNT) {
            try {
                val seg = DanmakuApi.getVideoDanmakuSegment(aid, cid, i)
                if (seg != null) {
                    segments.add(seg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (segments.isEmpty()) return

        val parser = BiliProtobufDanmakuParser()
        parser.setDanmakuSegments(segments)

        handler.post {
            danmakuParser = parser
            val timer = DanmakuTimer()
            parser.setConfig(danmakuContext).setTimer(timer)
            danmakuView.prepare(parser, danmakuContext)
        }
    }

    private suspend fun loadSubtitles() {
        if (aid <= 0 || cid <= 0) return
        try {
            subtitleLinks = PlayerApi.getSubtitleLinks(aid, cid)
            if (subtitleLinks.isNotEmpty()) {
                val firstSub = subtitleLinks.first()
                subtitles = PlayerApi.getSubtitle(firstSub.url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadViewPoints() {
        if (aid <= 0 || cid <= 0) return
        try {
            viewPoints = PlayerApi.getViewPoints(aid, cid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadHighEnergyData() {
        if (aid <= 0 || cid <= 0) return
        try {
            highEnergyData = PlayerApi.getHighEnergyData(cid, aid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        mediaPlayer?.start()
        isPlaying = true
        danmakuView.start()
        playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun pause() {
        mediaPlayer?.pause()
        isPlaying = false
        danmakuView.pause()
        playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs)
        danmakuView.seekTo(positionMs)
    }

    private fun seekRelative(deltaMs: Long) {
        val currentPos = mediaPlayer?.currentPosition ?: 0
        val duration = getDuration()
        val target = (currentPos + deltaMs).coerceIn(0, duration)
        seekTo(target)
        showGestureHint(formatTime(target))
    }

    private fun getDuration(): Long {
        return mediaPlayer?.duration ?: 0
    }

    private fun updateProgress() {
        val current = mediaPlayer?.currentPosition ?: 0
        val duration = getDuration()
        if (duration > 0) {
            currentTimeText.text = formatTime(current)
            if (!isSeeking) {
                seekBar.progress = (current * 1000 / duration).toInt()
            }
        }
        updateSubtitleDisplay(current)
    }

    private fun updateSubtitleDisplay(currentMs: Long) {
        if (subtitles.isEmpty()) {
            subtitleText.visibility = View.GONE
            return
        }
        val currentSec = currentMs / 1000.0
        val matched = subtitles.find { currentSec in it.from..it.to }
        if (matched != null) {
            subtitleText.text = matched.content
            subtitleText.visibility = View.VISIBLE
        } else {
            subtitleText.visibility = View.GONE
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }

    private fun setSpeed(speed: Float) {
        try {
            mediaPlayer?.setSpeed(speed)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        danmakuView.setSpeed(speed)
        speedBtn.text = "${speed}x"
    }

    private fun toggleControls() {
        if (isControlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        isControlsVisible = true
        controlsOverlay.alpha = 1f
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        scheduleControlsHide()
    }

    private fun hideControls() {
        isControlsVisible = false
        controlsOverlay.alpha = 0f
        topBar.visibility = View.GONE
        bottomBar.visibility = View.GONE
    }

    private fun scheduleControlsHide() {
        removeControlsHideCallback()
        handler.postDelayed(controlsHideRunnable, CONTROLS_HIDE_DELAY)
    }

    private fun removeControlsHideCallback() {
        handler.removeCallbacks(controlsHideRunnable)
    }

    private fun showGestureHint(text: String) {
        gestureHintRunnable?.let { handler.removeCallbacks(it) }
        gestureHintText.text = text
        gestureHintText.visibility = View.VISIBLE
        gestureHintRunnable = Runnable { gestureHintText.visibility = View.GONE }
        handler.postDelayed(gestureHintRunnable!!, 800)
    }

    private fun hideGestureHint() {
        gestureHintRunnable?.let { handler.removeCallbacks(it) }
        gestureHintText.visibility = View.GONE
    }

    private fun toggleDanmaku() {
        isDanmakuVisible = !isDanmakuVisible
        if (isDanmakuVisible) {
            danmakuView.show()
        } else {
            danmakuView.hide()
        }
    }

    private fun showSpeedSelector() {
        val speeds = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x", "3.0x")
        val speedValues = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
        val currentIndex = speedValues.indexOfFirst {
            it == (mediaPlayer?.getSpeed(0f) ?: 1.0f)
        }.coerceAtLeast(2)

        android.app.AlertDialog.Builder(this)
            .setTitle("播放速度")
            .setSingleChoiceItems(speeds, currentIndex) { dialog, which ->
                setSpeed(speedValues[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showQualitySelector() {
        val qnStr = qnStrList
        val qnVal = qnValueList
        if (qnStr == null || qnVal == null || qnStr.isEmpty()) return

        val items = qnStr.mapIndexed { index, name ->
            val mark = if (qnVal[index] == currentQuality) " ●" else ""
            "$name$mark"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("画质选择")
            .setSingleChoiceItems(items, -1) { dialog, which ->
                dialog.dismiss()
                switchQuality(qnVal[which])
            }
            .show()
    }

    private fun switchQuality(qn: Int) {
        if (qn == currentQuality) return
        currentQuality = qn
        val savedPosition = mediaPlayer?.currentPosition ?: 0
        initialProgress = savedPosition.toInt()
        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                playerData = playerData.copy(qn = qn)
                val dashResult = PlayerApi.getVideoDash(playerData)
                playerData = dashResult
                dashData = dashResult.dashData
                videoUrl = dashResult.videoUrl
                currentQuality = dashResult.progress
                qnStrList = dashResult.qnStrList
                qnValueList = dashResult.qnValueList

                if (videoUrl.isNotEmpty()) {
                    initialProgress = savedPosition.toInt()
                    preparePlayer()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("切换画质失败")
            }
        }
    }

    private fun showPageSelector() {
        if (pagenames.isEmpty() || cids.isEmpty()) return
        val items = pagenames.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("选择分P")
            .setSingleChoiceItems(items, currentPageIndex) { dialog, which ->
                dialog.dismiss()
                switchPage(which)
            }
            .show()
    }

    private fun switchPage(index: Int) {
        if (index == currentPageIndex || index < 0 || index >= cids.size) return
        currentPageIndex = index
        cid = cids[index]
        title = if (index < pagenames.size) pagenames[index] else title
        titleText.text = title
        initialProgress = 0
        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                playerData = playerData.copy(
                    cid = cid,
                    title = title,
                    progress = 0,
                    currentPageIndex = index
                )
                val dashResult = PlayerApi.getVideoDash(playerData)
                playerData = dashResult
                dashData = dashResult.dashData
                videoUrl = dashResult.videoUrl
                currentQuality = dashResult.progress

                if (videoUrl.isNotEmpty()) {
                    danmakuView.clearDanmakusOnScreen()
                    preparePlayer()
                    loadDanmakuData()
                    loadSubtitles()
                    loadHighEnergyData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("切换分P失败")
            }
        }
    }

    private fun playNextPage() {
        if (currentPageIndex < cids.size - 1) {
            switchPage(currentPageIndex + 1)
        }
    }

    private fun showError(message: String) {
        loadingIndicator.visibility = View.GONE
        val errorText = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundColor(0xCC000000.toInt())
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        rootLayout.addView(errorText, lp)
        handler.postDelayed({ rootLayout.removeView(errorText) }, 3000)
    }

    private fun updateBatteryDisplay() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as? BatteryManager
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (level >= 0) {
            batteryText.text = "🔋$level%"
        }
    }

    private fun releasePlayer() {
        handler.removeCallbacks(progressUpdateRunnable)
        mediaPlayer?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        isPrepared = false
        isPlaying = false
    }

    private fun releaseDanmaku() {
        danmakuView.release()
    }

    override fun onResume() {
        super.onResume()
        updateBatteryDisplay()
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateBatteryDisplay()
                handler.postDelayed(this, 60_000)
            }
        }, 60_000)
    }

    override fun onPause() {
        super.onPause()
        if (isPlaying) {
            pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        releaseDanmaku()
        releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        adjustAspectRatio(
            mediaPlayer?.videoWidth ?: 0,
            mediaPlayer?.videoHeight ?: 0
        )
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause()
                return true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                start()
                return true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                pause()
                return true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekRelative(SEEK_SECONDS * 1000L)
                return true
            }
            android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekRelative(-SEEK_SECONDS * 1000L)
                return true
            }
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                toggleControls()
                return true
            }
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isControlsVisible) {
                    seekRelative(SEEK_SECONDS * 1000L)
                }
                return true
            }
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isControlsVisible) {
                    seekRelative(-SEEK_SECONDS * 1000L)
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
