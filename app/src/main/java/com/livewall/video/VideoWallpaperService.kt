package com.livewall.video

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var player: MediaPlayer? = null
        private val prefs = getSharedPreferences("w", MODE_PRIVATE)
        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "v") restart()
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(listener)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            start(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            try {
                if (visible) player?.start() else player?.pause()
            } catch (_: Exception) {
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stop()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            stop()
            super.onDestroy()
        }

        private fun restart() {
            stop()
            start(surfaceHolder)
        }

        private fun start(holder: SurfaceHolder) {
            val file = File(filesDir, "wallpaper.mp4")
            if (!file.exists() || !holder.surface.isValid) return
            try {
                val mp = MediaPlayer()
                mp.setSurface(holder.surface)
                mp.setDataSource(file.absolutePath)
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                mp.setOnPreparedListener { if (isVisible) it.start() }
                mp.prepareAsync()
                player = mp
            } catch (e: Exception) {
                stop()
            }
        }

        private fun stop() {
            player?.let {
                try { it.stop() } catch (_: Exception) {}
                try { it.release() } catch (_: Exception) {}
            }
            player = null
        }
    }
}
