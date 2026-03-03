package com.example.scrollersdashboard

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scroll_data")
data class ScrollRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val appType: String,
    val scrollCount: Int,
    val screenTimeMillis: Long = 0L // New field for screen time
)

@Entity(tableName = "scroll_events")
data class ScrollEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val appType: String,
    val date: String
)

@Entity(tableName = "user_settings")
data class UserSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface ScrollDao {
    @Query("SELECT * FROM scroll_data WHERE date = :date AND appType = :appType LIMIT 1")
    suspend fun getRecord(date: String, appType: String): ScrollRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScrollRecord)

    @Query("UPDATE scroll_data SET scrollCount = scrollCount + 1 WHERE date = :date AND appType = :appType")
    suspend fun incrementScroll(date: String, appType: String)

    @Query("UPDATE scroll_data SET screenTimeMillis = screenTimeMillis + :addedTime WHERE date = :date AND appType = :appType")
    suspend fun addScreenTime(date: String, appType: String, addedTime: Long)

    @Query("UPDATE scroll_data SET scrollCount = 0 WHERE date = :date AND appType = :appType")
    suspend fun resetCount(date: String, appType: String)

    @Query("SELECT * FROM scroll_data WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<ScrollRecord>>

    @Query("SELECT * FROM scroll_data ORDER BY date DESC LIMIT 60")
    fun getRecentRecords(): Flow<List<ScrollRecord>>

    @Query("DELETE FROM scroll_data")
    suspend fun deleteAll()

    @Insert
    suspend fun insertEvent(event: ScrollEvent)

    @Query("SELECT * FROM scroll_events WHERE date = :date ORDER BY timestamp ASC")
    fun getEventsForDate(date: String): Flow<List<ScrollEvent>>

    @Query("DELETE FROM scroll_events WHERE date = :date AND appType = :appType")
    suspend fun deleteEventsForToday(date: String, appType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: UserSetting)

    @Query("SELECT value FROM user_settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM user_settings WHERE key = :key")
    fun getSettingFlow(key: String): Flow<String?>
}

@Database(entities = [ScrollRecord::class, ScrollEvent::class, UserSetting::class], version = 4) // Bumped version to 4
abstract class AppDatabase : RoomDatabase() {
    abstract fun scrollDao(): ScrollDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scroller-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
