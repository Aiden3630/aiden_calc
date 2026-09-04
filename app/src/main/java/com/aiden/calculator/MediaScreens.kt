package com.aiden.calculator

import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import java.text.DecimalFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
@Composable
internal fun MediaCard(
    item: VaultItem,
    marked: Boolean,
    selecting: Boolean,
    repository: VaultRepository,
    itemMenu: @Composable (VaultItem) -> Unit,
    open: () -> Unit,
    toggle: () -> Unit,
) {
    val scale by animateFloatAsState(if (marked) 0.97f else 1f, tween(140), label = "media-card-scale")
    val container by animateColorAsState(
        if (marked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(180),
        label = "media-card-bg",
    )
    val border = if (marked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Card(
        Modifier.padding(2.dp).scale(scale).combinedClickable(
            onClick = { if (selecting) toggle() else open() },
            onLongClick = toggle,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        border = border,
    ) {
        Box(Modifier.padding(6.dp)) {
            Thumbnail(
                item = item,
                repository = repository,
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 1f,
            )
            if (item.type == VaultItemType.VIDEO) {
                PlayBadge(Modifier.align(Alignment.Center).size(38.dp))
            }
            if (marked) SelectionBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                ItemName(item, repository)
                ItemSize(item, repository, marked, MaterialTheme.typography.labelSmall)
            }
            itemMenu(item)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun MediaListRow(
    item: VaultItem,
    marked: Boolean,
    selecting: Boolean,
    repository: VaultRepository,
    itemMenu: @Composable (VaultItem) -> Unit,
    open: () -> Unit,
    toggle: () -> Unit,
) {
    val container by animateColorAsState(
        if (marked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        tween(180),
        label = "media-row-bg",
    )
    val border = if (marked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).combinedClickable(
            onClick = { if (selecting) toggle() else open() },
            onLongClick = toggle,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 0.dp),
        border = border,
    ) {
        Row(Modifier.fillMaxWidth().height(88.dp).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val previewModifier = if (item.type == VaultItemType.VIDEO) Modifier.width(116.dp).height(72.dp) else Modifier.size(72.dp)
            Box(previewModifier) {
                Thumbnail(item, repository, Modifier.fillMaxSize(), keepAspectRatio = false)
                if (item.type == VaultItemType.VIDEO) {
                    PlayBadge(Modifier.align(Alignment.Center).size(34.dp))
                }
                if (marked) SelectionBadge(Modifier.align(Alignment.TopStart).padding(5.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                ItemName(item, repository)
                ItemSize(item, repository, marked, MaterialTheme.typography.labelMedium)
            }
            itemMenu(item)
        }
    }
}

@Composable
internal fun Thumbnail(
    item: VaultItem,
    repository: VaultRepository,
    modifier: Modifier = Modifier,
    keepAspectRatio: Boolean = true,
    aspectRatio: Float = if (item.type == VaultItemType.VIDEO) 16f / 9f else 1f,
) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.id) {
        image = runCatching {
            loadThumbnailImage(repository, item)
        }.getOrNull()
    }
    val shape = RoundedCornerShape(12.dp)
    val background = if (item.type == VaultItemType.VIDEO) Color(0xFF111111)
        else MaterialTheme.colorScheme.surfaceVariant
    val thumbnailModifier = if (keepAspectRatio) modifier.aspectRatio(aspectRatio) else modifier
    Box(
        thumbnailModifier
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val currentImage = image
        if (currentImage != null) {
            Image(
                currentImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (item.type == VaultItemType.VIDEO) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun PlayBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(Color(0xA6000000)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun SelectionBadge(modifier: Modifier = Modifier) {
    Icon(
        Icons.Default.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .size(22.dp),
    )
}

@Composable
internal fun ItemName(item: VaultItem, repository: VaultRepository) {
    val context = LocalContext.current
    var name by remember(item.id) { mutableStateOf(context.getString(R.string.file)) }
    LaunchedEffect(item.id) { name = repository.displayName(item) }
    Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
}

@Composable
internal fun ItemSize(item: VaultItem, repository: VaultRepository, marked: Boolean, style: TextStyle) {
    var size by remember(item.id, item.plainSize) { mutableStateOf(item.plainSize) }
    LaunchedEffect(item.id, item.plainSize) { size = runCatching { repository.plainSize(item) }.getOrNull() }
    Text("${formatFileSize(size ?: item.size)}${if (marked) " ✓" else ""}", style = style)
}

@OptIn(UnstableApi::class)
@Composable
internal fun PhotoViewerDialog(
    photos: List<VaultItem>,
    selectedId: String,
    repository: VaultRepository,
    previews: MediaPreviewController,
    close: () -> Unit,
) {
    val context = LocalContext.current
    var index by remember(selectedId) { mutableStateOf(photos.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)) }
    val item = photos[index]
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var swipeX by remember { mutableStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var decodeFailed by remember { mutableStateOf(false) }
    var panelsVisible by remember { mutableStateOf(true) }
    var name by remember(item.id) { mutableStateOf(context.getString(R.string.file)) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    LaunchedEffect(item.id) {
        name = runCatching { repository.displayName(item) }.getOrDefault(context.getString(R.string.file))
        loading = true
        decodeFailed = false
        bitmap = runCatching { previews.sampledBitmap(item, screenWidth, screenHeight) }.getOrNull()
        decodeFailed = bitmap == null
        loading = false
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        swipeX = 0f
    }
    fun show(next: Int) {
        index = (index + next).coerceIn(0, photos.lastIndex)
        swipeX = 0f
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(
                    it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                        .pointerInput(item.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val oldScale = scale
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (oldScale == 1f && scale == 1f) {
                                    swipeX += pan.x
                                    if (kotlin.math.abs(swipeX) > 120f) show(if (swipeX < 0) 1 else -1)
                                } else {
                                    val fit = min(screenWidth.toFloat() / it.width, screenHeight.toFloat() / it.height)
                                    val maxX = max(0f, (it.width * fit * scale - screenWidth) / 2f)
                                    val maxY = max(0f, (it.height * fit * scale - screenHeight) / 2f)
                                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                                }
                            }
                        }
                        .pointerInput(item.id) { detectTapGestures { panelsVisible = !panelsVisible } },
                    contentScale = ContentScale.Fit,
                )
            }
            if (loading) Text(stringResource(R.string.photo_loading), color = Color.White, modifier = Modifier.align(Alignment.Center))
            if (decodeFailed) Text(stringResource(R.string.photo_decode_failed), color = Color.White, modifier = Modifier.align(Alignment.Center))
            if (panelsVisible) {
                Row(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0x99000000)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = close) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White) }
                }
                Row(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x99000000)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = { show(-1) }, enabled = index > 0) { Text("<") }
                    Button(onClick = { show(1) }, enabled = index < photos.lastIndex) { Text(">") }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun VideoPlayerScreen(
    activity: FragmentActivity,
    itemId: String,
    videoIds: List<String>,
    repository: VaultRepository,
    previews: MediaPreviewController,
    close: () -> Unit,
) {
    var items by remember(videoIds) { mutableStateOf<List<VaultItem>?>(null) }
    LaunchedEffect(videoIds) {
        val byId = repository.currentItems(videoIds).associateBy { it.id }
        items = videoIds.mapNotNull(byId::get).filter {
            it.type == VaultItemType.VIDEO && it.trashState == TrashState.ACTIVE
        }
    }
    val videos = items
    if (videos == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.video_loading), color = Color.White)
        }
        return
    }
    if (videos.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.video_loading), color = Color.White)
        }
        return
    }
    val initialIndex = videos.indexOfFirst { it.id == itemId }.coerceAtLeast(0)
    var playbackError by remember(videos, itemId) { mutableStateOf(false) }
    val player = remember(videos, initialIndex) {
        ExoPlayer.Builder(activity).build().apply {
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = true
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    playbackError = false
                }
            })
            val sources = videos.map { item ->
                ProgressiveMediaSource.Factory(previews.videoDataSource(item))
                    .createMediaSource(MediaItem.fromUri(Uri.parse("vault://${item.id}")))
            }
            setMediaSources(sources, initialIndex, 0L)
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            player.release()
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(event: MotionEvent): Boolean = true

                    override fun onFling(
                        first: MotionEvent?,
                        second: MotionEvent,
                        velocityX: Float,
                        velocityY: Float,
                    ): Boolean {
                        val start = first ?: return false
                        val distanceX = second.x - start.x
                        val distanceY = second.y - start.y
                        if (abs(distanceX) < VIDEO_SWIPE_DISTANCE ||
                            abs(velocityX) < VIDEO_SWIPE_VELOCITY ||
                            abs(distanceX) <= abs(distanceY)
                        ) return false
                        if (distanceX < 0 && player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                            player.play()
                            return true
                        }
                        if (distanceX > 0 && player.hasPreviousMediaItem()) {
                            player.seekToPreviousMediaItem()
                            player.play()
                            return true
                        }
                        return false
                    }
                })
                PlayerView(context).apply {
                    this.player = player
                    controllerShowTimeoutMs = 3_000
                    controllerAutoShow = true
                    setShowPreviousButton(true)
                    setShowNextButton(true)
                    setOnTouchListener { _, event ->
                        gestures.onTouchEvent(event)
                        false
                    }
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = close,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color(0x99000000)),
        ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White) }
        if (playbackError) {
            Row(
                Modifier.align(Alignment.Center).padding(20.dp).background(Color(0xCC000000)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                Text(stringResource(R.string.video_playback_failed), color = Color.White, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes Б"
    val units = arrayOf("КБ", "МБ", "ГБ", "ТБ")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "${DecimalFormat("0.##").format(value)} ${units[unit]}"
}

private suspend fun loadThumbnailImage(repository: VaultRepository, item: VaultItem): ImageBitmap? {
    val key = "${item.id}:${item.size}:${item.createdAt}"
    ThumbnailImageCache.get(key)?.let { return it }
    return withContext(Dispatchers.Default) {
        val bytes = repository.thumbnail(item) ?: return@withContext null
        val bitmap = decodeSampledThumbnail(bytes, THUMBNAIL_MAX_DECODE_SIDE) ?: return@withContext null
        bitmap.asImageBitmap().also { ThumbnailImageCache.put(key, it) }
    }
}

private fun decodeSampledThumbnail(bytes: ByteArray, maxSide: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private object ThumbnailImageCache {
    private val entries = object : LinkedHashMap<String, ImageBitmap>(THUMBNAIL_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > THUMBNAIL_CACHE_SIZE
    }

    @Synchronized fun get(key: String): ImageBitmap? = entries[key]

    @Synchronized fun put(key: String, image: ImageBitmap) {
        entries[key] = image
    }
}

private const val THUMBNAIL_MAX_DECODE_SIDE = 180
private const val THUMBNAIL_CACHE_SIZE = 80
private const val VIDEO_SWIPE_DISTANCE = 120f
private const val VIDEO_SWIPE_VELOCITY = 300f
