package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_history")
data class GameRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val player1: String = "Vishwanath",
    val player2: String = "AI Bot",
    val result: String, // "White Wins", "Black Wins", "Draw", "Ongoing"
    val isOngoing: Boolean = false,
    val serializedBoard: String, // representation from ChessEngine
    val activeColor: String = "WHITE",
    val diffLevel: Int = 1, // 1=Easy, 2=Medium
    val moveCount: Int = 0
)

@Dao
interface GameHistoryDao {
    @Query("SELECT * FROM game_history ORDER BY date DESC")
    fun getAllHistory(): Flow<List<GameRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecord): Long

    @Query("DELETE FROM game_history WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM game_history")
    suspend fun clearAll()
}

@Database(entities = [GameRecord::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameHistoryDao(): GameHistoryDao
}

class GameRepository(private val dao: GameHistoryDao) {
    val allHistory: Flow<List<GameRecord>> = dao.getAllHistory()

    suspend fun insert(record: GameRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun delete(id: Int) {
        dao.deleteRecordById(id)
    }

    suspend fun clear() {
        dao.clearAll()
    }
}
