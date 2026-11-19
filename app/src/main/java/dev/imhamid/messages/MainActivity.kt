package dev.imhamid.messages

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.ui.AppRoot
import dev.imhamid.messages.ui.MessagesTheme

class MainActivity : AppCompatActivity() {
    private var launchAddress by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            val theme by app.settings.theme.collectAsStateWithLifecycle()
            val bubble by app.settings.bubble.collectAsStateWithLifecycle()
            val dynamic by app.settings.dynamic.collectAsStateWithLifecycle()
            val fontIdx by app.settings.fontScale.collectAsStateWithLifecycle()
            val dark = when (theme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                onDispose { }
            }
            MessagesTheme(dark, bubble, dynamic, fontIdx) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppRoot(launchAddress = launchAddress, onLaunchConsumed = { launchAddress = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent?) {
        val data = i?.data ?: return
        if (data.scheme in listOf("sms", "smsto", "mms", "mmsto")) {
            val a = data.schemeSpecificPart.orEmpty().substringBefore('?').trim()
            if (a.isNotEmpty()) launchAddress = a
        }
    }
}
