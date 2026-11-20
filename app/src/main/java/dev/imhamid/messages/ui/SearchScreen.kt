package dev.imhamid.messages.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.imhamid.messages.R
import dev.imhamid.messages.data.MessageEntity
import dev.imhamid.messages.util.DateFmt
import kotlinx.coroutines.delay

private fun highlight(body: String, q: String, fg: Color, bg: Color): AnnotatedString {
    val idx = body.indexOf(q, ignoreCase = true).coerceAtLeast(0)
    val start = (idx - 30).coerceAtLeast(0)
    val end = (idx + q.length + 60).coerceAtMost(body.length)
    val prefix = if (start > 0) "…" else ""
    val snippet = prefix + body.substring(start, end) + if (end < body.length) "…" else ""
    return buildAnnotatedString {
        append(snippet)
        var from = 0
        while (true) {
            val i = snippet.indexOf(q, from, ignoreCase = true)
            if (i < 0) break
            addStyle(SpanStyle(color = fg, background = bg, fontWeight = FontWeight.Bold), i, i + q.length)
            from = i + q.length.coerceAtLeast(1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String, Long) -> Unit) {
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    val cal by vm.calendar.collectAsStateWithLifecycle()
    val fa = rememberIsFa()
    val focus = remember { FocusRequester() }
    val term = q.trim()

    LaunchedEffect(Unit) { focus.requestFocus() }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(term) {
        if (term.length < 2) {
            results = emptyList()
            loading = false
        } else {
            loading = true
            delay(200)
            results = vm.search(term)
            loading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                title = {
                    BasicTextField(
                        value = q, onValueChange = { q = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface, textDirection = TextDirection.Content
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (q.isEmpty()) {
                                    Text(
                                        stringResource(R.string.search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                inner()
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                term.length < 2 -> EmptyState(Icons.Default.Search, stringResource(R.string.search_min))
                loading && results.isEmpty() -> SkeletonList(count = 6, avatar = false)
                results.isEmpty() -> EmptyState(Icons.Default.Search, stringResource(R.string.search_empty))
                else -> LazyColumn {
                    items(results, key = { it.id }) { m ->
                        val name by produceState<String?>(null, m.address) { value = vm.displayName(m.address) }
                        Column(
                            Modifier.fillMaxWidth().animateItem().clickable { onOpen(m.address, m.id) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name ?: DateFmt.digits(m.address, fa), Modifier.weight(1f), maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    DateFmt.full(m.date, cal, fa), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            var shown by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { shown = true }
                            val hlBg by animateColorAsState(
                                if (shown) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, tween(450), label = "hl-bg"
                            )
                            val hlFg by animateColorAsState(
                                if (shown) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                tween(450), label = "hl-fg"
                            )
                            Text(
                                highlight(m.body, term, hlFg, hlBg),
                                maxLines = 3, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
