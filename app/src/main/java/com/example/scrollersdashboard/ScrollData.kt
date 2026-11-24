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
import androidx.room.Transaction
import androidx.room.Index
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scroll_data", indices = [Index(value = ["date", "appType"], unique = true)])
data class ScrollRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val appType: String,
    val scrollCount: Int,
    val screenTimeMillis: Long = 0L
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

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val date: String // resets daily
)

@Entity(tableName = "habit_tasks")
data class HabitTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val lastCompletedDate: String = "" // Items remain, completion resets
)

@Dao
interface ScrollDao {
    @Query("SELECT * FROM scroll_data WHERE date = :date AND appType = :appType LIMIT 1")
    suspend fun getRecord(date: String, appType: String): ScrollRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScrollRecord)

    @Query("UPDATE scroll_data SET scrollCount = scrollCount + 1 WHERE date = :date AND appType = :appType")
    suspend fun incrementScroll(date: String, appType: String)

    @Transaction
    suspend fun recordScroll(event: ScrollEvent): Int {
        val existing = getRecord(event.date, event.appType)
        if (existing == null) {
            insert(ScrollRecord(date = event.date, appType = event.appType, scrollCount = 1))
        } else {
            incrementScroll(event.date, event.appType)
        }
        insertEvent(event)
        return (existing?.scrollCount ?: 0) + 1
    }

    @Transaction
    suspend fun setScreenTime(date: String, appType: String, totalMillis: Long) {
        val existing = getRecord(date, appType)
        if (existing == null) insert(ScrollRecord(date = date, appType = appType, scrollCount = 0, screenTimeMillis = totalMillis))
        else insert(existing.copy(screenTimeMillis = totalMillis))
    }

    @Query("UPDATE scroll_data SET screenTimeMillis = screenTimeMillis + :addedTime WHERE date = :date AND appType = :appType")
    suspend fun addScreenTime(date: String, appType: String, addedTime: Long)

    @Query("SELECT * FROM scroll_data WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<ScrollRecord>>

    @Query("SELECT * FROM scroll_data ORDER BY date DESC LIMIT 180")
    fun getRecentRecords(): Flow<List<ScrollRecord>>

    @Query("SELECT * FROM scroll_data ORDER BY date DESC")
    fun getAllRecords(): Flow<List<ScrollRecord>>

    @Insert
    suspend fun insertEvent(event: ScrollEvent)

    @Query("SELECT * FROM scroll_events WHERE date = :date ORDER BY timestamp ASC")
    fun getEventsForDate(date: String): Flow<List<ScrollEvent>>

    @Query("SELECT * FROM scroll_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<ScrollEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: UserSetting)

    @Query("SELECT value FROM user_settings WHERE key = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT value FROM user_settings WHERE key = :key")
    fun getSettingFlow(key: String): Flow<String?>

    // --- Todo Tasks ---
    @Query("SELECT * FROM todo_tasks WHERE date = :date ORDER BY id DESC")
    fun getTodoTasks(date: String): Flow<List<TodoTask>>

    @Query("SELECT * FROM todo_tasks ORDER BY id DESC")
    fun getAllTodoTasks(): Flow<List<TodoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(task: TodoTask)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteTodo(id: Int)

    @Query("DELETE FROM todo_tasks WHERE date < :today")
    suspend fun deleteOldTodos(today: String)

    // --- Habit Tasks ---
    @Query("SELECT * FROM habit_tasks ORDER BY id DESC")
    fun getHabitTasks(): Flow<List<HabitTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitTask)

    @Query("DELETE FROM habit_tasks WHERE id = :id")
    suspend fun deleteHabit(id: Int)
}

@Database(entities = [ScrollRecord::class, ScrollEvent::class, UserSetting::class, TodoTask::class, HabitTask::class], version = 6)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scrollDao(): ScrollDao

    companion object {
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `todo_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `date` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `lastCompletedDate` TEXT NOT NULL)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE scroll_data SET scrollCount = (SELECT MAX(other.scrollCount) FROM scroll_data AS other WHERE other.date = scroll_data.date AND other.appType = scroll_data.appType), screenTimeMillis = (SELECT MAX(other.screenTimeMillis) FROM scroll_data AS other WHERE other.date = scroll_data.date AND other.appType = scroll_data.appType)")
                db.execSQL("DELETE FROM scroll_data WHERE id NOT IN (SELECT MIN(id) FROM scroll_data GROUP BY date, appType)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scroll_data_date_appType ON scroll_data (date, appType)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scroller-db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
