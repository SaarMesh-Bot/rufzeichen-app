package de.hamlookup.rufzeichen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.hamlookup.rufzeichen.ui.AppViewModelFactory
import de.hamlookup.rufzeichen.ui.navigation.MainScreen
import de.hamlookup.rufzeichen.ui.theme.RufzeichenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RufzeichenApplication
        val factory = AppViewModelFactory(
            callsignRepository = app.callsignRepository,
            settingsRepository = app.settingsRepository
        )

        setContent {
            RufzeichenTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(factory = factory)
                }
            }
        }
    }
}
