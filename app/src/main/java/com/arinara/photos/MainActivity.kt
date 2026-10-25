// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.arinara.photos.theme.PhotosTheme
import com.arinara.photos.ui.home.HomeScreen

enum class PipCoreAction {
    GO_LEFT,
    PLAY_PAUSE,
    GO_RIGHT
}

class MainActivity : ComponentActivity() {

    private var isInSystemPipMode by mutableStateOf(false)
    private var pipActionTick by mutableIntStateOf(0)
    private var lastPipAction by mutableStateOf<PipCoreAction?>(null)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_PIP_CONTROL) return
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, -1)) {
                CONTROL_GO_LEFT -> {
                    lastPipAction = PipCoreAction.GO_LEFT
                    pipActionTick++
                }
                CONTROL_PLAY_PAUSE -> {
                    lastPipAction = PipCoreAction.PLAY_PAUSE
                    pipActionTick++
                }
                CONTROL_GO_RIGHT -> {
                    lastPipAction = PipCoreAction.GO_RIGHT
                    pipActionTick++
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            ContextCompat.registerReceiver(
                this,
                pipReceiver,
                IntentFilter(ACTION_PIP_CONTROL),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Throwable) {
        }

        setContent {
            PhotosTheme {
                HomeScreen(
                    isInSystemPipMode = isInSystemPipMode,
                    pipActionTick = pipActionTick,
                    lastPipAction = lastPipAction,
                    onRequestPipMode = { triggerPipMode() }
                )
            }
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInSystemPipMode = isInPictureInPictureMode
    }

    private fun triggerPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val actions = listOf(
                    buildRemoteAction(
                        requestCode = CONTROL_GO_LEFT,
                        iconRes = android.R.drawable.ic_media_rew,
                        title = "Go Left",
                        controlType = CONTROL_GO_LEFT
                    ),
                    buildRemoteAction(
                        requestCode = CONTROL_PLAY_PAUSE,
                        iconRes = android.R.drawable.ic_media_play,
                        title = "Play / Pause",
                        controlType = CONTROL_PLAY_PAUSE
                    ),
                    buildRemoteAction(
                        requestCode = CONTROL_GO_RIGHT,
                        iconRes = android.R.drawable.ic_media_ff,
                        title = "Go Right",
                        controlType = CONTROL_GO_RIGHT
                    )
                )
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setActions(actions)
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildRemoteAction(
        requestCode: Int,
        iconRes: Int,
        title: String,
        controlType: Int
    ): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(packageName)
            putExtra(EXTRA_CONTROL_TYPE, controlType)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags)
        val icon = Icon.createWithResource(this, iconRes)
        return RemoteAction(icon, title, title, pendingIntent)
    }

    companion object {
        private const val ACTION_PIP_CONTROL = "com.arinara.photos.ACTION_PIP_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "extra_control_type"
        private const val CONTROL_GO_LEFT = 1
        private const val CONTROL_PLAY_PAUSE = 2
        private const val CONTROL_GO_RIGHT = 3
    }
}
