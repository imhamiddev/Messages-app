package dev.imhamid.messages.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TYPE_INBOX = 1
const val TYPE_SENT = 2
const val TYPE_SENDING = 4
const val TYPE_FAILED = 5

/** Marker for messages the user emptied from the trash but that still exist in the system SMS store. */
const val HIDDEN = Long.MAX_VALUE

const val SCHED_PENDING = 0
const val SCHED_SENT = 1
const val SCHED_FAILED = 2

@Entity(
    tableName = "messages",
    indices = [Index("threadKey"), Index(value = ["providerId"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: Long? = null,
    val threadKey: String,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean = true,
    val subId: Int = -1,
    val blocked: Boolean = false,
    val deletedAt: Long? = null
) {
    val incoming: Boolean get() = type == TYPE_INBOX
}

@Entity(tableName = "chat_prefs")
data class ChatPref(
    @PrimaryKey val threadKey: String,
    val pinned: Boolean = false,
    val pinnedAt: Long = 0,
    val simSubId: Int? = null,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false
)

@Entity(tableName = "blocked")
data class BlockedEntity(
    @PrimaryKey val threadKey: String,
    val address: String,
    val blockedAt: Long
)

@Entity(tableName = "scheduled")
data class ScheduledEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadKey: String,
    val address: String,
    val body: String,
    val sendAt: Long,
    val subId: Int = -1,
    val state: Int = SCHED_PENDING,
    val createdAt: Long
)

data class UnreadCount(val threadKey: String, val cnt: Int)
