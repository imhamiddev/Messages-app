package dev.imhamid.messages.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import dev.imhamid.messages.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

data class Contact(val name: String, val number: String)

class ContactsRepo(private val ctx: Context) {
    private val cache = ConcurrentHashMap<String, String>()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private val photoCache = ConcurrentHashMap<String, Optional<ImageBitmap>>()

    fun clearCache() {
        cache.clear()
        photoCache.clear()
    }

    /** Contact photo for an address (full-size if available, else thumbnail), or null. Cached. */
    suspend fun photoFor(address: String): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!hasPermission() || address.isBlank()) return@withContext null
        val key = PhoneUtils.key(address)
        photoCache[key]?.let { return@withContext it.orElse(null) }
        val bmp = runCatching { loadPhoto(address) }.getOrNull()
        photoCache[key] = Optional.ofNullable(bmp)
        bmp
    }

    private fun loadPhoto(address: String): ImageBitmap? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
        var full: String? = null
        var thumb: String? = null
        ctx.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.PHOTO_URI, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI),
            null, null, null
        )?.use {
            if (it.moveToFirst()) {
                full = it.getString(0)
                thumb = it.getString(1)
            }
        }
        for (u in listOfNotNull(full, thumb)) {
            decode(Uri.parse(u))?.let { return it }
        }
        return null
    }

    private fun decode(uri: Uri): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > 320) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }?.asImageBitmap()
    }

    /** Contact display name for an address, or null when unknown. Cached; call off the main thread. */
    fun nameFor(address: String): String? {
        if (!hasPermission() || address.isBlank()) return null
        val key = PhoneUtils.key(address)
        cache[key]?.let { return it.ifEmpty { null } }
        val name = lookup(address)
        cache[key] = name ?: ""
        return name
    }

    private fun lookup(address: String): String? = runCatching {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
        ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()

    /** Live search by name or (partial) phone number. */
    suspend fun search(query: String): List<Contact> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val qn = PhoneUtils.normalizeDigits(query).trim()
        val numeric = qn.isNotEmpty() && qn.all { it.isDigit() || it in "+ -()" }
        val digitsQ = qn.filter { it.isDigit() }.trimStart('0')
        val byName = qn.isNotEmpty() && !numeric
        val out = LinkedHashMap<String, Contact>()
        runCatching {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                if (byName) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?" else null,
                if (byName) arrayOf("%$qn%") else null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE LOCALIZED ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val num = c.getString(1) ?: continue
                    if (numeric && digitsQ.isNotEmpty()) {
                        val d = PhoneUtils.normalizeDigits(num).filter { it.isDigit() }
                        if (!d.contains(digitsQ)) continue
                    }
                    val k = PhoneUtils.key(num)
                    if (!out.containsKey(k)) out[k] = Contact(name, num)
                }
            }
        }
        out.values.take(60).toList()
    }
}
