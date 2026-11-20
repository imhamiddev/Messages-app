package dev.imhamid.messages.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableIntStateOf
import kotlin.math.hypot
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.layout.onSizeChanged
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.imhamid.messages.util.LinkKind
import dev.imhamid.messages.util.LinkToken
import dev.imhamid.messages.util.Smart
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.provider.ContactsContract
import android.net.Uri
import android.content.Intent
import android.content.Context
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import dev.imhamid.messages.util.DateFmt
import dev.imhamid.messages.util.PhoneUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: MainViewModel, address: String, focusId: Long = -1L, onBack: () -> Unit) {
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
    val openedAt = remember { System.currentTimeMillis() }
    val played = remember { mutableSetOf<Long>() }

    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var askDeleteChat by remember { mutableStateOf(false) }
    var askBlock by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var selectingId by remember { mutableStateOf<Long?>(null) }
    var showSchedule by remember { mutableStateOf(false) }
    var contactChoice by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    val listState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val dens = LocalDensity.current
    var composerH by remember { mutableStateOf(64.dp) }
    val uiScope = rememberCoroutineScope()
    val jumpHaptics = LocalHapticFeedback.current
    val atBottom by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 200 } }
    var newCount by remember { mutableIntStateOf(0) }
    var lastTopId by remember { mutableStateOf<Long?>(null) }
    var flashId by remember { mutableStateOf<Long?>(null) }
    var focusDone by remember { mutableStateOf(false) }

    fun appendContact(name: String, number: String) {
        text = (if (text.isBlank()) "" else text.trimEnd() + "\n") + name + "\n" + number
    }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val c = readContact(ctx, uri)
            when {
                c == null || c.second.isEmpty() ->
                    Toast.makeText(ctx, ctx.getString(R.string.contact_no_number), Toast.LENGTH_SHORT).show()
                c.second.size == 1 -> appendContact(c.first, c.second[0])
                else -> contactChoice = c
            }
        }
    }

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
        val top = messages.firstOrNull()
        if (top != null && top.id != lastTopId) {
            val first = lastTopId == null
            lastTopId = top.id
            // Don't yank the view down while the user is reading older messages.
            if (!first) {
                if (!top.incoming || atBottom) listState.animateScrollToItem(0) else newCount++
            }
        }
        if (messages.any { !it.read }) vm.markRead(key)
    }
    LaunchedEffect(atBottom) { if (atBottom) newCount = 0 }
    LaunchedEffect(messages.isNotEmpty(), focusId) {
        if (focusId >= 0 && !focusDone && messages.isNotEmpty()) {
            val idx = messages.indexOfFirst { it.id == focusId }
            if (idx >= 0) {
                focusDone = true
                delay(350)
                listState.animateScrollToItem(idx)
                flashId = focusId
                delay(1700)
                flashId = null
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.ime),
            topBar = {
              Box(Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp).glass(RoundedCornerShape(26.dp), hazeState)) {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(title, isContact = name != null, size = 38.dp, address = address, sharedKey = key)
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
                            AppDropdown(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                                menuItem(
                                    text = stringResource(if (pinned) R.string.action_unpin else R.string.action_pin),
                                    icon = if (pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                    onClick = { menuOpen = false; vm.pin(listOf(key), !pinned) }
                                )
                                menuItem(
                                    text = stringResource(if (isBlocked) R.string.action_unblock else R.string.action_block),
                                    icon = Icons.Default.Block,
                                    onClick = {
                                        menuOpen = false
                                        if (isBlocked) vm.unblock(key) else askBlock = true
                                    }
                                )
                                menuItem(
                                    text = stringResource(R.string.delete_chat_menu),
                                    icon = Icons.Default.Delete, danger = true,
                                    onClick = { menuOpen = false; askDeleteChat = true }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
              }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
                LazyColumn(
                    Modifier.fillMaxSize().haze(hazeState), state = listState, reverseLayout = true,
                    contentPadding = PaddingValues(top = pad.calculateTopPadding() + 4.dp, bottom = composerH + 4.dp)
                ) {
                    itemsIndexed(messages, key = { _, m -> m.id }) { i, m ->
                        val older = messages.getOrNull(i + 1)
                        val newer = messages.getOrNull(i - 1)
                        val gap = 5 * 60 * 1000L
                        val groupedOlder = older != null && older.incoming == m.incoming &&
                            DateFmt.isSameDay(older.date, m.date) && m.date - older.date < gap
                        val groupedNewer = newer != null && newer.incoming == m.incoming &&
                            DateFmt.isSameDay(newer.date, m.date) && newer.date - m.date < gap
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
                                m = m, fa = fa, sims = sims, flash = flashId == m.id,
                                animateIn = m.date >= openedAt && m.id !in played,
                                onPlayed = { played.add(m.id) },
                                groupedOlder = groupedOlder, groupedNewer = groupedNewer,
                                selecting = selectingId == m.id,
                                onSelectDone = { if (selectingId == m.id) selectingId = null },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(m.body))
                                    Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                                },
                                onSelectText = { selectingId = m.id },
                                onDelete = { deleteTarget = m },
                                onResend = { vm.resend(m, effectiveSub) }
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !atBottom,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = composerH + 10.dp),
                    enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f), initialScale = 0.3f) + fadeIn(tween(120)),
                    exit = scaleOut(tween(140), targetScale = 0.4f) + fadeOut(tween(120))
                ) {
                    JumpButton(newCount, fa) {
                        jumpHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        uiScope.launch {
                            if (listState.firstVisibleItemIndex > 15) listState.scrollToItem(10)
                            listState.animateScrollToItem(0)
                        }
                    }
                }
                Composer(
                    modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { composerH = with(dens) { it.height.toDp() } },
                    haze = hazeState,
                    text = text,
                    onText = { text = it },
                    sims = sims,
                    effectiveSub = effectiveSub,
                    chatSimSet = pref?.simSubId != null,
                    onPickSim = { vm.setChatSim(key, it) },
                    onSchedule = { showSchedule = true },
                    onAttach = { pickContact.launch(null) },
                    onSend = {
                        vm.send(address, text.trim(), effectiveSub)
                        text = ""
                    }
                )
            }
        }
    }

    contactChoice?.let { (cname, numbers) ->
        AlertDialog(
            onDismissRequest = { contactChoice = null },
            confirmButton = {},
            title = { Text(stringResource(R.string.contact_pick_title)) },
            text = {
                Column {
                    numbers.forEach { n ->
                        Text(
                            n,
                            Modifier.fillMaxWidth().clickable { appendContact(cname, n); contactChoice = null }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        )
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
        Surface(shape = CircleShape, color = Color.Transparent, modifier = Modifier.glass(CircleShape)) {
            Text(
                label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Read-only text field that starts with the whole message selected, so the system toolbar offers Copy. */
@Composable
private fun SelectableBody(text: String, color: Color, out: Boolean, initial: TextRange?, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(text, initial ?: TextRange(0, text.length))) }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler { onDone() }
    val primary = MaterialTheme.colorScheme.primary
    // Outgoing bubbles are coloured, so the highlight is a dark veil; incoming ones get an accent veil.
    val selColors = if (out) TextSelectionColors(handleColor = Color.White, backgroundColor = Color.Black.copy(alpha = 0.40f))
    else TextSelectionColors(handleColor = primary, backgroundColor = primary.copy(alpha = 0.38f))
    CompositionLocalProvider(LocalTextSelectionColors provides selColors) {
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        readOnly = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = color, textDirection = TextDirection.Content),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = Modifier.focusRequester(focus).onFocusChanged {
            if (it.isFocused) hadFocus = true else if (hadFocus) onDone()
        }
    )
    }
}

private val digitRun = Regex("[0-9\\u0660-\\u0669\\u06F0-\\u06F9]+")

/** Caret offset in the bubble text under a touch point, or null when the touch is outside the text. */
private fun offsetAt(
    pos: Offset,
    box: LayoutCoordinates?,
    textCoords: LayoutCoordinates?,
    layout: TextLayoutResult?,
    length: Int
): Int? {
    if (box == null || textCoords == null || layout == null || !box.isAttached || !textCoords.isAttached) return null
    val local = textCoords.localPositionOf(box, pos)
    if (local.x < -16f || local.y < -8f || local.x > layout.size.width + 16f || local.y > layout.size.height + 8f) return null
    return layout.getOffsetForPosition(local).coerceIn(0, length)
}

/** The run of digits (Latin, Arabic-Indic or Persian) under a touch point, or null. */
private fun numberAt(
    pos: Offset, box: LayoutCoordinates?, textCoords: LayoutCoordinates?, layout: TextLayoutResult?, body: String
): TextRange? {
    val off = offsetAt(pos, box, textCoords, layout, body.length) ?: return null
    for (m in digitRun.findAll(body)) {
        if (off >= m.range.first && off <= m.range.last + 1) return TextRange(m.range.first, m.range.last + 1)
    }
    return null
}

private fun tokenAt(
    pos: Offset, box: LayoutCoordinates?, textCoords: LayoutCoordinates?, layout: TextLayoutResult?,
    body: String, tokens: List<LinkToken>
): LinkToken? {
    if (tokens.isEmpty()) return null
    val off = offsetAt(pos, box, textCoords, layout, body.length) ?: return null
    return tokens.firstOrNull { off >= it.start && off <= it.end }
}

private fun openToken(ctx: Context, t: LinkToken) {
    val intent = if (t.kind == LinkKind.URL) {
        val u = if (t.value.contains("://")) t.value else "https://" + t.value
        Intent(Intent.ACTION_VIEW, Uri.parse(u)).addCategory(Intent.CATEGORY_BROWSABLE)
    } else {
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(t.value)))
    }
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(ctx, ctx.getString(R.string.link_open_failed), Toast.LENGTH_SHORT).show()
    }
}

/** Name and phone numbers of a contact picked with the system picker. */
private fun readContact(ctx: Context, uri: Uri): Pair<String, List<String>>? {
    var id: String? = null
    var name = ""
    ctx.contentResolver.query(
        uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME), null, null, null
    )?.use { c ->
        if (c.moveToFirst()) {
            id = c.getString(0)
            name = c.getString(1).orEmpty()
        }
    }
    val cid = id ?: return null
    val numbers = LinkedHashMap<String, String>()
    try {
        ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(cid), null
        )?.use { c ->
            while (c.moveToNext()) {
                val n = c.getString(0).orEmpty().trim()
                if (n.isNotEmpty()) numbers.putIfAbsent(PhoneUtils.clean(n).ifEmpty { n }, n)
            }
        }
    } catch (_: SecurityException) {
    }
    return name to numbers.values.toList()
}

@Composable
private fun Bubble(
    m: MessageEntity,
    fa: Boolean,
    sims: List<SimInfo>,
    flash: Boolean = false,
    animateIn: Boolean,
    onPlayed: () -> Unit,
    groupedOlder: Boolean,
    groupedNewer: Boolean,
    selecting: Boolean,
    onSelectDone: () -> Unit,
    onCopy: () -> Unit,
    onSelectText: () -> Unit,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    val out = !m.incoming
    val grad = LocalGradient.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    var showSim by remember { mutableStateOf(false) }
    val simSlot = if (m.subId >= 0) sims.firstOrNull { it.subId == m.subId }?.slot ?: 0 else 0
    var linkTarget by remember { mutableStateOf<LinkToken?>(null) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var numberSel by remember { mutableStateOf<TextRange?>(null) }
    val rscope = rememberCoroutineScope()
    val rip = remember { Animatable(0f) }
    var ripCenter by remember { mutableStateOf(Offset.Zero) }
    val flashA by animateFloatAsState(if (flash) 1f else 0f, tween(500), label = "flash")
    val shake = remember { Animatable(0f) }
    var prevType by remember { mutableIntStateOf(m.type) }
    LaunchedEffect(m.type) {
        if (m.type == TYPE_FAILED && prevType != TYPE_FAILED) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            for (v in listOf(-14f, 12f, -9f, 6f, -3f, 0f)) shake.animateTo(v, tween(55))
        }
        prevType = m.type
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.25f, 0.75f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse-a"
    )
    val sendA by animateFloatAsState(if (m.type == TYPE_SENDING) 1f else 0f, tween(300), label = "send-a")
    val failA by animateFloatAsState(if (m.type == TYPE_FAILED) 1f else 0f, tween(300), label = "fail-a")
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) {
            appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
            onPlayed()
        }
    }
    // Consecutive messages from the same side (within 5 minutes) form a visual group.
    val big = 20.dp
    val join = 4.dp
    val sideTop by animateDpAsState(
        if (groupedOlder) join else big, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "corner-top"
    )
    val sideBottom by animateDpAsState(
        if (groupedNewer) join else big, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "corner-bottom"
    )
    // Only the last bubble of a run shows time/status, so a run reads as one block.
    val showMeta = !groupedNewer || m.type == TYPE_FAILED || m.type == TYPE_SENDING
    val shape = if (out) RoundedCornerShape(big, sideTop, sideBottom, big) else RoundedCornerShape(sideTop, big, big, sideBottom)
    val useGradient = out && grad != null
    val container = when {
        useGradient -> Color.Transparent
        out -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        useGradient -> Color.White
        out -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val bgMod = if (useGradient) Modifier.background(Brush.linearGradient(grad!!), shape) else Modifier

    val tokens = remember(m.body) { Smart.tokens(m.body) }
    val linkColor = if (out) content else MaterialTheme.colorScheme.primary
    val emoji = remember(m.body) { Emoji.spans(ctx, m.body) }
    val inline = remember(emoji) {
        emoji.mapIndexed { i, sp ->
            "e$i" to InlineTextContent(Placeholder(1.25.em, 1.25.em, PlaceholderVerticalAlign.TextCenter)) { EmojiImage(sp.file) }
        }.toMap()
    }
    val annotated = remember(m.body, linkColor, emoji) {
        buildAnnotatedString {
            var pos = 0
            emoji.forEachIndexed { i, sp ->
                append(m.body.substring(pos, sp.start))
                appendInlineContent("e$i", m.body.substring(sp.start, sp.end))
                pos = sp.end
            }
            append(m.body.substring(pos))
            tokens.forEach { addStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), it.start, it.end) }
        }
    }
    val code = remember(m.body, out) { if (out) null else Smart.verificationCode(m.body) }
    val ringColor = when {
        failA > 0.01f -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f * failA)
        sendA > 0.01f -> (if (out) content else MaterialTheme.colorScheme.primary).copy(alpha = pulse * sendA)
        else -> Color.Transparent
    }

    Row(
        Modifier.fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = if (groupedOlder) 2.dp else 10.dp, bottom = 0.dp)
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                val sc = 0.85f + 0.15f * appear.value
                scaleX = sc
                scaleY = sc
                translationY = (1f - appear.value) * 56f
                translationX = (1f - appear.value) * (if (out) 40f else -40f) + shake.value
                transformOrigin = TransformOrigin(if (out) 1f else 0f, 1f)
            },
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
        Box {
            Surface(
                shape = shape,
                color = container,
                contentColor = content,
                modifier = Modifier.widthIn(max = 300.dp).pressScale(src, 0.97f).then(bgMod).clip(shape)
                    .drawWithContent {
                        drawContent()
                        val r = rip.value
                        if (r > 0.001f && r < 0.999f) {
                            val diag = hypot(size.width, size.height)
                            drawCircle(content.copy(alpha = 0.22f * (1f - r)), radius = diag * r * 1.1f, center = ripCenter)
                        }
                        if (flashA > 0.01f) drawRect(content.copy(alpha = 0.28f * flashA))
                    }
                    .then(if (ringColor.alpha > 0.01f) Modifier.border(1.5.dp, ringColor, shape) else Modifier)
                    .onGloballyPositioned { boxCoords = it }
                    .then(
                        if (selecting) Modifier else Modifier.pointerInput(m.id) {
                            detectTapGestures(
                                onPress = { off ->
                                    ripCenter = off
                                    rscope.launch { rip.snapTo(0f); rip.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
                                    val press = PressInteraction.Press(off)
                                    src.emit(press)
                                    val released = tryAwaitRelease()
                                    src.emit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                                },
                                onTap = { pos ->
                                    val t = tokenAt(pos, boxCoords, textCoords, textLayout, m.body, tokens)
                                    if (t != null) openToken(ctx, t) else if (simSlot > 0) showSim = !showSim
                                },
                                onLongPress = { pos ->
                                    val lt = tokenAt(pos, boxCoords, textCoords, textLayout, m.body, tokens)
                                    val r = numberAt(pos, boxCoords, textCoords, textLayout, m.body)
                                    if (lt != null && lt.kind == LinkKind.URL) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        linkTarget = lt
                                    } else if (r != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        numberSel = r
                                        onSelectText()
                                    } else {
                                        menu = true
                                    }
                                }
                            )
                        }
                    )
            ) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = if (showMeta) 6.dp else 8.dp).animateContentSize()) {
                    if (selecting) SelectableBody(m.body, content, out, numberSel) { numberSel = null; onSelectDone() }
                    else Text(
                        annotated,
                        inlineContent = inline,
                        modifier = Modifier.onGloballyPositioned { textCoords = it },
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                        onTextLayout = { textLayout = it }
                    )
                    val linkTok = tokens.firstOrNull { it.kind == LinkKind.URL }
                    if (linkTok != null && !selecting) LinkCard(linkTok.value, content) { openToken(ctx, linkTok) }
                    if (code != null && !selecting) {
                        var copied by remember { mutableStateOf(false) }
                        LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
                        Row(
                            Modifier.padding(top = 6.dp).clip(CircleShape)
                                .background(content.copy(alpha = 0.14f))
                                .clickable {
                                    clipboard.setText(AnnotatedString(code))
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    copied = true
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedContent(
                                targetState = copied,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f), initialScale = 0.6f) + fadeIn(tween(120))) togetherWith
                                        (scaleOut(tween(90), targetScale = 0.6f) + fadeOut(tween(90)))
                                },
                                label = "copy-chip"
                            ) { c ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (c) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (c) stringResource(R.string.code_copied) else stringResource(R.string.copy_code, code),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                    if (showMeta) Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
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
                            AnimatedContent(
                                targetState = m.type == TYPE_SENDING, label = "status",
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = 0.4f, stiffness = 500f), initialScale = 0.2f) + fadeIn(tween(120))) togetherWith
                                        (scaleOut(tween(120), targetScale = 0.4f) + fadeOut(tween(100)))
                                }
                            ) { sending ->
                                Icon(
                                    if (sending) Icons.Default.Schedule else Icons.Default.Check,
                                    null, Modifier.size(13.dp), tint = content.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            AppDropdown(expanded = linkTarget != null, onDismiss = { linkTarget = null }, alignEnd = out) {
                val t = linkTarget
                if (t != null) {
                    menuItem(
                        text = stringResource(if (t.kind == LinkKind.URL) R.string.link_open else R.string.link_call),
                        icon = if (t.kind == LinkKind.URL) Icons.Default.OpenInBrowser else Icons.Default.Call,
                        onClick = { linkTarget = null; openToken(ctx, t) }
                    )
                    menuItem(
                        text = stringResource(R.string.link_copy), icon = Icons.Default.ContentCopy,
                        onClick = {
                            linkTarget = null
                            clipboard.setText(AnnotatedString(t.value))
                            Toast.makeText(ctx, ctx.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            AppDropdown(expanded = menu, onDismiss = { menu = false }, alignEnd = out) {
                menuItem(text = stringResource(R.string.action_copy), icon = Icons.Default.ContentCopy, onClick = { menu = false; onCopy() })
                menuItem(
                    text = stringResource(R.string.action_select_text), icon = Icons.Default.SelectAll,
                    onClick = { menu = false; numberSel = null; onSelectText() }
                )
                if (m.type == TYPE_FAILED) {
                    menuItem(
                        text = stringResource(R.string.action_resend), icon = Icons.AutoMirrored.Filled.Send,
                        onClick = { menu = false; onResend() }
                    )
                }
                menuItem(
                    text = stringResource(R.string.action_delete), icon = Icons.Default.Delete, danger = true,
                    onClick = { menu = false; onDelete() }
                )
            }
        }
        AnimatedVisibility(
            visible = showSim && simSlot > 0,
            enter = fadeIn(tween(160)) + expandVertically(spring(dampingRatio = 0.7f)) + scaleIn(spring(dampingRatio = 0.5f), initialScale = 0.5f),
            exit = fadeOut(tween(110)) + shrinkVertically(tween(140)) + scaleOut(tween(110), targetScale = 0.6f)
        ) {
            SimBadge(simSlot, fa)
        }
        }
    }
}

/** Tiny SIM-card icon with the slot number inside. */
@Composable
private fun SimBadge(slot: Int, fa: Boolean) {
    Box(Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp).size(18.dp), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.SimCard, null, Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            DateFmt.digits(slot.toString(), fa),
            Modifier.padding(top = 2.dp),
            fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.background
        )
    }
}

@Composable
private fun Composer(
    modifier: Modifier = Modifier,
    haze: HazeState? = null,
    text: String,
    onText: (String) -> Unit,
    sims: List<SimInfo>,
    effectiveSub: Int,
    chatSimSet: Boolean,
    onPickSim: (Int?) -> Unit,
    onSchedule: () -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val canSend = text.isNotBlank()
    val sendAlpha by animateFloatAsState(if (canSend) 1f else 0f, tween(220), label = "send-alpha")
    val sendScale by animateFloatAsState(
        if (canSend) 1f else 0.86f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "send-scale"
    )
    val clockAlpha by animateFloatAsState(if (canSend) 1f else 0.35f, tween(200), label = "clock-alpha")
    val clockRot by animateFloatAsState(
        if (canSend) 0f else -40f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "clock-rot"
    )
    val sendFg by animateColorAsState(
        if (canSend) onAccent() else MaterialTheme.colorScheme.outline, label = "send-fg"
    )
    val fly = remember { Animatable(0f) }
    val sendSrc = remember { MutableInteractionSource() }
    val flyPx = with(density) { 44.dp.toPx() }
    val focus = remember { FocusRequester() }
    val pillSrc = remember { MutableInteractionSource() }

    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.PersonAdd, stringResource(R.string.attach_contact))
            }
        }
        Surface(
            modifier = Modifier.weight(1f).animateContentSize().glass(RoundedCornerShape(26.dp), haze),
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp)
                    .clickable(interactionSource = pillSrc, indication = null) { focus.requestFocus() },
                verticalAlignment = Alignment.Bottom
            ) {
                if (sims.size > 1) {
                    Box(Modifier.height(52.dp), contentAlignment = Alignment.Center) {
                        SimChip(sims, effectiveSub, chatSimSet, onPickSim)
                    }
                }
                // The schedule icon sits right after the text (not pinned to the far edge), so there is no big gap.
                Box(
                    Modifier.weight(1f, fill = false).widthIn(min = 96.dp).heightIn(min = 52.dp)
                        .padding(start = 8.dp, end = 2.dp, top = 14.dp, bottom = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = text, onValueChange = onText,
                        modifier = Modifier.focusRequester(focus),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.Content,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6,
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (text.isEmpty()) {
                                    Text(
                                        stringResource(R.string.type_message),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onSchedule, enabled = canSend) {
                        Icon(
                            Icons.Default.Schedule, stringResource(R.string.schedule),
                            Modifier.graphicsLayer { alpha = clockAlpha; rotationZ = clockRot }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        val burst = remember { Animatable(0f) }
        val squish = remember { Animatable(1f) }
        val ringColor = MaterialTheme.colorScheme.primary
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            // Expanding ring + sparks that burst out of the button when a message is sent.
            Canvas(Modifier.requiredSize(140.dp)) {
                val t = burst.value
                if (t > 0f && t < 1f) {
                    val c = Offset(size.width / 2, size.height / 2)
                    val r0 = 26.dp.toPx()
                    val r = r0 + (62.dp.toPx() - r0) * t
                    drawCircle(
                        ringColor.copy(alpha = (1f - t) * 0.55f), r, c,
                        style = Stroke(width = (3.dp.toPx() * (1f - t)).coerceAtLeast(0.5f))
                    )
                    for (k in 0 until 8) {
                        val ang = Math.toRadians(k * 45.0 + 22.0)
                        val d = r0 + (50.dp.toPx() - r0) * t
                        drawCircle(
                            ringColor.copy(alpha = (1f - t) * 0.9f), 3.5.dp.toPx() * (1f - t),
                            Offset(c.x + (Math.cos(ang) * d).toFloat(), c.y + (Math.sin(ang) * d).toFloat())
                        )
                    }
                }
            }
            Box(
                Modifier.size(52.dp)
                    .graphicsLayer { scaleX = sendScale * squish.value; scaleY = sendScale * squish.value }
                    .pressScale(sendSrc)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(interactionSource = sendSrc, indication = null, enabled = canSend) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            launch { burst.snapTo(0f); burst.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
                            launch {
                                squish.animateTo(0.8f, tween(80))
                                squish.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow))
                            }
                            fly.animateTo(1f, tween(230, easing = FastOutLinearInEasing))
                            fly.snapTo(-1f)
                            fly.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
                        }
                        onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = sendAlpha }.background(accentBrush(), CircleShape))
                Icon(
                    Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), Modifier.graphicsLayer {
                        translationX = fly.value * flyPx * 1.3f
                        translationY = -fly.value * flyPx * 0.8f
                        rotationZ = -fly.value * 22f
                        val sc = 1f - 0.35f * kotlin.math.abs(fly.value)
                        scaleX = sc
                        scaleY = sc
                        alpha = (1f - kotlin.math.abs(fly.value)).coerceIn(0f, 1f)
                    },
                    tint = sendFg
                )
            }
        }
    }
}

@Composable
private fun SimChip(sims: List<SimInfo>, effectiveSub: Int, chatSimSet: Boolean, onPick: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = sims.firstOrNull { it.subId == effectiveSub }
    val fa = rememberIsFa()
    val simHaptics = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.height(36.dp).pressScale(src, 0.92f).clip(CircleShape)
                .clickable(interactionSource = src, indication = null) { open = true }
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
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
        AppDropdown(expanded = open, onDismiss = { open = false }, alignEnd = false) {
            menuItem(
                text = stringResource(R.string.sim_default_option), icon = Icons.Default.SimCard, checked = !chatSimSet,
                onClick = { open = false; simHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPick(null) }
            )
            sims.forEach { s ->
                menuItem(
                    text = stringResource(R.string.sim_label, s.slot) + " · " + s.name, icon = Icons.Default.SimCard,
                    checked = chatSimSet && s.subId == effectiveSub,
                    onClick = { open = false; simHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onPick(s.subId) }
                )
            }
        }
    }
}


/** Offline link card (domain only). We deliberately don't fetch the page: that would tell the sender the SMS was opened. */
@Composable
private fun LinkCard(url: String, fg: Color, onClick: () -> Unit) {
    val full = if (url.contains("://")) url else "https://$url"
    val parsed = remember(full) { runCatching { Uri.parse(full) }.getOrNull() }
    val host = parsed?.host?.removePrefix("www.")
    if (host.isNullOrBlank()) return
    val secure = parsed?.scheme.equals("https", ignoreCase = true)
    val path = remember(full) { parsed?.path.orEmpty().trim('/').ifEmpty { null } }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(visible = shown, enter = expandVertically(spring(dampingRatio = 0.75f)) + fadeIn(tween(200))) {
        Row(
            Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(fg.copy(alpha = 0.12f)).clickable(onClick = onClick).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.hsv(((host.hashCode() and 0x7fffffff) % 360).toFloat(), 0.55f, 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                Text(host.first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(host, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    path ?: (if (secure) "https" else "http"), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f)
                )
            }
            Icon(
                if (secure) Icons.Default.OpenInBrowser else Icons.Default.WarningAmber, null, Modifier.size(18.dp),
                tint = if (secure) fg.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun JumpButton(count: Int, fa: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box {
        Surface(
            shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp,
            modifier = Modifier.size(44.dp).pressScale(src).clip(CircleShape)
                .clickable(interactionSource = src, indication = null, onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.jump_latest))
            }
        }
        AnimatedVisibility(
            visible = count > 0,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
            enter = scaleIn(spring(dampingRatio = 0.4f, stiffness = 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                Modifier.height(20.dp).widthIn(min = 20.dp).clip(CircleShape).background(accentBrush()).padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.4f, stiffness = 500f)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                    },
                    label = "jump-count"
                ) { n ->
                    Text(
                        DateFmt.digits(if (n > 99) "99+" else n.toString(), fa),
                        style = MaterialTheme.typography.labelSmall, color = onAccent()
                    )
                }
            }
        }
    }
}
