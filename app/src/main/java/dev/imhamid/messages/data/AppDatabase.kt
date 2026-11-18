package dev.imhamid.messages.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, ChatPref::class, BlockedEntity::class, ScheduledEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messages(): MessageDao
    abstract fun prefs(): PrefDao
    abstract fun blocked(): BlockedDao
    abstract fun scheduled(): ScheduledDao

    companion object {
        fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "messages.db").build()
    }
}
