package dev.imhamid.messages.ui

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
    val openedAt = remember { System.currentTimeMillis() }
    val played = remember { mutableSetOf<Long>() }

    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var askDeleteChat by remember { mutableStateOf(false) }
    var askBlock by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MessageEntity?>(null) }
    var selectingId by remember { mutableStateOf<Long?>(null) }
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
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
                            Avatar(title, isContact = name != null, size = 38.dp, address = address)
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
                                    leadingIcon = { Icon(if (pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin, null) },
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { pad ->
            Column(Modifier.fillMaxSize().padding(pad)) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, reverseLayout = true) {
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
                                animateIn = m.date >= openedAt && m.id !in played,
                                onPlayed = { played.add(m.id) },
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

/** Read-only text field that starts with the whole message selected, so the system toolbar offers Copy. */
@Composable
private fun SelectableBody(text: String, color: Color, initial: TextRange?, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(text, initial ?: TextRange(0, text.length))) }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler { onDone() }
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

private val digitRun = Regex("[0-9\\u0660-\\u0669\\u06F0-\\u06F9]+")

/** The run of digits (Latin, Arabic-Indic or Persian) under a touch point in the bubble text, or null. */
private fun numberAt(
    pos: Offset,
    box: LayoutCoordinates?,
    textCoords: LayoutCoordinates?,
    layout: TextLayoutResult?,
    body: String
): TextRange? {
    if (box == null || textCoords == null || layout == null || !box.isAttached || !textCoords.isAttached) return null
    val local = textCoords.localPositionOf(box, pos)
    if (local.x < -16f || local.y < -8f || local.x > layout.size.width + 16f || local.y > layout.size.height + 8f) return null
    val off = layout.getOffsetForPosition(local).coerceIn(0, body.length)
    for (m in digitRun.findAll(body)) {
        if (off >= m.range.first && off <= m.range.last + 1) return TextRange(m.range.first, m.range.last + 1)
    }
    return null
}

@Composable
private fun Bubble(
    m: MessageEntity,
    fa: Boolean,
    animateIn: Boolean,
    onPlayed: () -> Unit,
    selecting: Boolean,
    onSelectDone: () -> Unit,
    onCopy: () -> Unit,
    onSelectText: () -> Unit,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    val out = !m.incoming
    val grad = LocalGradient.current
    val src = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var boxCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var numberSel by remember { mutableStateOf<TextRange?>(null) }
    val appear = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animateIn) {
            appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
            onPlayed()
        }
    }
    val shape = if (out) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
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

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).graphicsLayer {
            alpha = appear.value.coerceIn(0f, 1f)
            val s = 0.85f + 0.15f * appear.value
            scaleX = s
            scaleY = s
            translationY = (1f - appear.value) * 48f
            transformOrigin = TransformOrigin(if (out) 1f else 0f, 1f)
        },
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start
    ) {
        Box {
            Surface(
                shape = shape,
                color = container,
                contentColor = content,
                modifier = Modifier.widthIn(max = 300.dp).pressScale(src, 0.97f).then(bgMod).clip(shape)
                    .onGloballyPositioned { boxCoords = it }
                    .then(
                        if (selecting) Modifier else Modifier.pointerInput(m.id) {
                            detectTapGestures(
                                onPress = {
                                    val press = PressInteraction.Press(it)
                                    src.emit(press)
                                    val released = tryAwaitRelease()
                                    src.emit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                                },
                                onTap = { menu = true },
                                onLongPress = { pos ->
                                    val r = numberAt(pos, boxCoords, textCoords, textLayout, m.body)
                                    if (r != null) {
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
                Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp).animateContentSize()) {
                    if (selecting) SelectableBody(m.body, content, numberSel) { numberSel = null; onSelectDone() }
                    else Text(
                        m.body,
                        modifier = Modifier.onGloballyPositioned { textCoords = it },
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                        onTextLayout = { textLayout = it }
                    )
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
                            AnimatedContent(targetState = m.type == TYPE_SENDING, label = "status") { sending ->
                                Icon(
                                    if (sending) Icons.Default.Schedule else Icons.Default.Check,
                                    null, Modifier.size(13.dp), tint = content.copy(alpha = 0.7f)
                                )
                            }
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
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_select_text)) },
                    leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                    onClick = { menu = false; numberSel = null; onSelectText() }
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

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            Modifier.weight(1f).animateContentSize(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
                if (sims.size > 1) {
                    Box(Modifier.height(52.dp), contentAlignment = Alignment.Center) {
                        SimChip(sims, effectiveSub, chatSimSet, onPickSim)
                    }
                }
                Box(
                    Modifier.weight(1f).heightIn(min = 52.dp).padding(horizontal = 8.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = text, onValueChange = onText,
                        modifier = Modifier.fillMaxWidth(),
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
        Box(
            Modifier.size(52.dp)
                .graphicsLayer { scaleX = sendScale; scaleY = sendScale }
                .pressScale(sendSrc)
                .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(interactionSource = sendSrc, indication = null, enabled = canSend) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        fly.animateTo(1f, tween(140))
                        fly.snapTo(-1f)
                        fly.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                    }
                    onSend()
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = sendAlpha }.background(accentBrush(), CircleShape))
            Icon(
                Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), Modifier.graphicsLayer {
                    translationX = fly.value * flyPx
                    alpha = (1f - kotlin.math.abs(fly.value)).coerceIn(0f, 1f)
                },
                tint = sendFg
            )
        }
    }
}

@Composable
private fun SimChip(sims: List<SimInfo>, effectiveSub: Int, chatSimSet: Boolean, onPick: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val current = sims.firstOrNull { it.subId == effectiveSub }
    val fa = rememberIsFa()
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
