package dev.imhamid.messages.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.imhamid.messages.R
import dev.imhamid.messages.util.CalendarType
import dev.imhamid.messages.util.DateFmt
import dev.imhamid.messages.util.Jalali
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

private fun monthLength(cal: CalendarType, y: Int, m: Int): Int =
    if (cal == CalendarType.JALALI) Jalali.monthLength(y, m) else YearMonth.of(y, m).lengthOfMonth()

private fun toDate(cal: CalendarType, y: Int, m: Int, d: Int): LocalDate =
    if (cal == CalendarType.JALALI) {
        val g = Jalali.toGregorian(y, m, d)
        LocalDate.of(g[0], g[1], g[2])
    } else LocalDate.of(y, m, d)

/** Moves the year or month by [delta] in the chosen calendar, clamping the day. */
private fun shift(dt: LocalDateTime, cal: CalendarType, deltaMonths: Int): LocalDateTime {
    val p = DateFmt.parts(dt.toLocalDate(), cal)
    var y = p[0]
    var m = p[1] + deltaMonths
    while (m > 12) { m -= 12; y++ }
    while (m < 1) { m += 12; y-- }
    val d = minOf(p[2], monthLength(cal, y, m))
    return LocalDateTime.of(toDate(cal, y, m, d), dt.toLocalTime())
}

@Composable
fun DateTimePickerDialog(
    initialMillis: Long,
    cal: CalendarType,
    fa: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var dt by remember {
        mutableStateOf(
            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(initialMillis), ZoneId.systemDefault())
                .withSecond(0).withNano(0)
        )
    }
    val millis = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val inFuture = millis > System.currentTimeMillis()
    val p = DateFmt.parts(dt.toLocalDate(), cal)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.schedule_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    DateFmt.full(millis, cal, fa),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stepper(
                        stringResource(R.string.label_year), p[0], DateFmt.digits(p[0].toString(), fa),
                        onUp = { dt = shift(dt, cal, 12) }, onDown = { dt = shift(dt, cal, -12) }
                    )
                    Stepper(
                        stringResource(R.string.label_month), p[1], DateFmt.monthName(cal, p[1], fa),
                        onUp = { dt = shift(dt, cal, 1) }, onDown = { dt = shift(dt, cal, -1) }
                    )
                    Stepper(
                        stringResource(R.string.label_day), p[2], DateFmt.digits(p[2].toString(), fa),
                        onUp = { dt = dt.plusDays(1) }, onDown = { dt = dt.minusDays(1) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Stepper(
                        stringResource(R.string.label_hour), dt.hour, DateFmt.digits("%02d".format(dt.hour), fa),
                        onUp = { dt = dt.withHour((dt.hour + 1) % 24) },
                        onDown = { dt = dt.withHour((dt.hour + 23) % 24) }
                    )
                    Spacer(Modifier.width(24.dp))
                    Stepper(
                        stringResource(R.string.label_minute), dt.minute, DateFmt.digits("%02d".format(dt.minute), fa),
                        onUp = { dt = dt.withMinute((dt.minute + 5) % 60) },
                        onDown = { dt = dt.withMinute((dt.minute + 55) % 60) }
                    )
                }
                if (!inFuture) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.schedule_past),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(millis) }, enabled = inFuture) {
                Text(stringResource(R.string.schedule_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Up/down stepper; the value slides in the direction of travel. */
@Composable
private fun Stepper(label: String, value: Int, display: String, onUp: () -> Unit, onDown: () -> Unit) {
    var dir by remember { mutableIntStateOf(1) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { dir = 1; onUp() }) { Icon(Icons.Default.KeyboardArrowUp, null) }
        AnimatedContent(
            targetState = value to display,
            transitionSpec = {
                if (dir > 0) (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                else (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "stepper"
        ) { (_, text) ->
            Text(text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
        IconButton(onClick = { dir = -1; onDown() }) { Icon(Icons.Default.KeyboardArrowDown, null) }
    }
}
