package dev.imhamid.messages.util

object PhoneUtils {
    private const val FA = "۰۱۲۳۴۵۶۷۸۹"
    private const val AR = "٠١٢٣٤٥٦٧٨٩"

    /** Converts Persian/Arabic-Indic digits to ASCII digits. */
    fun normalizeDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val i = FA.indexOf(c)
            val j = AR.indexOf(c)
            sb.append(
                when {
                    i >= 0 -> 0 + i
                    j >= 0 -> 0 + j
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** Stable key so +98912..., 0912... and 98912... land in the same conversation. */
    fun key(address: String): String {
        val a = normalizeDigits(address).trim()
        val digits = a.filter { it.isDigit() }
        val hasLetters = a.any { it.isLetter() }
        return if (!hasLetters && digits.length >= 7) digits.takeLast(10) else a.lowercase()
    }

    fun looksLikeNumber(s: String): Boolean =
        Regex("^[+]?[0-9 ()\\-]{3,}$").matches(normalizeDigits(s).trim())

    /** Keeps only digits and a leading plus, ready to be used as a send target. */
    fun clean(address: String): String {
        val a = normalizeDigits(address).trim()
        val plus = a.startsWith("+")
        val digits = a.filter { it.isDigit() }
        return if (plus) "+$digits" else digits
    }
}
