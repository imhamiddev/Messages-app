package dev.imhamid.messages.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.imhamid.messages.R

@Composable
fun Onboarding(smsGranted: Boolean, onChanged: () -> Unit, onSkipDefault: () -> Unit) {
    val ctx = LocalContext.current
    val perms = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onChanged() }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onSkipDefault() // answered either way; don't nag again
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.entrance(0).graphicsLayer { scaleX = scale; scaleY = scale }
                .size(112.dp).clip(CircleShape).background(accentBrush()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(52.dp), tint = onAccent())
        }
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(if (smsGranted) R.string.onboarding_default_title else R.string.onboarding_title),
            Modifier.entrance(1), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (smsGranted) R.string.onboarding_default_body else R.string.onboarding_body),
            Modifier.entrance(2), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        if (!smsGranted) {
            Button(onClick = { permLauncher.launch(perms) }, modifier = Modifier.fillMaxWidth().entrance(3)) {
                Text(stringResource(R.string.onboarding_continue))
            }
        } else {
            Button(onClick = { roleLauncher.launch(defaultSmsIntent(ctx)) }, modifier = Modifier.fillMaxWidth().entrance(3)) {
                Text(stringResource(R.string.onboarding_default_action))
            }
            TextButton(onClick = onSkipDefault, modifier = Modifier.entrance(4)) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}
