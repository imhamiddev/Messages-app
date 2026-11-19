package dev.imhamid.messages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.R
import dev.imhamid.messages.data.MessageEntity
import dev.imhamid.messages.data.SCHED_FAILED
import dev.imhamid.messages.data.SCHED_SENT
import dev.imhamid.messages.data.ScheduledEntity
import dev.imhamid.messages.util.DateFmt

@Composable
fun TrashScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val fa = rememberIsFa()
    var menu by remember { mutableStateOf(false) }
    var askEmpty by remember { mutableStateOf(false) }
    var askForever by remember { mutableStateOf<MessageEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(stringResource(R.string.trash_title), onBack) {
                if (items.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.empty_trash)) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                onClick = { menu = false; askEmpty = true }
                            )
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (items.isEmpty()) {
                EmptyState(Icons.Default.Delete, stringResource(R.string.trash_empty), stringResource(R.string.trash_note))
            } else {
                LazyColumn {
                    item {
                        Text(
                            stringResource(R.string.trash_note), Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(items, key = { it.id }) { m ->
                        val name by produceState<String?>(null, m.address) { value = vm.displayName(m.address) }
                        Row(
                            Modifier.animateItem().fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name ?: m.address, isContact = name != null, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name ?: m.address, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    m.body, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    DateFmt.full(m.date, cal, fa), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { vm.restore(listOf(m.id)) }) {
                                Icon(Icons.Default.RestoreFromTrash, stringResource(R.string.restore))
                            }
                            IconButton(onClick = { askForever = m }) {
                                Icon(Icons.Default.DeleteForever, stringResource(R.string.delete_forever), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    if (askEmpty) {
        ConfirmDialog(
            stringResource(R.string.empty_trash_title), stringResource(R.string.empty_trash_body),
            stringResource(R.string.empty_trash),
            onConfirm = { vm.emptyTrash(); askEmpty = false }, onDismiss = { askEmpty = false }
        )
    }
    askForever?.let { m ->
        ConfirmDialog(
            stringResource(R.string.delete_forever_title), stringResource(R.string.delete_forever_body),
            stringResource(R.string.delete_forever),
            onConfirm = { vm.deleteForever(listOf(m.id)); askForever = null }, onDismiss = { askForever = null }
        )
    }
}

@Composable
fun BlockedScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.blockedList.collectAsStateWithLifecycle(initialValue = emptyList())
    val fa = rememberIsFa()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.blocked_title), onBack) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (items.isEmpty()) {
                EmptyState(Icons.Default.Block, stringResource(R.string.blocked_empty))
            } else {
                LazyColumn {
                    items(items, key = { it.threadKey }) { b ->
                        val name by produceState<String?>(null, b.address) { value = vm.displayName(b.address) }
                        Row(
                            Modifier.animateItem().fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name ?: b.address, isContact = name != null, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name ?: DateFmt.digits(b.address, fa), maxLines = 1, style = MaterialTheme.typography.titleSmall)
                                if (name != null) {
                                    Text(
                                        DateFmt.digits(b.address, fa), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { vm.unblock(b.threadKey) }) { Text(stringResource(R.string.action_unblock)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledScreen(vm: MainViewModel, onBack: () -> Unit) {
    val items by vm.scheduledList.collectAsStateWithLifecycle(initialValue = emptyList())
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val fa = rememberIsFa()
    var askCancel by remember { mutableStateOf<ScheduledEntity?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.scheduled_title), onBack) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (items.isEmpty()) {
                EmptyState(Icons.Default.Schedule, stringResource(R.string.scheduled_empty))
            } else {
                LazyColumn {
                    items(items, key = { it.id }) { s ->
                        val name by produceState<String?>(null, s.address) { value = vm.displayName(s.address) }
                        val label = when (s.state) {
                            SCHED_SENT -> R.string.scheduled_sent
                            SCHED_FAILED -> R.string.scheduled_failed
                            else -> R.string.scheduled_pending
                        }
                        Row(
                            Modifier.animateItem().fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(name ?: s.address, isContact = name != null, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name ?: DateFmt.digits(s.address, fa), maxLines = 1, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    s.body, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    DateFmt.full(s.sendAt, cal, fa) + " · " + stringResource(label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (s.state == SCHED_FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { if (s.state == 0) askCancel = s else vm.removeScheduled(s.id) }) {
                                Icon(Icons.Default.Close, stringResource(R.string.scheduled_remove))
                            }
                        }
                    }
                }
            }
        }
    }
    askCancel?.let { s ->
        ConfirmDialog(
            stringResource(R.string.scheduled_cancel_title), stringResource(R.string.scheduled_cancel_body),
            stringResource(R.string.scheduled_remove),
            onConfirm = { vm.removeScheduled(s.id); askCancel = null }, onDismiss = { askCancel = null }
        )
    }
}
