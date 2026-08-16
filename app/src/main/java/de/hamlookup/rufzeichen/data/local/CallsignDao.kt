package de.hamlookup.rufzeichen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallsignDao {

    // ---- Favorites ----
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE callsign = :callsign)")
    fun isFavorite(callsign: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE callsign = :callsign")
    suspend fun removeFavorite(callsign: String)

    // ---- History ----
    @Query("SELECT * FROM history ORDER BY searchedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(entry: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    // ---- Offline cache ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cache(entries: List<CachedCallsignEntity>)

    @Query("SELECT * FROM cache WHERE callsign LIKE :pattern ORDER BY callsign LIMIT 100")
    suspend fun searchCache(pattern: String): List<CachedCallsignEntity>

    @Query("SELECT * FROM cache WHERE callsign = :callsign LIMIT 1")
    suspend fun getCached(callsign: String): CachedCallsignEntity?
}
