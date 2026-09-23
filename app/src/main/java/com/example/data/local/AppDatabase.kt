package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SmsQueueEntity::class,
        CallQueueEntity::class,
        HostMessageEntity::class,
        HostCallEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsQueueDao(): SmsQueueDao
    abstract fun callQueueDao(): CallQueueDao
    abstract fun hostMessageDao(): HostMessageDao
    abstract fun hostCallDao(): HostCallDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_host_messages_hostCode_receivedAt` ON `host_messages` (`hostCode`, `receivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_host_messages_receivedAt` ON `host_messages` (`receivedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_host_calls_hostCode_timestamp` ON `host_calls` (`hostCode`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_host_calls_timestamp` ON `host_calls` (`timestamp`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_bridge.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
