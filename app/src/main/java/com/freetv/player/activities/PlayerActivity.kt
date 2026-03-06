package com.freetv.player.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.freetv.player.R
import com.freetv.player.databinding.ActivityPlayerBinding
import com.freetv.player.models.Channel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
    }

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var channel: Channel? = null
    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen immersive
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channel = intent.getParcelableExtra(EXTRA_CHANNEL)

        setupUI()
        setupPlayer()
    }

    private fun setupUI() {
        val ch = channel ?: return

        binding.tvChannelName.text = ch.name
        binding.tvChannelCategory.text = ch.category.ifBlank { "Live TV" }

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }

        // Hide status bar for immersive experience
        hideSystemUI()
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun setupPlayer() {
        val ch = channel ?: return

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("FreeTV-Player/1.0")

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        binding.playerView.player = player

        // Set up player controls visibility
        binding.playerView.setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
        binding.playerView.useController = true
        binding.playerView.controllerAutoShow = true
        binding.playerView.controllerHideOnTouch = true

        val mediaItem = MediaItem.fromUri(ch.streamUrl)

        // Use HLS source for .m3u8 streams
        val mediaSource = if (ch.streamUrl.contains(".m3u8", ignoreCase = true) ||
            ch.streamUrl.contains("hls", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        player?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBuffering.visibility = View.VISIBLE
                            binding.tvStatus.text = getString(R.string.buffering)
                            binding.tvStatus.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            binding.progressBuffering.visibility = View.GONE
                            binding.tvStatus.visibility = View.GONE
                        }
                        Player.STATE_ENDED -> {
                            binding.tvStatus.text = getString(R.string.stream_ended)
                            binding.tvStatus.visibility = View.VISIBLE
                        }
                        Player.STATE_IDLE -> {
                            binding.progressBuffering.visibility = View.GONE
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.progressBuffering.visibility = View.GONE
                    val msg = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                            getString(R.string.error_network)
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                            getString(R.string.error_stream_unavailable)
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                            getString(R.string.error_decoder)
                        else -> getString(R.string.error_playback, error.errorCodeName)
                    }
                    binding.tvStatus.text = msg
                    binding.tvStatus.visibility = View.VISIBLE

                    Toast.makeText(this@PlayerActivity, msg, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            hideSystemUI()
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            showSystemUI()
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        player?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
