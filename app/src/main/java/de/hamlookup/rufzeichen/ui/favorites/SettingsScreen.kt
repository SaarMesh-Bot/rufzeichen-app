package de.hamlookup.rufzeichen.ui.favorites

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.ui.SettingsViewModel
import de.hamlookup.rufzeichen.ui.detail.LocatorPickerMap

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionLabel = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            "${pi.versionName} ($code)"
        }.getOrDefault("")
    }
    var showPicker by remember { mutableStateOf(false) }
    var callText by remember { mutableStateOf(settings.ownCallsign) }
    var callFocused by remember { mutableStateOf(false) }
    var locText by remember { mutableStateOf(settings.ownLocator) }
    var locFocused by remember { mutableStateOf(false) }
    LaunchedEffect(settings.ownCallsign, callFocused) {
        if (!callFocused && callText != settings.ownCallsign) callText = settings.ownCallsign
    }
    LaunchedEffect(settings.ownLocator, locFocused) {
        if (!locFocused && locText != settings.ownLocator) locText = settings.ownLocator
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Mein Standort", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Für Entfernung und Peilung zu gesuchten Stationen. Der Locator ist ein " +
                "Maidenhead-Kenner (z. B. JN39 oder JN39KF).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = callText,
            onValueChange = {
                val t = it.uppercase()
                callText = t
                viewModel.update(settings.copy(ownCallsign = t))
            },
            label = { Text("Eigenes Rufzeichen (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().onFocusChanged { callFocused = it.isFocused }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = locText,
            onValueChange = {
                val t = it.uppercase().replace(" ", "")
                locText = t
                viewModel.update(settings.copy(ownLocator = t))
            },
            label = { Text("Eigener Locator (Maidenhead)") },
            placeholder = { Text("z. B. JN39KF") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().onFocusChanged { locFocused = it.isFocused }
        )
        TextButton(onClick = { showPicker = true }) { Text("Standort auf Karte wählen") }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Text("Datenquellen", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        ToggleRow(
            title = "Internationale Suche (Server)",
            subtitle = "Erkennt das Land am Präfix und fragt automatisch die beste Quelle ab: " +
                "offiziell für Deutschland (BNetzA), USA (FCC/Callook) und Kanada (ISED). " +
                "Andere Länder nur, " +
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
            PersistedTextField(
                value = settings.hamQthUser,
                onCommit = { viewModel.update(settings.copy(hamQthUser = it)) },
                label = "HamQTH-Benutzername"
            )
            Spacer(Modifier.height(8.dp))
            PersistedTextField(
                value = settings.hamQthPass,
                onCommit = { viewModel.update(settings.copy(hamQthPass = it)) },
                label = "HamQTH-Passwort",
                password = true
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

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text(
            "Geodaten © OpenStreetMap-Mitwirkende (ODbL). Der QTH-Locator wird " +
                "serverseitig aus der Anschrift über OpenStreetMap/Nominatim ermittelt.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Über", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        if (versionLabel.isNotBlank()) {
            Text(
                "Version $versionLabel",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            "Entwickelt von Mathias Kasper",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Kontakt & Feedback: app@saarmesh.de",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                val subject = "Rufzeichen $versionLabel – Feedback"
                val intent = Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:app@saarmesh.de?subject=" + Uri.encode(subject))
                )
                runCatching { context.startActivity(intent) }
            }
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Datenschutzerklärung",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://saarmesh-bot.github.io/rufzeichen-app/datenschutz.html")
                )
                runCatching { context.startActivity(intent) }
            }
        )
    }

    if (showPicker) {
        var picked by remember { mutableStateOf(locText) }
        Dialog(onDismissRequest = { showPicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Standort auf der Karte wählen", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tippe auf deine Position. Gewählt: " + picked.ifBlank { "—" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LocatorPickerMap(
                        initialLocator = locText.ifBlank { null },
                        onPicked = { picked = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showPicker = false }) { Text("Abbrechen") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                locText = picked
                                viewModel.update(settings.copy(ownLocator = picked))
                                showPicker = false
                            },
                            enabled = picked.isNotBlank()
                        ) { Text("Übernehmen") }
                    }
                }
            }
        }
    }
}

/**
 * Text field backed by *local* state so typing is smooth. The persisted value
 * (from DataStore, delivered asynchronously) is only synced into the field when
 * the field is not focused – this prevents the cursor from jumping and keeps
 * fast keystrokes (incl. digits) from being dropped by the async round-trip.
 */
@Composable
private fun PersistedTextField(
    value: String,
    onCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    password: Boolean = false,
    capitalize: Boolean = false,
    transform: (String) -> String = { it }
) {
    var text by remember { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }

    // Adopt external changes only while the user is not editing this field.
    LaunchedEffect(value, focused) {
        if (!focused && text != value) text = value
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            val t = transform(it)
            text = t
            onCommit(t)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (capitalize)
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters) else KeyboardOptions.Default,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
    )
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
