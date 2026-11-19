package dev.imhamid.messages.ui

import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Search
import dev.imhamid.messages.util.Category
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.R
import dev.imhamid.messages.data.TYPE_FAILED
import dev.imhamid.messages.util.CalendarType
import dev.imhamid.messages.util.DateFmt

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    vm: MainViewModel,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onTrash: () -> Unit,
    onBlocked: () -> Unit,
    onScheduled: () -> Unit,
    onSearch: () -> Unit
) {
    val threads by vm.threads.collectAsStateWithLifecycle()
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val fa = rememberIsFa()
    val haptics = LocalHapticFeedback.current
    var selected by remember { mutableStateOf(setOf<String>()) }
    var askDelete by remember { mutableStateOf(false) }
    var askBlock by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var cat by remember { mutableStateOf<Category?>(null) }
    val list = threads?.let { l -> if (cat == null) l else l.filter { it.category == cat } }
    var introDone by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        fabVisible = true
        delay(900)
        introDone = true
    }

    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
    LaunchedEffect(list) {
        if (list != null) selected = selected.filter { k -> list.any { it.key == k } }.toSet()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AnimatedContent(
                targetState = selected.isNotEmpty(),
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "list-bar"
            ) { selecting ->
                if (selecting) {
                    val allPinned = list.orEmpty().filter { it.key in selected }.let { l -> l.isNotEmpty() && l.all { it.pinned } }
                    TopAppBar(
                        title = { Text(stringResource(R.string.selected_count, selected.size)) },
                        navigationIcon = {
                            IconButton(onClick = { selected = emptySet() }) { Icon(Icons.Default.Close, null) }
                        },
                        actions = {
                            IconButton(onClick = {
                                vm.pin(selected.toList(), !allPinned)
                                selected = emptySet()
                            }) {
                                Icon(
                                    if (allPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                    contentDescription = stringResource(if (allPinned) R.string.action_unpin else R.string.action_pin)
                                )
                            }
                            if (selected.size == 1) {
                                IconButton(onClick = { askBlock = true }) {
                                    Icon(Icons.Default.Block, stringResource(R.string.action_block))
                                }
                            }
                            IconButton(onClick = { askDelete = true }) {
                                Icon(Icons.Default.Delete, stringResource(R.string.action_delete))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) },
                        actions = {
                            IconButton(onClick = onSearch) {
                                Icon(Icons.Default.Search, stringResource(R.string.search))
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Default.MoreVert, stringResource(R.string.more))
                                }
                                AppDropdown(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                                    menuItem(text = stringResource(R.string.menu_scheduled), icon = Icons.Default.Schedule, onClick = { menuOpen = false; onScheduled() })
                                    menuItem(text = stringResource(R.string.menu_trash), icon = Icons.Default.Delete, onClick = { menuOpen = false; onTrash() })
                                    menuItem(text = stringResource(R.string.menu_blocked), icon = Icons.Default.Block, onClick = { menuOpen = false; onBlocked() })
                                    menuItem(text = stringResource(R.string.menu_settings), icon = Icons.Default.Settings, onClick = { menuOpen = false; onSettings() })
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
            }
        },
        floatingActionButton = {
            val src = remember { MutableInteractionSource() }
            val pressed by src.collectIsPressedAsState()
            val rot by animateFloatAsState(if (pressed) 90f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fab-rot")
            val fabShape = RoundedCornerShape(20.dp)
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                exit = scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    expanded = false,
                    interactionSource = src,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    modifier = Modifier.pressScale(src).shadow(8.dp, fabShape).clip(fabShape).background(accentBrush()),
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.new_chat), Modifier.graphicsLayer { rotationZ = rot }) },
                    text = {},
                    containerColor = Color.Transparent,
                    contentColor = onAccent()
                )
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
        CategoryTabs(cat) { cat = it }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                list == null -> Unit
                list.isEmpty() -> EmptyState(
                    Icons.Default.ChatBubbleOutline,
                    stringResource(R.string.no_chats),
                    stringResource(R.string.no_chats_hint)
                )
                else -> LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 96.dp)) {
                    itemsIndexed(list, key = { _, t -> t.key }) { index, t ->
                        ChatRow(
                            modifier = Modifier.animateItem().entrance(index, play = !introDone),
                            t = t, cal = cal, fa = fa,
                            selected = t.key in selected,
                            onClick = {
                                if (selected.isNotEmpty()) {
                                    selected = if (t.key in selected) selected - t.key else selected + t.key
                                } else onOpen(t.address)
                            },
                            onLong = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = if (t.key in selected) selected - t.key else selected + t.key
                            }
                        )
                    }
                }
            }
        }
        }
    }

    if (askDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_chats_title),
            body = stringResource(R.string.delete_chats_body),
            confirmText = stringResource(R.string.action_move_to_trash),
            onConfirm = {
                vm.trashThreads(selected.toList())
                selected = emptySet()
                askDelete = false
            },
            onDismiss = { askDelete = false }
        )
    }
    if (askBlock) {
        val target = list.orEmpty().firstOrNull { it.key in selected }
        ConfirmDialog(
            title = stringResource(R.string.block_title),
            body = stringResource(R.string.block_body),
            confirmText = stringResource(R.string.action_block),
            onConfirm = {
                target?.let { vm.block(it.key, it.address) }
                selected = emptySet()
                askBlock = false
            },
            onDismiss = { askBlock = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    modifier: Modifier,
    t: ThreadItem,
    cal: CalendarType,
    fa: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLong: () -> Unit
) {
    val src = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.background,
        label = "row-bg"
    )
    val unread = t.unread > 0
    val prefix = if (!t.last.incoming) stringResource(R.string.you_prefix) else ""
    Row(
        modifier.fillMaxWidth().pressScale(src, 0.97f).background(bg)
            .combinedClickable(interactionSource = src, indication = LocalIndication.current, onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(title = t.title, isContact = t.name != null, selected = selected, address = t.address)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium
                )
                AnimatedVisibility(
                    visible = t.pinned,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Filled.PushPin, null, Modifier.padding(horizontal = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    DateFmt.listStamp(t.last.date, cal, fa),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    prefix + t.last.body.replace('\n', ' '),
                    Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (t.last.type == TYPE_FAILED) MaterialTheme.colorScheme.error
                    else if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = unread,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.height(22.dp).clip(CircleShape).background(accentBrush())
                                .padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = t.unread,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                        (scaleOut() + fadeOut())
                                },
                                label = "badge"
                            ) { n ->
                                Text(
                                    DateFmt.digits(if (n > 99) "99+" else n.toString(), fa),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onAccent()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: Category?, onSelect: (Category?) -> Unit) {
    val items = listOf<Pair<Category?, Int>>(
        null to R.string.cat_all,
        Category.PERSONAL to R.string.cat_personal,
        Category.BANK to R.string.cat_bank,
        Category.CODES to R.string.cat_codes,
        Category.PROMO to R.string.cat_promo
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (c, label) ->
            FilterChip(
                selected = selected == c, onClick = { onSelect(c) },
                label = { Text(stringResource(label)) }
            )
        }
    }
}
