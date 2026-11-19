package dev.imhamid.messages.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.imhamid.messages.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import kotlinx.coroutines.delay

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
fun Avatar(title: String, isContact: Boolean, size: Dp = 48.dp, selected: Boolean = false, address: String? = null) {
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
    Box(
        Modifier.size(size).clip(CircleShape).background(brush),
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
