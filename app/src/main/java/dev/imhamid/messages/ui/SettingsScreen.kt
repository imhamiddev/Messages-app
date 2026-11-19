package dev.imhamid.messages.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.R
import dev.imhamid.messages.sms.SimManager
import dev.imhamid.messages.util.CalendarType
import dev.imhamid.messages.util.DateFmt

fun defaultSmsIntent(ctx: Context): Intent =
    if (Build.VERSION.SDK_INT >= 29) {
        ctx.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_SMS)
    } else {
        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, ctx.packageName)
    }

@Composable
private fun Section(title: String, index: Int, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().entrance(index).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val defaultSim by vm.defaultSim.collectAsStateWithLifecycle()
    val importing by vm.repo.importing.collectAsStateWithLifecycle()
    val imported by vm.repo.imported.collectAsStateWithLifecycle()
    val bubble by vm.bubble.collectAsStateWithLifecycle()
    val fa = rememberIsFa()

    var tick by remember { mutableIntStateOf(0) }
    val isDefault = remember(tick) { vm.repo.isDefaultSms() }
    var phoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }
    val sims = remember(phoneGranted) { SimManager.list(ctx) }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { phoneGranted = it }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    val langTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-')
    val langIndex = when (langTag) { "fa" -> 1; "en" -> 2; else -> 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.settings_title), onBack) }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())) {
            Section(stringResource(R.string.settings_language), 0) {
                Segmented(
                    listOf(stringResource(R.string.lang_system), stringResource(R.string.lang_fa), stringResource(R.string.lang_en)),
                    langIndex
                ) {
                    val tags = when (it) { 1 -> "fa"; 2 -> "en"; else -> "" }
                    AppCompatDelegate.setApplicationLocales(
                        if (tags.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tags)
                    )
                }
            }
            Section(stringResource(R.string.settings_calendar), 1) {
                Segmented(
                    listOf(stringResource(R.string.cal_jalali), stringResource(R.string.cal_gregorian)),
                    if (cal == CalendarType.JALALI) 0 else 1
                ) { vm.settings.setCalendar(if (it == 0) CalendarType.JALALI else CalendarType.GREGORIAN) }
                Spacer(Modifier.height(8.dp))
                Text(
                    DateFmt.full(System.currentTimeMillis(), cal, fa),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Section(stringResource(R.string.settings_theme), 2) {
                Segmented(
                    listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                    theme
                ) { vm.settings.setTheme(it) }
            }
            Section(stringResource(R.string.settings_gradient), 3) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BubbleStyles.forEachIndexed { i, colors ->
                        val brush = if (colors.size > 1) Brush.linearGradient(colors) else SolidColor(colors.first())
                        Swatch(bubble == i, brush) { vm.settings.setBubble(i) }
                    }
                }
            }
            Section(stringResource(R.string.settings_sim), 3) {
                if (!phoneGranted) {
                    Text(
                        stringResource(R.string.sim_permission_hint),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE) }) {
                        Text(stringResource(R.string.action_allow))
                    }
                } else {
                    SimOption(stringResource(R.string.sim_system_default), defaultSim < 0) { vm.settings.setDefaultSim(-1) }
                    sims.forEach { s ->
                        SimOption(
                            stringResource(R.string.sim_label, s.slot) + " · " + s.name,
                            defaultSim == s.subId
                        ) { vm.settings.setDefaultSim(s.subId) }
                    }
                }
            }
            Section(stringResource(R.string.settings_default_app), 4) {
                Text(
                    stringResource(if (isDefault) R.string.default_app_yes else R.string.default_app_no),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isDefault) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { roleLauncher.launch(defaultSmsIntent(ctx)) }) {
                        Text(stringResource(R.string.set_default))
                    }
                }
            }
            Section(stringResource(R.string.settings_import), 5) {
                Text(
                    stringResource(R.string.import_hint),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { vm.runImport() }, enabled = !importing) {
                        Text(stringResource(R.string.import_now))
                    }
                    if (importing) {
                        Text(stringResource(R.string.importing, imported), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SimOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}
