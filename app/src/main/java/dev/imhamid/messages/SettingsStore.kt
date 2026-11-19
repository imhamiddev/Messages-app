package dev.imhamid.messages

import android.content.Context
import dev.imhamid.messages.ui.SOLID_STYLE_COUNT
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

    /** Index into BubbleStyles. The old separate accent / gradient settings are migrated once. */
    private val _bubble = MutableStateFlow(
        if (sp.contains("bubble")) sp.getInt("bubble", 0)
        else {
            val g = sp.getInt("gradient", 0)
            if (g > 0) SOLID_STYLE_COUNT + g - 1 else sp.getInt("accent", 0)
        }
    )
    val bubble: StateFlow<Int> = _bubble

    fun setBubble(i: Int) { _bubble.value = i; sp.edit().putInt("bubble", i).apply() }

    /** Index into FontScales (0..3). */
    private val _fontScale = MutableStateFlow(sp.getInt("font_scale", 1))
    val fontScale: StateFlow<Int> = _fontScale
    fun setFontScale(i: Int) { _fontScale.value = i; sp.edit().putInt("font_scale", i).apply() }

    /** Use the system (Material You) colours instead of the bubble-derived accent. */
    private val _dynamic = MutableStateFlow(sp.getBoolean("dynamic", false))
    val dynamic: StateFlow<Boolean> = _dynamic
    fun setDynamic(v: Boolean) { _dynamic.value = v; sp.edit().putBoolean("dynamic", v).apply() }

    /** True once the user has answered the "make default SMS app" prompt (accepted or skipped). */
    var defaultPromptDismissed: Boolean
        get() = sp.getBoolean("default_prompt_dismissed", false)
        set(v) = sp.edit().putBoolean("default_prompt_dismissed", v).apply()

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
