package de.hamlookup.rufzeichen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A call sign the user marked as favorite. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val callsign: String,
    val holderName: String?,
    val licenceClass: String?,
    val qth: String?,
    val country: String?,
    val locator: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val note: String? = null,
    val listName: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

/** A user-defined favourite list (folder) used to group favourites. */
@Entity(tableName = "favorite_lists")
data class FavoriteListEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** A past search query. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val resultCount: Int,
    val searchedAt: Long = System.currentTimeMillis()
)

/**
 * A cached lookup result, so previously found call signs are searchable
 * offline. [extraJson] holds the detail map as a JSON object; [sourcesCsv]
 * the source labels.
 */
@Entity(tableName = "cache")
data class CachedCallsignEntity(
    @PrimaryKey val callsign: String,
    val holderName: String?,
    val licenceClass: String?,
    val qth: String?,
    val country: String?,
    val extraJson: String,
    val sourcesCsv: String,
    val cachedAt: Long = System.currentTimeMillis()
)
