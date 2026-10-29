package com.livewall.video

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            text = "Video Wallpaper"
            textSize = 26f
        })
        root.addView(TextView(this).apply {
            text = "1. Pick your MP4 video\n" +
                "2. Tap Set as wallpaper\n" +
                "3. Choose Home and lock screens, then confirm"
            setPadding(0, pad / 2, 0, pad / 2)
        })
        status = TextView(this).apply { setPadding(0, 0, 0, pad / 2) }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Pick MP4 video"
            setOnClickListener { pick() }
        })
        root.addView(Button(this).apply {
            text = "Set as wallpaper"
            setOnClickListener { setWallpaper() }
        })
        setContentView(root)
        refreshStatus()
    }

    private fun videoFile() = File(filesDir, "wallpaper.mp4")

    private fun refreshStatus() {
        val f = videoFile()
        status.text = if (f.exists()) {
            "Video ready (${f.length() / 1024 / 1024} MB)"
        } else {
            "No video chosen yet"
        }
    }

    private fun pick() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(i, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != 1 || resultCode != RESULT_OK || uri == null) return
        status.text = "Copying video..."
        Thread {
            try {
                val tmp = File(filesDir, "wallpaper.tmp")
                contentResolver.openInputStream(uri)!!.use { input ->
                    tmp.outputStream().use { out -> input.copyTo(out) }
                }
                tmp.renameTo(videoFile())
                getSharedPreferences("w", MODE_PRIVATE).edit()
                    .putLong("v", System.currentTimeMillis()).apply()
                runOnUiThread {
                    refreshStatus()
                    Toast.makeText(this, "Video saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Failed: ${e.message}" }
            }
        }.start()
    }

    private fun setWallpaper() {
        if (!videoFile().exists()) {
            Toast.makeText(this, "Pick a video first", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(
                Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this, VideoWallpaperService::class.java)
                )
            )
        } catch (e: Exception) {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }
    }
}
