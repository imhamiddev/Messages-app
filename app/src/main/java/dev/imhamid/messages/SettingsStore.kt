package dev.imhamid.messages

import android.content.Context
import dev.imhamid.messages.util.CalendarType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsStore(ctx: Context) {
    private val sp = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _calendar = MutableStateFlow(
        runCatching { CalendarType.valueOf(sp.getString("calendar", "JALALI")!!) }.getOrDefault(CalendarType.JALALI)
    )
    val calendar: StateFlow<CalendarType> = _calendar

    /** Subscription id used for sending by default; -1 means "system default". */
    private val _defaultSim = MutableStateFlow(sp.getInt("default_sim", -1))
    val defaultSim: StateFlow<Int> = _defaultSim

    /** 0 = follow system, 1 = light, 2 = dark. */
    private val _theme = MutableStateFlow(sp.getInt("theme", 0))
    val theme: StateFlow<Int> = _theme

    fun setCalendar(c: CalendarType) {
        _calendar.value = c
        sp.edit().putString("calendar", c.name).apply()
    }

    fun setDefaultSim(subId: Int) {
        _defaultSim.value = subId
        sp.edit().putInt("default_sim", subId).apply()
    }

    fun setTheme(t: Int) {
        _theme.value = t
        sp.edit().putInt("theme", t).apply()
    }
}
