package de.hamlookup.rufzeichen.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.ui.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Datenquellen", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        ToggleRow(
            title = "Internationale Suche (Server)",
            subtitle = "Erkennt das Land am Präfix und fragt automatisch die beste Quelle ab: " +
                "offiziell für Deutschland (BNetzA) und USA (FCC/Callook). Andere Länder nur, " +
                "wenn serverseitig eine Community-Quelle konfiguriert ist.",
            checked = settings.useBackend,
            onCheckedChange = { viewModel.update(settings.copy(useBackend = it)) }
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        ToggleRow(
            title = "BNetzA-Onlineabfrage (Gerät)",
            subtitle = "Direkte Abfrage der Bundesnetzagentur vom Gerät – auch für die " +
                "Platzhalter-Suche (z. B. db2*k) und als Fallback ohne Server.",
            checked = settings.useBnetza,
            onCheckedChange = { viewModel.update(settings.copy(useBnetza = it)) }
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        ToggleRow(
            title = "HamQTH (Gerät, international)",
            subtitle = "Weltweite Community-Datenbank direkt vom Gerät. Kostenloses Konto erforderlich.",
            checked = settings.useHamQth,
            onCheckedChange = { viewModel.update(settings.copy(useHamQth = it)) }
        )

        if (settings.useHamQth) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = settings.hamQthUser,
                onValueChange = { viewModel.update(settings.copy(hamQthUser = it)) },
                label = { Text("HamQTH-Benutzername") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = settings.hamQthPass,
                onValueChange = { viewModel.update(settings.copy(hamQthPass = it)) },
                label = { Text("HamQTH-Passwort") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Offline", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Die Rufzeichen-Analyse (Präfix, Land, geschätzte Klasse) funktioniert immer offline. " +
                "Zuvor online gefundene Rufzeichen werden lokal gespeichert und sind ohne Internet durchsuchbar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(
            "Hinweis: Offizielle Halterdaten stammen aus den amtlichen Quellen (BNetzA, FCC). " +
                "Community-Datenbanken sind als solche gekennzeichnet und werden nie als „behördlich " +
                "bestätigt“ dargestellt. Ein vorübergehender Ausfall einer Quelle bedeutet nicht, " +
                "dass ein Rufzeichen nicht existiert.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(0.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
