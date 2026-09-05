package de.hamlookup.rufzeichen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FavoriteEntity::class, FavoriteListEntity::class, HistoryEntity::class, CachedCallsignEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callsignDao(): CallsignDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN locator TEXT")
                db.execSQL("ALTER TABLE favorites ADD COLUMN lat REAL")
                db.execSQL("ALTER TABLE favorites ADD COLUMN lon REAL")
                db.execSQL("ALTER TABLE favorites ADD COLUMN note TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN listName TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS favorite_lists (" +
                    "name TEXT NOT NULL PRIMARY KEY, createdAt INTEGER NOT NULL)")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rufzeichen.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
