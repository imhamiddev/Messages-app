package dev.imhamid.messages.util

enum class Category { PERSONAL, BANK, CODES, PROMO }

enum class LinkKind { URL, PHONE }

data class LinkToken(val start: Int, val end: Int, val kind: LinkKind, val value: String)

/** Text heuristics: verification codes, conversation categories, links and phone numbers. */
object Smart {
    private const val D = "0-9\u0660-\u0669\u06F0-\u06F9"

    private val codeRegex = Regex("(?<![$D])[$D]{4,8}(?![$D])")
    private val codeWords = Regex(
        "code|otp|pin|passcode|password|verification|verify|کد|رمز|تایید|تأیید|یکبار",
        RegexOption.IGNORE_CASE
    )
    private val bankWords = Regex(
        "بانک|مبلغ|موجودی|واریز|برداشت|حساب|کارت|تراکنش|پرداخت|ریال|bank|balance|debit|credit|account|transaction",
        RegexOption.IGNORE_CASE
    )
    private val promoWords = Regex(
        "تخفیف|پیشنهاد ویژه|فروش ویژه|عضویت|لغو ?۱۱|لغو 11|جایزه|قرعه|discount|sale|offer|unsubscribe|promo",
        RegexOption.IGNORE_CASE
    )
    private val urlRegex = Regex(
        "(?:https?://|www\\.)[^\\s]+|\\b[a-z0-9][a-z0-9-]*(?:\\.[a-z0-9-]+)*\\.(?:com|ir|net|org|io|app|me|co|info|xyz|link)\\b(?:/[^\\s]*)?",
        RegexOption.IGNORE_CASE
    )
    private val phoneRegex = Regex("(?<![$D])\\+?[$D](?:[ -]?[$D]){8,14}(?![$D])")

    fun toAscii(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            sb.append(
                when (ch) {
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    /** A 4–8 digit verification code (as ASCII digits), or null when the message does not look like one. */
    fun verificationCode(body: String): String? {
        if (!codeWords.containsMatchIn(body)) return null
        return codeRegex.find(body)?.value?.let { toAscii(it) }
    }

    fun categorize(address: String, body: String, isContact: Boolean): Category {
        if (isContact) return Category.PERSONAL
        if (verificationCode(body) != null) return Category.CODES
        if (bankWords.containsMatchIn(body)) return Category.BANK
        val digits = address.count { it.isDigit() }
        val shortOrNamed = address.any { it.isLetter() } || digits <= 6
        if (promoWords.containsMatchIn(body) || shortOrNamed) return Category.PROMO
        return Category.PERSONAL
    }

    fun tokens(body: String): List<LinkToken> {
        val out = ArrayList<LinkToken>()
        for (m in urlRegex.findAll(body)) {
            val start = m.range.first
            var end = m.range.last + 1
            while (end > start && body[end - 1] in ".,;:!?)\"'،؛") end--
            if (end > start) out += LinkToken(start, end, LinkKind.URL, body.substring(start, end))
        }
        for (m in phoneRegex.findAll(body)) {
            val s = m.range.first
            val e = m.range.last + 1
            if (out.any { s < it.end && e > it.start }) continue
            val v = toAscii(m.value).filter { it.isDigit() || it == '+' }
            out += LinkToken(s, e, LinkKind.PHONE, v)
        }
        return out.sortedBy { it.start }
    }
}
