package com.ifts18.unadecisionmas.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ifts18.unadecisionmas.data.model.DecisionRecord
import kotlinx.coroutines.flow.Flow

/**
 * Entidad Room para guardar el progreso de una partida en curso (Checkpoint).
 * Permite cerrar la app a mitad de semana y continuarla otro día.
 */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val id: Int = 1,
    val playerName: String,
    val avatarResId: Int,
    val currentDayIndex: Int,
    val currentDebt: Int,
    val currentSavings: Int,
    val wellbeingPoints: Int,
    val correctDecisionsCount: Int,
    val incorrectDecisionsCount: Int,
    val totalDecisionTimeMillis: Long,
    val serializedHistory: String,
    val betMomMoneyOnTuesday: Boolean = false,
    val serializedSessionAchievements: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private const val RECORD_SEP = "|||"
        private const val FIELD_SEP = ":::"

        fun serializeHistory(records: List<DecisionRecord>): String {
            return records.joinToString(RECORD_SEP) { r ->
                listOf(
                    r.dayName,
                    r.chosenOptionText.replace(FIELD_SEP, " "),
                    r.isSafe.toString(),
                    r.addedDebt.toString(),
                    r.savingsDelta.toString(),
                    r.wellbeingDelta.toString(),
                    r.responseTimeMillis.toString(),
                    r.feedback.replace(FIELD_SEP, " ")
                ).joinToString(FIELD_SEP)
            }
        }

        fun deserializeHistory(raw: String): List<DecisionRecord> {
            if (raw.isBlank()) return emptyList()
            return raw.split(RECORD_SEP).mapNotNull { item ->
                val parts = item.split(FIELD_SEP)
                if (parts.size >= 8) {
                    DecisionRecord(
                        dayName = parts[0],
                        chosenOptionText = parts[1],
                        isSafe = parts[2].toBooleanStrictOrNull() ?: true,
                        addedDebt = parts[3].toIntOrNull() ?: 0,
                        savingsDelta = parts[4].toIntOrNull() ?: 0,
                        wellbeingDelta = parts[5].toIntOrNull() ?: 0,
                        responseTimeMillis = parts[6].toLongOrNull() ?: 0L,
                        feedback = parts[7]
                    )
                } else null
            }
        }
    }
}

/**
 * Entidad Room para almacenar las medallas y logros desbloqueados localmente
 * (incluyendo el Easter Egg SIXSEVEN desbloqueado por código QR).
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long = System.currentTimeMillis()
)

/**
 * Entidad Room para almacenar el historial local de puntajes y actuar como caché offline
 * cuando no hay conectividad con Firebase.
 */
@Entity(tableName = "local_scores")
data class LocalScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String,
    val playerName: String,
    val avatarIndex: Int,
    val score: Int,
    val debt: Int,
    val savings: Int,
    val wellbeingPoints: Int,
    val totalTimeMillis: Long,
    val achievementsCount: Int,
    val hasSixSeven: Boolean,
    val gameMode: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

/**
 * Entidad Room para conservar el progreso en curso de los minijuegos (Tragamonedas y Ruleta)
 * al salir al menú principal o cerrar la app.
 */
@Entity(tableName = "minigame_progress")
data class MinigameProgressEntity(
    @PrimaryKey val gameKey: String,
    val spinsUsed: Int,
    val bonusChips: Int,
    val fakeLockedWinnings: Int,
    val realMoneyLost: Int,
    val elapsedMillis: Long,
    val reel1Name: String = "SEVEN",
    val reel2Name: String = "DIAMOND",
    val reel3Name: String = "CHERRY",
    val selectedNumberIndex: Int = 30,
    val selectedBetMode: String = "PLENO",
    val landedPocketIndex: Int = 30,
    val statusBanner: String = "",
    val stageTitle: String = "",
    val stageExplanation: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface GameDao {

    @Query("SELECT * FROM saved_game WHERE id = 1 LIMIT 1")
    fun observeSavedGame(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_game WHERE id = 1 LIMIT 1")
    suspend fun getSavedGame(): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameProgress(entity: SavedGameEntity)

    @Query("DELETE FROM saved_game")
    suspend fun clearSavedGame()

    @Query("SELECT * FROM minigame_progress WHERE gameKey = :gameKey LIMIT 1")
    suspend fun getMinigameProgress(gameKey: String): MinigameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMinigameProgress(entity: MinigameProgressEntity)

    @Query("DELETE FROM minigame_progress WHERE gameKey = :gameKey")
    suspend fun clearMinigameProgress(gameKey: String)

    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockAchievement(entity: AchievementEntity)

    @Query("SELECT * FROM local_scores ORDER BY score DESC, totalTimeMillis ASC LIMIT 50")
    fun observeTopScores(): Flow<List<LocalScoreEntity>>

    @Query("SELECT * FROM local_scores WHERE isSynced = 0")
    suspend fun getUnsyncedScores(): List<LocalScoreEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(entity: LocalScoreEntity): Long

    @Query("UPDATE local_scores SET isSynced = 1 WHERE id = :localId")
    suspend fun markScoreSynced(localId: Long)
}

@Database(
    entities = [
        SavedGameEntity::class,
        AchievementEntity::class,
        LocalScoreEntity::class,
        MinigameProgressEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "juego_limpio_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
