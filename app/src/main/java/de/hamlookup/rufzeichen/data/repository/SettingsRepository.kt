package de.hamlookup.rufzeichen.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

const val DEFAULT_BACKEND_URL = "https://live.saarmesh.de/hamapi/api/v1"

/** Holds the current, resolved settings snapshot. */
data class Settings(
    val useBnetza: Boolean = true,
    val useHamQth: Boolean = false,
    val hamQthUser: String = "",
    val hamQthPass: String = "",
    // International lookup via the backend (country detection + official/community).
    val useBackend: Boolean = true,
    val backendUrl: String = DEFAULT_BACKEND_URL,
    // Own station profile (for distance/bearing). Locator is a Maidenhead grid.
    val ownCallsign: String = "",
    val ownLocator: String = "",
    // Exact coordinates when picked on the map (else null -> use locator centre).
    val ownLat: Double? = null,
    val ownLon: Double? = null
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USE_BNETZA = booleanPreferencesKey("use_bnetza")
        val USE_HAMQTH = booleanPreferencesKey("use_hamqth")
        val HAMQTH_USER = stringPreferencesKey("hamqth_user")
        val HAMQTH_PASS = stringPreferencesKey("hamqth_pass")
        val USE_BACKEND = booleanPreferencesKey("use_backend")
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val OWN_CALLSIGN = stringPreferencesKey("own_callsign")
        val OWN_LOCATOR = stringPreferencesKey("own_locator")
        val OWN_LAT = doublePreferencesKey("own_lat")
        val OWN_LON = doublePreferencesKey("own_lon")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            useBnetza = p[Keys.USE_BNETZA] ?: true,
            useHamQth = p[Keys.USE_HAMQTH] ?: false,
            hamQthUser = p[Keys.HAMQTH_USER] ?: "",
            hamQthPass = p[Keys.HAMQTH_PASS] ?: "",
            useBackend = p[Keys.USE_BACKEND] ?: true,
            backendUrl = (p[Keys.BACKEND_URL] ?: DEFAULT_BACKEND_URL).ifBlank { DEFAULT_BACKEND_URL },
            ownCallsign = p[Keys.OWN_CALLSIGN] ?: "",
            ownLocator = p[Keys.OWN_LOCATOR] ?: "",
            ownLat = p[Keys.OWN_LAT],
            ownLon = p[Keys.OWN_LON]
        )
    }

    suspend fun update(settings: Settings) {
        context.dataStore.edit { p ->
            p[Keys.USE_BNETZA] = settings.useBnetza
            p[Keys.USE_HAMQTH] = settings.useHamQth
            p[Keys.HAMQTH_USER] = settings.hamQthUser
            p[Keys.HAMQTH_PASS] = settings.hamQthPass
            p[Keys.USE_BACKEND] = settings.useBackend
            p[Keys.BACKEND_URL] = settings.backendUrl
            p[Keys.OWN_CALLSIGN] = settings.ownCallsign
            p[Keys.OWN_LOCATOR] = settings.ownLocator
            val lat = settings.ownLat
            val lon = settings.ownLon
            if (lat != null && lon != null) {
                p[Keys.OWN_LAT] = lat
                p[Keys.OWN_LON] = lon
            } else {
                p.remove(Keys.OWN_LAT)
                p.remove(Keys.OWN_LON)
            }
        }
    }
}
