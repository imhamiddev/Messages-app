package dev.imhamid.messages.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, ChatPref::class, BlockedEntity::class, ScheduledEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messages(): MessageDao
    abstract fun prefs(): PrefDao
    abstract fun blocked(): BlockedDao
    abstract fun scheduled(): ScheduledDao

    companion object {
        fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "messages.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_prefs ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
