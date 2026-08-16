package de.hamlookup.rufzeichen.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Holds the current, resolved settings snapshot. */
data class Settings(
    val useBnetza: Boolean = true,
    val useHamQth: Boolean = false,
    val hamQthUser: String = "",
    val hamQthPass: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USE_BNETZA = booleanPreferencesKey("use_bnetza")
        val USE_HAMQTH = booleanPreferencesKey("use_hamqth")
        val HAMQTH_USER = stringPreferencesKey("hamqth_user")
        val HAMQTH_PASS = stringPreferencesKey("hamqth_pass")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            useBnetza = p[Keys.USE_BNETZA] ?: true,
            useHamQth = p[Keys.USE_HAMQTH] ?: false,
            hamQthUser = p[Keys.HAMQTH_USER] ?: "",
            hamQthPass = p[Keys.HAMQTH_PASS] ?: ""
        )
    }

    suspend fun update(settings: Settings) {
        context.dataStore.edit { p ->
            p[Keys.USE_BNETZA] = settings.useBnetza
            p[Keys.USE_HAMQTH] = settings.useHamQth
            p[Keys.HAMQTH_USER] = settings.hamQthUser
            p[Keys.HAMQTH_PASS] = settings.hamQthPass
        }
    }
}
