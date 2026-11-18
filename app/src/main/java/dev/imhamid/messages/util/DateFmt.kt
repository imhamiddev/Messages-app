package dev.imhamid.messages.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class CalendarType { JALALI, GREGORIAN }

/** Gregorian <-> Jalali (Solar Hijri) conversion. */
object Jalali {
    private val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    /** @return [year, month, day] */
    fun fromGregorian(gy: Int, gm: Int, gd: Int): IntArray {
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }

    /** @return [year, month, day] */
    fun toGregorian(jyIn: Int, jm: Int, jd: Int): IntArray {
        val jy = jyIn + 1595
        var days = -355668 + (365 * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4) + jd +
            (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val sal = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13) {
            if (gd <= sal[gm]) break
            gd -= sal[gm]
            gm++
        }
        return intArrayOf(gy, gm, gd)
    }

    fun isLeap(jy: Int): Boolean {
        val g = toGregorian(jy, 12, 30)
        val back = fromGregorian(g[0], g[1], g[2])
        return back[0] == jy && back[1] == 12 && back[2] == 30
    }

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }
}

object DateFmt {
    private val jFa = arrayOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")
    private val jEn = arrayOf("Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand")
    private val gFa = arrayOf("ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن", "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر")
    private val gEn = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

    fun digits(s: String, fa: Boolean): String =
        if (!fa) s else buildString { for (c in s) append(if (c in '0'..'9') FA_DIGITS[c - '0'] else c) }

    fun monthName(cal: CalendarType, month: Int, fa: Boolean): String {
        val arr = when {
            cal == CalendarType.JALALI && fa -> jFa
            cal == CalendarType.JALALI -> jEn
            fa -> gFa
            else -> gEn
        }
        return arr[(month - 1).coerceIn(0, 11)]
    }

    private fun localDate(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    /** @return [year, month, day] in the requested calendar */
    fun parts(date: LocalDate, cal: CalendarType): IntArray =
        if (cal == CalendarType.JALALI) Jalali.fromGregorian(date.year, date.monthValue, date.dayOfMonth)
        else intArrayOf(date.year, date.monthValue, date.dayOfMonth)

    fun date(ms: Long, cal: CalendarType, fa: Boolean): String {
        val p = parts(localDate(ms), cal)
        return digits("${p[2]} ${monthName(cal, p[1], fa)} ${p[0]}", fa)
    }

    fun shortDate(ms: Long, cal: CalendarType, fa: Boolean): String {
        val p = parts(localDate(ms), cal)
        return digits("${p[2]} ${monthName(cal, p[1], fa)}", fa)
    }

    fun time(ms: Long, fa: Boolean): String {
        val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        return digits(String.format("%02d:%02d", t.hour, t.minute), fa)
    }

    fun full(ms: Long, cal: CalendarType, fa: Boolean): String =
        date(ms, cal, fa) + " · " + time(ms, fa)

    /** Chat-list stamp: time today, short date this year, full date otherwise. */
    fun listStamp(ms: Long, cal: CalendarType, fa: Boolean): String {
        val d = localDate(ms)
        val today = LocalDate.now()
        return when {
            d == today -> time(ms, fa)
            parts(d, cal)[0] == parts(today, cal)[0] -> shortDate(ms, cal, fa)
            else -> date(ms, cal, fa)
        }
    }

    fun dayHeader(ms: Long, cal: CalendarType, fa: Boolean, todayLabel: String, yesterdayLabel: String): String {
        val d = localDate(ms)
        val today = LocalDate.now()
        return when (d) {
            today -> todayLabel
            today.minusDays(1) -> yesterdayLabel
            else -> date(ms, cal, fa)
        }
    }

    fun isSameDay(a: Long, b: Long): Boolean = localDate(a) == localDate(b)
}
