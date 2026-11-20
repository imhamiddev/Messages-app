package dev.imhamid.messages.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(m: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<MessageEntity>)

    @Query("SELECT providerId FROM messages WHERE providerId IS NOT NULL")
    suspend fun knownProviderIds(): List<Long>

    @Query(
        "SELECT * FROM messages WHERE id IN (" +
            "SELECT id FROM (SELECT id, MAX(date) FROM messages " +
            "WHERE deletedAt IS NULL AND blocked = 0 GROUP BY threadKey)) " +
            "ORDER BY date DESC"
    )
    fun observeThreads(): Flow<List<MessageEntity>>

    @Query(
        "SELECT threadKey, COUNT(*) AS cnt FROM messages " +
            "WHERE deletedAt IS NULL AND blocked = 0 AND read = 0 AND type = 1 GROUP BY threadKey"
    )
    fun observeUnread(): Flow<List<UnreadCount>>

    @Query(
        "SELECT * FROM messages WHERE deletedAt IS NULL AND blocked = 0 " +
            "AND body LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY date DESC, id DESC LIMIT 300"
    )
    suspend fun search(q: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE threadKey = :key AND deletedAt IS NULL ORDER BY date DESC, id DESC")
    fun observeThread(key: String): Flow<List<MessageEntity>>

    @Query("SELECT providerId FROM messages WHERE threadKey = :key AND read = 0 AND providerId IS NOT NULL")
    suspend fun unreadProviderIds(key: String): List<Long>

    @Query("UPDATE messages SET read = 1 WHERE threadKey = :key AND read = 0")
    suspend fun markRead(key: String)

    @Query("UPDATE messages SET deletedAt = :now WHERE id IN (:ids)")
    suspend fun trashIds(ids: List<Long>, now: Long)

    @Query("UPDATE messages SET deletedAt = :now WHERE threadKey IN (:keys) AND deletedAt IS NULL")
    suspend fun trashThreads(keys: List<String>, now: Long)

    @Query("UPDATE messages SET deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restore(ids: List<Long>)

    @Query("UPDATE messages SET deletedAt = NULL WHERE threadKey IN (:keys) AND deletedAt = :at")
    suspend fun restoreThreads(keys: List<String>, at: Long)

    @Query("SELECT * FROM messages WHERE deletedAt IS NOT NULL AND deletedAt < 9223372036854775807 ORDER BY deletedAt DESC, date DESC")
    fun observeTrash(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE deletedAt IS NOT NULL AND deletedAt < 9223372036854775807")
    suspend fun allTrash(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun expiredTrash(before: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    suspend fun deleteRows(ids: List<Long>)

    @Query("UPDATE messages SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: Int)

    @Query("UPDATE messages SET type = :type WHERE id = :id AND type = 4")
    suspend fun setTypeIfSending(id: Long, type: Int)

    @Query("UPDATE messages SET blocked = :blocked WHERE threadKey = :key")
    suspend fun setBlocked(key: String, blocked: Boolean)
}

@Dao
interface PrefDao {
    @Query("SELECT * FROM chat_prefs")
    fun observeAll(): Flow<List<ChatPref>>

    @Query("SELECT * FROM chat_prefs WHERE threadKey = :key")
    fun observe(key: String): Flow<ChatPref?>

    @Query("SELECT * FROM chat_prefs WHERE threadKey = :key")
    suspend fun get(key: String): ChatPref?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ChatPref)
}

@Dao
interface BlockedDao {
    @Query("SELECT * FROM blocked ORDER BY blockedAt DESC")
    fun observeAll(): Flow<List<BlockedEntity>>

    @Query("SELECT COUNT(*) FROM blocked WHERE threadKey = :key")
    suspend fun count(key: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(b: BlockedEntity)

    @Query("DELETE FROM blocked WHERE threadKey = :key")
    suspend fun remove(key: String)

    @Query("SELECT threadKey FROM blocked")
    suspend fun keys(): List<String>
}

@Dao
interface ScheduledDao {
    @Insert
    suspend fun insert(s: ScheduledEntity): Long

    @Query("SELECT * FROM scheduled ORDER BY state ASC, sendAt ASC")
    fun observeAll(): Flow<List<ScheduledEntity>>

    @Query("SELECT * FROM scheduled WHERE id = :id")
    suspend fun get(id: Long): ScheduledEntity?

    @Query("SELECT * FROM scheduled WHERE state = 0")
    suspend fun pending(): List<ScheduledEntity>

    @Query("UPDATE scheduled SET state = :state WHERE id = :id")
    suspend fun setState(id: Long, state: Int)

    @Query("DELETE FROM scheduled WHERE id = :id")
    suspend fun delete(id: Long)
}
