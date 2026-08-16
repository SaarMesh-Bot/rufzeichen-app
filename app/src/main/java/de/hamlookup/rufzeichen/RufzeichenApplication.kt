package de.hamlookup.rufzeichen

import android.app.Application
import de.hamlookup.rufzeichen.data.local.AppDatabase
import de.hamlookup.rufzeichen.data.repository.CallsignRepository
import de.hamlookup.rufzeichen.data.repository.SettingsRepository

/**
 * Minimal manual dependency container. Keeps the project free of a DI
 * framework while still sharing single instances of the repositories.
 */
class RufzeichenApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var callsignRepository: CallsignRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
        callsignRepository = CallsignRepository(
            context = this,
            dao = db.callsignDao(),
            settingsRepo = settingsRepository
        )
    }
}
