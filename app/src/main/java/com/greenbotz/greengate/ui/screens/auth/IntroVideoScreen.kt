package com.greenbotz.greengate.ui.screens.auth

import android.content.Context
import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.greenbotz.greengate.R
import kotlin.math.max

@Composable
fun IntroVideoScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val videoView = remember {
        PromoTextureView(context, onFinished).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            setVideoUri(Uri.parse("android.resource://${context.packageName}/${R.raw.login_promo}"))
        }
    }
    DisposableEffect(videoView) {
        onDispose { videoView.release() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { videoView }, modifier = Modifier.fillMaxSize())
        Text(
            "Skip",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 20.dp)
                .background(Color.Black.copy(alpha = .45f))
                .clickable { onFinished() }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * MediaPlayer-backed TextureView that applies center-crop scaling after the video
 * dimensions are known. This avoids letterboxing on phones, tablets, and landscape
 * windows while preserving the video's aspect ratio.
 */
private class PromoTextureView(
    context: Context,
    private val onFinished: () -> Unit
) : TextureView(context), TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var videoUri: Uri? = null

    init {
        surfaceTextureListener = this
        isOpaque = true
    }

    fun setVideoUri(uri: Uri) {
        videoUri = uri
        if (isAvailable) prepare(uri)
    }

    private fun prepare(uri: Uri) {
        player?.release()
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            setSurface(surface ?: Surface(surfaceTexture))
            setOnPreparedListener { mediaPlayer ->
                applyCenterCrop(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
                mediaPlayer.isLooping = false
                mediaPlayer.start()
            }
            setOnCompletionListener { onFinished() }
            setOnErrorListener { _, _, _ -> onFinished(); true }
            prepareAsync()
        }
    }

    private fun applyCenterCrop(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) return
        val scale = max(width.toFloat() / videoWidth, height.toFloat() / videoHeight)
        val matrix = Matrix().apply {
            setScale(
                videoWidth * scale / width,
                videoHeight * scale / height,
                width / 2f,
                height / 2f
            )
        }
        setTransform(matrix)
    }

    fun release() {
        player?.release()
        player = null
        surface?.release()
        surface = null
    }

    override fun onSurfaceTextureAvailable(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        surface = Surface(texture)
        videoUri?.let(::prepare)
    }

    override fun onSurfaceTextureSizeChanged(texture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        player?.let { applyCenterCrop(it.videoWidth, it.videoHeight) }
    }

    override fun onSurfaceTextureDestroyed(texture: android.graphics.SurfaceTexture): Boolean {
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(texture: android.graphics.SurfaceTexture) = Unit
}
