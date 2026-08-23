package de.hamlookup.rufzeichen

import android.app.Application
import de.hamlookup.rufzeichen.data.local.AppDatabase
import de.hamlookup.rufzeichen.data.repository.CallsignRepository
import de.hamlookup.rufzeichen.data.repository.SettingsRepository
import org.osmdroid.config.Configuration

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

        // osmdroid: a descriptive User-Agent is required by the OpenStreetMap
        // tile usage policy (the default value gets blocked), and the tile
        // cache lives in the app-private storage so no extra permission and no
        // shared-storage clutter is needed.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osm_tiles")
        }

        val db = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
        callsignRepository = CallsignRepository(
            context = this,
            dao = db.callsignDao(),
            settingsRepo = settingsRepository
        )
    }
}
