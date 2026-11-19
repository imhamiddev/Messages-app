package dev.imhamid.messages.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.imhamid.messages.R
import dev.imhamid.messages.data.Contact
import dev.imhamid.messages.util.DateFmt
import dev.imhamid.messages.util.PhoneUtils
import kotlinx.coroutines.delay

@Composable
fun NewChatScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val fa = rememberIsFa()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val focus = remember { FocusRequester() }
    val isNumber = PhoneUtils.looksLikeNumber(query)

    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(query, granted) {
        delay(120) // debounce while typing
        results = if (granted) vm.searchContacts(query) else emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 12.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                TextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    placeholder = { Text(stringResource(R.string.new_chat_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (isNumber) onOpen(PhoneUtils.clean(query)) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
            if (isNumber) {
                item(key = "number") {
                    ResultRow(
                        modifier = Modifier.animateItem(),
                        title = stringResource(R.string.send_to_number, DateFmt.digits(PhoneUtils.clean(query), fa)),
                        subtitle = null, isContact = false, send = true,
                        onClick = { onOpen(PhoneUtils.clean(query)) }
                    )
                }
            }
            if (!granted) {
                item(key = "perm") {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.contacts_permission_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.size(8.dp))
                        Button(onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text(stringResource(R.string.action_allow))
                        }
                    }
                }
            } else if (results.isEmpty() && query.isNotBlank() && !isNumber) {
                item(key = "none") {
                    Text(
                        stringResource(R.string.no_results), Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(results, key = { PhoneUtils.key(it.number) }) { c ->
                ResultRow(
                    modifier = Modifier.animateItem(),
                    title = c.name, subtitle = DateFmt.digits(c.number, fa), isContact = true, send = false,
                    onClick = { onOpen(PhoneUtils.clean(c.number)) }
                )
            }
        }
    }
}

@Composable
private fun ResultRow(
    modifier: Modifier,
    title: String,
    subtitle: String?,
    isContact: Boolean,
    send: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (send) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        } else Avatar(title, isContact)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle, maxLines = 1, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
