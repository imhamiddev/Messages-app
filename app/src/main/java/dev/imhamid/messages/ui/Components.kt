@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package dev.imhamid.messages.ui

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.imhamid.messages.R
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import dev.imhamid.messages.app

@Composable
fun rememberIsFa(): Boolean {
    val cfg = LocalConfiguration.current
    return cfg.locales[0].language == "fa"
}

/** Springy "squish" on press – used on buttons, rows and the FAB. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.94f): Modifier {
    val isPressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

private val AvatarPalette = listOf(0xFF5B5FEF, 0xFF0EA5A4, 0xFFE0662F, 0xFFC2417C, 0xFF2F7DD1, 0xFF7C5CD6, 0xFF3C9A55, 0xFFB7791F)

/** Round avatar: contact photo when available, otherwise a gradient with the initial. */
@Composable
fun Avatar(title: String, isContact: Boolean, size: Dp = 48.dp, selected: Boolean = false, address: String? = null, sharedKey: String? = null) {
    val ctx = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, address) {
        value = if (address != null) ctx.app.repo.contacts.photoFor(address) else null
    }
    val color = Color(AvatarPalette[(title.hashCode() and 0x7fffffff) % AvatarPalette.size])
    val brush = if (selected) SolidColor(MaterialTheme.colorScheme.primary)
    else Brush.linearGradient(listOf(lerp(color, Color.White, 0.22f), lerp(color, Color.Black, 0.22f)))
    val mode = when {
        selected -> 0
        photo != null -> 1
        else -> 2
    }
    val shared = LocalShared.current
    val sharedMod = if (sharedKey != null && shared != null) {
        with(shared.first) { Modifier.sharedElement(rememberSharedContentState("avatar-$sharedKey"), shared.second) }
    } else Modifier
    Box(
        Modifier.size(size).then(sharedMod).clip(CircleShape).background(brush),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.6f) + fadeIn()) togetherWith
                    (scaleOut(targetScale = 0.6f) + fadeOut())
            },
            label = "avatar-content"
        ) { m ->
            val letter = title.firstOrNull { it.isLetter() }
            when {
                m == 0 -> Icon(Icons.Default.Check, null, tint = Color.White)
                m == 1 && photo != null -> Image(
                    bitmap = photo!!, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
                isContact && letter != null -> Text(
                    letter.uppercase(), color = Color.White,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
                else -> Icon(Icons.Default.Person, null, tint = Color.White)
            }
        }
    }
}

/** Slide-up + fade entrance, staggered by [index]. Pass play = false to skip (e.g. on scroll re-entry). */
@Composable
fun Modifier.entrance(index: Int, play: Boolean = true): Modifier {
    val p = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (play) {
            delay(index.coerceAtMost(12) * 45L)
            p.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow))
        }
    }
    return this.graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * 56f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmText,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null) {
    val t = rememberInfiniteTransition(label = "float")
    val dy by t.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy"
    )
    Column(
        Modifier.fillMaxSize().padding(32.dp).entrance(0),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.graphicsLayer { translationY = dy }.size(88.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
        }
    }
}

/** Compact segmented selector with an animated highlight and press feedback. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val src = remember { MutableInteractionSource() }
            val bg by animateColorAsState(
                if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, label = "seg-bg"
            )
            val fg by animateColorAsState(
                if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "seg-fg"
            )
            Box(
                Modifier.weight(1f).pressScale(src, 0.95f).clip(RoundedCornerShape(10.dp)).background(bg)
                    .clickable(interactionSource = src, indication = null) { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Round colour/gradient swatch. brush == null renders the "none" option. */
@Composable
fun Swatch(selected: Boolean, brush: Brush?, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        if (selected) 1.1f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swatch"
    )
    val ring by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, label = "ring"
    )
    val fill = brush ?: SolidColor(MaterialTheme.colorScheme.surfaceContainerHigh)
    Box(
        Modifier.size(46.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .pressScale(src, 0.88f).border(2.dp, ring, CircleShape).padding(5.dp)
            .clip(CircleShape).background(fill)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (brush == null && !selected) {
            Icon(Icons.Default.Block, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                Icons.Default.Check, null, Modifier.size(20.dp),
                tint = if (brush == null) MaterialTheme.colorScheme.onSurface else Color.White
            )
        }
    }
}

/**
 * Real frosted glass: the content behind the surface (marked with `Modifier.haze(state)`) is blurred and
 * lightly tinted with one flat colour, plus a thin flat rim. No gradients. Without a [haze] state (or where
 * blur is unsupported) it falls back to a flat, mostly opaque surface.
 */
@Composable
fun Modifier.glass(shape: Shape, haze: HazeState? = null): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val rim = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
    val base = this.clip(shape)
    val filled = if (haze != null) {
        val style = HazeStyle(
            backgroundColor = scheme.surface,
            tints = listOf(HazeTint(scheme.surface.copy(alpha = if (dark) 0.50f else 0.55f))),
            blurRadius = 24.dp,
            noiseFactor = 0.03f,
            fallbackTint = HazeTint(scheme.surface.copy(alpha = 0.90f))
        )
        base.hazeChild(haze, style)
    } else {
        base.background(scheme.surface.copy(alpha = 0.90f))
    }
    return filled.border(1.dp, rim, shape)
}

class MenuScope(col: ColumnScope) : ColumnScope by col {
    internal var count = 0
}

/**
 * Styled replacement for the stock DropdownMenu: scales/fades out of the anchor corner, flips above the
 * anchor when there is no room below, and its items stagger in. Put it inside a Box next to the anchor.
 */
@Composable
fun AppDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    alignEnd: Boolean = true,
    content: @Composable MenuScope.() -> Unit
) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    if (visible.currentState || visible.targetState) {
        var up by remember { mutableStateOf(false) }
        val pad = 12.dp
        val padPx = with(LocalDensity.current) { pad.roundToPx() }
        val gapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
        val rightAligned = alignEnd == (LocalLayoutDirection.current == LayoutDirection.Ltr)
        val provider = remember(rightAligned, padPx, gapPx) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize
                ): IntOffset {
                    val x0 = if (rightAligned) anchorBounds.right - popupContentSize.width + padPx else anchorBounds.left - padPx
                    val x = x0.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                    val below = anchorBounds.bottom - padPx + gapPx
                    val above = anchorBounds.top - popupContentSize.height + padPx - gapPx
                    val goUp = below + popupContentSize.height > windowSize.height && above >= 0
                    if (goUp != up) up = goUp
                    val y = if (goUp) above else below.coerceAtMost(windowSize.height - popupContentSize.height).coerceAtLeast(0)
                    return IntOffset(x, y)
                }
            }
        }
        Popup(popupPositionProvider = provider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
            val origin = TransformOrigin(if (rightAligned) 1f else 0f, if (up) 1f else 0f)
            AnimatedVisibility(
                visibleState = visible,
                enter = fadeIn(tween(120)) + scaleIn(spring(dampingRatio = 0.72f, stiffness = 520f), initialScale = 0.7f, transformOrigin = origin),
                exit = fadeOut(tween(90)) + scaleOut(tween(110), targetScale = 0.9f, transformOrigin = origin)
            ) {
                Box(Modifier.padding(pad)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 3.dp,
                        shadowElevation = 12.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 176.dp).padding(6.dp)) {
                            MenuScope(this).content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScope.menuItem(
    text: String,
    icon: ImageVector? = null,
    danger: Boolean = false,
    checked: Boolean = false,
    onClick: () -> Unit
) {
    val idx = remember { count++ }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(idx * 28L)
        appear.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 380f))
    }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        tween(120), label = "menu-item-bg"
    )
    val iconScale by animateFloatAsState(
        if (pressed) 1.2f else 1f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "menu-item-icon"
    )
    val fg = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 14f
            }
            .pressScale(src, 0.97f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon, null,
                Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                tint = if (danger) fg else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = fg)
        if (checked) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}


/** Scopes handed down by the nav host so avatars can fly between the list and the chat. */
val LocalShared = compositionLocalOf<Pair<SharedTransitionScope, AnimatedVisibilityScope>?> { null }

@Composable
fun HapticSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Switch(checked = checked, onCheckedChange = {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onCheckedChange(it)
    })
}

// ---------------------------------------------------------------- shimmer skeletons

@Composable
fun Modifier.shimmer(): Modifier {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "shimmer-x"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.surface)
    return this.drawBehind {
        drawRect(
            Brush.linearGradient(
                listOf(base, hi, base),
                start = Offset(size.width * x, 0f), end = Offset(size.width * (x + 0.6f), size.height * 0.2f)
            )
        )
    }
}

@Composable
fun SkeletonRow(avatar: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (avatar) {
            Box(Modifier.size(48.dp).clip(CircleShape).shimmer())
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.42f).height(14.dp).clip(RoundedCornerShape(7.dp)).shimmer())
            Spacer(Modifier.height(9.dp))
            Box(Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(6.dp)).shimmer())
        }
    }
}

@Composable
fun SkeletonList(count: Int = 8, avatar: Boolean = true) {
    Column(Modifier.fillMaxSize()) { repeat(count) { SkeletonRow(avatar) } }
}

// ---------------------------------------------------------------- swipe row

class SwipeAction(val icon: ImageVector, val color: Color)

private enum class SwipeTier { NONE, START, END, FAR }

/**
 * Swipe right = [start]; swipe left = [end] (partial) or [far] (long swipe). The action icon grows with the
 * finger, bounces when the threshold is crossed (with a tick), then turns into a check mark on release.
 */
@Composable
fun SwipeRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    start: SwipeAction? = null, onStart: () -> Unit = {},
    end: SwipeAction? = null, onEnd: () -> Unit = {},
    far: SwipeAction? = null, onFar: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offset = remember { Animatable(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    var done by remember { mutableStateOf(false) }
    val tier by remember(start, end, far) {
        derivedStateOf {
            val o = offset.value
            when {
                start != null && o > width * 0.28f -> SwipeTier.START
                far != null && o < -width * 0.62f -> SwipeTier.FAR
                end != null && o < -width * 0.28f -> SwipeTier.END
                else -> SwipeTier.NONE
            }
        }
    }
    LaunchedEffect(tier) { if (tier != SwipeTier.NONE) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val active: SwipeAction? = when (tier) {
        SwipeTier.START -> start
        SwipeTier.END -> end
        SwipeTier.FAR -> far
        SwipeTier.NONE -> if (offset.value > 0f) start else if (offset.value < 0f) end else null
    }
    val progress = (abs(offset.value) / (width * 0.28f)).coerceIn(0f, 1f)
    val bg by animateColorAsState(
        if (tier != SwipeTier.NONE && active != null) active.color else MaterialTheme.colorScheme.surfaceContainerHighest,
        tween(160), label = "swipe-bg"
    )
    val iconScale by animateFloatAsState(
        if (tier != SwipeTier.NONE) 1.3f else 0.5f + 0.5f * progress,
        spring(dampingRatio = 0.45f, stiffness = 500f), label = "swipe-icon"
    )

    fun release() {
        scope.launch {
            when (tier) {
                SwipeTier.START -> {
                    done = true
                    onStart()
                    delay(380)
                    offset.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 400f))
                    done = false
                }
                SwipeTier.END, SwipeTier.FAR -> {
                    val t = tier
                    done = true
                    delay(130)
                    offset.animateTo(-width, tween(220))
                    if (t == SwipeTier.FAR) onFar() else onEnd()
                    delay(700)
                    offset.snapTo(0f)
                    done = false
                }
                SwipeTier.NONE -> offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
            }
        }
    }

    Box(modifier.fillMaxWidth().clipToBounds().onSizeChanged { width = it.width.toFloat() }) {
        if (offset.value != 0f) {
            Box(
                Modifier.matchParentSize().background(bg),
                contentAlignment = if (offset.value > 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val shown = if (done) Icons.Default.Check else active?.icon
                if (shown != null) {
                    AnimatedContent(
                        targetState = shown,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.45f, stiffness = 500f), initialScale = 0.3f) + fadeIn(tween(100))) togetherWith
                                (scaleOut(tween(90), targetScale = 0.3f) + fadeOut(tween(80)))
                        },
                        label = "swipe-icon-swap"
                    ) { ic ->
                        Icon(
                            ic, null,
                            Modifier.padding(horizontal = 26.dp).size(26.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            tint = if (tier != SwipeTier.NONE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }.then(
                if (!enabled) Modifier else Modifier.pointerInput(start, end, far) {
                    detectHorizontalDragGestures(
                        onDragStart = { done = false },
                        onDragEnd = { release() },
                        onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            val lo = if (end != null || far != null) -width else 0f
                            val hi = if (start != null) width * 0.5f else 0f
                            scope.launch { offset.snapTo((offset.value + dx).coerceIn(lo, hi)) }
                        }
                    )
                }
            )
        ) { content() }
    }
}
