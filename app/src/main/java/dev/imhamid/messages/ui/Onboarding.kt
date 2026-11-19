package dev.imhamid.messages.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box0 {
            EmptyStateInline(
                title = stringResource(if (smsGranted) R.string.onboarding_default_title else R.string.onboarding_title),
                body = stringResource(if (smsGranted) R.string.onboarding_default_body else R.string.onboarding_body)
            )
        }
        Spacer(Modifier.height(24.dp))
        if (!smsGranted) {
            Button(onClick = { permLauncher.launch(perms) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_continue))
            }
        } else {
            Button(onClick = { roleLauncher.launch(defaultSmsIntent(ctx)) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_default_action))
            }
            TextButton(onClick = onSkipDefault) { Text(stringResource(R.string.onboarding_skip)) }
        }
    }
}

@Composable
private fun Box0(content: @Composable () -> Unit) = content()

@Composable
private fun EmptyStateInline(title: String, body: String) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(
            Modifier.padding(bottom = 20.dp)
        ) {
            androidx.compose.material3.Icon(
                Icons.Default.ChatBubbleOutline, null,
                Modifier.padding(8.dp).height(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
