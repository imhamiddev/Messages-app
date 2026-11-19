package dev.imhamid.messages.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.R
import dev.imhamid.messages.data.MessageEntity
import dev.imhamid.messages.data.TYPE_FAILED
import dev.imhamid.messages.data.TYPE_SENDING
import dev.imhamid.messages.sms.Notifier
import dev.imhamid.messages.sms.SimInfo
import dev.imhamid.messages.sms.SimManager
import dev.imhamid.messages.util.CalendarType
import dev.imhamid.messages.util.DateFmt
import dev.imhamid.messages.util.PhoneUtils

@Composable
fun ChatScreen(vm: MainViewModel, address: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val key = remember(address) { PhoneUtils.key(address) }
    val messages by vm.chat(key).collectAsStateWithLifecycle(initialValue = emptyList())
    val pref by vm.chatPref(key).collectAsStateWithLifecycle(initialValue = null)
    val blockedKeys by vm.blockedKeys.collectAsStateWithLifecycle(initialValue = emptySet())
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val defaultSim by vm.defaultSim.collectAsStateWithLifecycle()
    val fa = rememberIsFa()
    val name by produceState<String?>(null, address) { value = vm.displayName(address) }
    val sims = remember { SimManager.list(ctx) }

    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var askDeleteChat by remember { mutableStateOf(false) }
    var askBlock by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var showSchedule by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val effectiveSub = pref?.simSubId ?: defaultSim
    val isBlocked = key in blockedKeys
    val pinned = pref?.pinned == true
    val title = name ?: address

    DisposableEffect(key) {
        vm.repo.activeKey = key
        Notifier.cancel(ctx, key)
        onDispose { if (vm.repo.activeKey == key) vm.repo.activeKey = null }
    }
    LaunchedEffect(messages) {
        if (messages.any { !it.read }) vm.markRead(key)
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(title, isContact = name != null, size = 38.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            if (name != null) {
                                Text(
                                    DateFmt.digits(address, fa), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (pinned) R.string.action_unpin else R.string.action_pin)) },
                                leadingIcon = {
                                    Icon(if (pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin, null)
                                },
                                onClick = { menuOpen = false; vm.pin(listOf(key), !pinned) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(if (isBlocked) R.string.action_unblock else R.string.action_block)) },
                                leadingIcon = { Icon(Icons.Default.Block, null) },
                                onClick = {
                                    menuOpen = false
                                    if (isBlocked) vm.unblock(key) else askBlock = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_chat_menu)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menuOpen = false; askDeleteChat = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                reverseLayout = true
            ) {
                itemsIndexed(messages, key = { _, m -> m.id }) { i, m ->
                    val older = messages.getOrNull(i + 1)
                    Column(Modifier.animateItem()) {
                        if (older == null || !DateFmt.isSameDay(older.date, m.date)) {
                            DayHeader(
                                DateFmt.dayHeader(
                                    m.date, cal, fa,
                                    stringResource(R.string.today), stringResource(R.string.yesterday)
                                )
                            )
                        }
                        Bubble(
                            m = m, fa = fa,
                            onCopy = {
                                clipboard.setText(AnnotatedString(m.body))
                                Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { deleteTarget = m },
                            onResend = { vm.resend(m, effectiveSub) }
                        )
                    }
                }
            }
            Composer(
                text = text,
                onText = { text = it },
                sims = sims,
                effectiveSub = effectiveSub,
                chatSimSet = pref?.simSubId != null,
                onPickSim = { vm.setChatSim(key, it) },
                onSchedule = { showSchedule = true },
                onSend = {
                    vm.send(address, text.trim(), effectiveSub)
                    text = ""
                }
            )
        }
    }

    if (showSchedule) {
        DateTimePickerDialog(
            initialMillis = System.currentTimeMillis() + 60 * 60 * 1000L,
            cal = cal, fa = fa,
            onDismiss = { showSchedule = false },
            onConfirm = { at ->
                vm.schedule(address, text.trim(), at, effectiveSub)
                text = ""
                showSchedule = false
                Toast.makeText(ctx, ctx.getString(R.string.schedule_toast), Toast.LENGTH_SHORT).show()
            }
        )
    }
    deleteTarget?.let { m ->
        ConfirmDialog(
            title = stringResource(R.string.delete_message_title),
            body = stringResource(R.string.delete_message_body),
            confirmText = stringResource(R.string.action_move_to_trash),
            onConfirm = { vm.trashMessages(listOf(m.id)); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
    if (askDeleteChat) {
        ConfirmDialog(
            title = stringResource(R.string.delete_chats_title),
            body = stringResource(R.string.delete_chats_body),
            confirmText = stringResource(R.string.action_move_to_trash),
            onConfirm = { vm.trashThreads(listOf(key)); askDeleteChat = false; onBack() },
            onDismiss = { askDeleteChat = false }
        )
    }
    if (askBlock) {
        ConfirmDialog(
            title = stringResource(R.string.block_title),
            body = stringResource(R.string.block_body),
            confirmText = stringResource(R.string.action_block),
            onConfirm = { vm.block(key, address); askBlock = false; onBack() },
            onDismiss = { askBlock = false }
        )
    }
}

@Composable
private fun DayHeader(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Bubble(m: MessageEntity, fa: Boolean, onCopy: () -> Unit, onDelete: () -> Unit, onResend: () -> Unit) {
    val out = !m.incoming
    val src = remember { MutableInteractionSource() }
    var menu by remember { mutableStateOf(false) }
    val shape = if (out) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val container = if (out) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (out) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                shape = shape,
                color = container,
                contentColor = content,
                modifier = Modifier.widthIn(max = 300.dp).pressScale(src, 0.97f).clip(shape)
                    .clickable(interactionSource = src, indication = null) { menu = true }
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                    SelectionContainer { Text(m.body, style = MaterialTheme.typography.bodyLarge) }
                    Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                        if (m.type == TYPE_FAILED) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.error)
                            Text(
                                " " + stringResource(R.string.not_sent), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            DateFmt.time(m.date, fa), style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.7f)
                        )
                        if (out && m.type != TYPE_FAILED) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                if (m.type == TYPE_SENDING) Icons.Default.Schedule else Icons.Default.Check,
                                null, Modifier.size(13.dp), tint = content.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_copy)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                    onClick = { menu = false; onCopy() }
                )
                if (m.type == TYPE_FAILED) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_resend)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null) },
                        onClick = { menu = false; onResend() }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete)) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { menu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun Composer(
    text: String,
    onText: (String) -> Unit,
    sims: List<SimInfo>,
    effectiveSub: Int,
    chatSimSet: Boolean,
    onPickSim: (Int?) -> Unit,
    onSchedule: () -> Unit,
    onSend: () -> Unit
) {
    val canSend = text.isNotBlank()
    val sendScale by animateFloatAsState(
        if (canSend) 1f else 0.82f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "send-scale"
    )
    val sendBg by animateColorAsState(
        if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "send-bg"
    )
    val sendFg by animateColorAsState(
        if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
        label = "send-fg"
    )
    val sendSrc = remember { MutableInteractionSource() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (sims.size > 1) {
                    SimChip(sims, effectiveSub, chatSimSet, onPickSim)
                }
                TextField(
                    value = text, onValueChange = onText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.type_message)) },
                    maxLines = 6,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
                IconButton(onClick = onSchedule, enabled = canSend) {
                    Icon(Icons.Default.Schedule, stringResource(R.string.schedule))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(48.dp)
                .graphicsLayer { scaleX = sendScale; scaleY = sendScale }
                .pressScale(sendSrc)
                .clip(CircleShape).background(sendBg)
                .clickable(interactionSource = sendSrc, indication = null, enabled = canSend) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), tint = sendFg)
        }
    }
}

@Composable
private fun SimChip(sims: List<SimInfo>, effectiveSub: Int, chatSimSet: Boolean, onPick: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = sims.firstOrNull { it.subId == effectiveSub }
    val fa = rememberIsFa()
    Box(Modifier.padding(start = 6.dp, bottom = 6.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.clip(CircleShape).clickable { open = true }
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SimCard, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(4.dp))
                AnimatedContent(targetState = current?.slot ?: 0, label = "sim-chip") { slot ->
                    Text(
                        if (slot > 0) DateFmt.digits(slot.toString(), fa) else stringResource(R.string.sim_auto),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center
                    )
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sim_default_option)) },
                onClick = { open = false; onPick(null) },
                trailingIcon = { if (!chatSimSet) Icon(Icons.Default.Check, null) }
            )
            sims.forEach { s ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sim_label, s.slot) + " · " + s.name) },
                    onClick = { open = false; onPick(s.subId) },
                    trailingIcon = { if (chatSimSet && s.subId == effectiveSub) Icon(Icons.Default.Check, null) }
                )
            }
        }
    }
}
