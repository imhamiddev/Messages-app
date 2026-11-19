package dev.imhamid.messages.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * iOS-style emoji: the system font is bypassed and each emoji is drawn from a bundled image
 * (assets/emoji/<codepoints>.webp). Detection is a small hand-written scanner so it works on every API level.
 */
object Emoji {
    class Span(val start: Int, val end: Int, val file: String)

    @Volatile private var names: Set<String>? = null
    private val cache = LruCache<String, ImageBitmap>(300)

    private fun names(ctx: Context): Set<String> =
        names ?: synchronized(this) {
            names ?: (ctx.assets.list("emoji")?.map { it.removeSuffix(".webp") }?.toHashSet() ?: hashSetOf()).also { names = it }
        }

    private fun hex(cps: List<Int>) = cps.joinToString("-") { "%04x".format(it) }

    private fun lookup(set: Set<String>, cps: List<Int>): String? {
        val exact = hex(cps)
        if (exact in set) return exact
        val bare = cps.filter { it != 0xFE0F }
        if (bare.isEmpty()) return null
        hex(bare).let { if (it in set) return it }
        hex(listOf(bare[0], 0xFE0F) + bare.drop(1)).let { if (it in set) return it }
        return null
    }

    private fun isRegional(c: Int) = c in 0x1F1E6..0x1F1FF

    /** Characters that are emoji even without a variation selector. */
    private fun emojiByDefault(c: Int) =
        c >= 0x1F000 || c in 0x2600..0x27BF || c in 0x2B50..0x2B55 || c in 0x231A..0x231B ||
            c in 0x23E9..0x23F3 || c in 0x25FD..0x25FE || c in 0x2B1B..0x2B1C

    fun spans(ctx: Context, text: String): List<Span> {
        if (text.none { it.code > 0x7F } && !text.contains('\u20E3')) return emptyList()
        val set = names(ctx)
        val out = ArrayList<Span>()
        val n = text.length
        var i = 0
        while (i < n) {
            val c0 = text.codePointAt(i)
            val w0 = Character.charCount(c0)
            val next = if (i + w0 < n) text.codePointAt(i + w0) else -1
            val keycapBase = c0 == '#'.code || c0 == '*'.code || c0 in '0'.code..'9'.code
            val candidate = if (keycapBase) next == 0xFE0F || next == 0x20E3
            else c0 >= 0x80 && (emojiByDefault(c0) || next == 0xFE0F)
            if (!candidate) { i += w0; continue }
            val cps = arrayListOf(c0)
            var j = i + w0
            if (isRegional(c0) && next != -1 && isRegional(next)) { cps += next; j += Character.charCount(next) }
            while (j < n) {
                val c = text.codePointAt(j)
                val w = Character.charCount(c)
                if (c == 0xFE0F || c == 0x20E3 || c in 0x1F3FB..0x1F3FF || c in 0xE0020..0xE007F) {
                    cps += c; j += w
                } else if (c == 0x200D && j + w < n && text.codePointAt(j + w) >= 0x2190) {
                    val c2 = text.codePointAt(j + w)
                    cps += c; cps += c2; j += w + Character.charCount(c2)
                } else break
            }
            val file = lookup(set, cps)
            if (file != null) { out += Span(i, j, file); i = j } else i += w0
        }
        return out
    }

    fun bitmap(ctx: Context, file: String): ImageBitmap? {
        cache.get(file)?.let { return it }
        val bmp = runCatching {
            ctx.assets.open("emoji/$file.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }.getOrNull()
        if (bmp != null) cache.put(file, bmp)
        return bmp
    }
}

@Composable
fun EmojiImage(file: String) {
    val ctx = LocalContext.current
    val bmp = remember(file) { Emoji.bitmap(ctx, file) }
    if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), filterQuality = FilterQuality.High)
}
