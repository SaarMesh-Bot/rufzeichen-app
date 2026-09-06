package de.hamlookup.rufzeichen.ui.tools

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.hamlookup.rufzeichen.data.local.QsoEntity
import de.hamlookup.rufzeichen.data.tools.CoaxData
import de.hamlookup.rufzeichen.data.tools.RadioMath
import de.hamlookup.rufzeichen.data.tools.Reference
import de.hamlookup.rufzeichen.ui.Loc
import de.hamlookup.rufzeichen.ui.ToolsViewModel
import de.hamlookup.rufzeichen.ui.bands.BandPlanScreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class ToolPage { HUB, CALC, REF, BANDS, LOG }

private fun String.toNum(): Double? = trim().replace(',', '.').toDoubleOrNull()
private fun fmt(v: Double, dec: Int = 2): String = "%.${dec}f".format(Locale.US, v)

@Composable
fun ToolsScreen(viewModel: ToolsViewModel) {
    var page by remember { mutableStateOf(ToolPage.HUB) }
    BackHandler(enabled = page != ToolPage.HUB) { page = ToolPage.HUB }

    Column(Modifier.fillMaxSize()) {
        if (page != ToolPage.HUB) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { page = ToolPage.HUB }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Loc.toolBack)
                }
                Text(
                    when (page) {
                        ToolPage.CALC -> Loc.toolCalc
                        ToolPage.REF -> Loc.toolRef
                        ToolPage.BANDS -> Loc.tabBands
                        ToolPage.LOG -> Loc.toolLog
                        else -> Loc.toolsTitle
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        when (page) {
            ToolPage.HUB -> Hub(onOpen = { page = it })
            ToolPage.CALC -> CalculatorsScreen()
            ToolPage.REF -> ReferenceScreen()
            ToolPage.BANDS -> BandPlanScreen()
            ToolPage.LOG -> LogbookScreen(viewModel)
        }
    }
}

@Composable
private fun Hub(onOpen: (ToolPage) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HubCard(Loc.toolCalc, Loc.toolCalcSub) { onOpen(ToolPage.CALC) }
        HubCard(Loc.toolRef, Loc.toolRefSub) { onOpen(ToolPage.REF) }
        HubCard(Loc.tabBands, Loc.toolBandsSub) { onOpen(ToolPage.BANDS) }
        HubCard(Loc.toolLog, Loc.toolLogSub) { onOpen(ToolPage.LOG) }
    }
}

@Composable
private fun HubCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- Calculators

@Composable
private fun CalculatorsScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AntennaCalc()
        PowerCalc()
        EirpCalc()
        CoaxCalc()
        Text(Loc.calcGuideline, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CalcCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AntennaCalc() {
    var f by remember { mutableStateOf("") }
    CalcCard(Loc.calcAntenna) {
        NumField(Loc.calcFreqMhz, f) { f = it }
        val mhz = f.toNum()
        if (mhz != null && mhz > 0) {
            Spacer(Modifier.height(10.dp))
            ResultRow(Loc.calcWavelength, "${fmt(RadioMath.wavelengthM(mhz))} m")
            ResultRow(Loc.calcDipole, "${fmt(RadioMath.dipoleHalfWaveM(mhz))} m")
            ResultRow(Loc.calcVertical, "${fmt(RadioMath.quarterWaveM(mhz))} m")
            ResultRow(Loc.calcLoop, "${fmt(RadioMath.fullWaveLoopM(mhz))} m")
        }
    }
}

@Composable
private fun PowerCalc() {
    var w by remember { mutableStateOf("") }
    var dbm by remember { mutableStateOf("") }
    CalcCard(Loc.calcPower) {
        NumField(Loc.calcWatt, w) { w = it }
        w.toNum()?.let { if (it > 0) ResultRow("→ dBm", fmt(RadioMath.wattToDbm(it), 1)) }
        Spacer(Modifier.height(8.dp))
        NumField("dBm", dbm) { dbm = it }
        dbm.toNum()?.let { ResultRow("→ ${Loc.calcWatt}", "${fmt(RadioMath.dbmToWatt(it), 3)} W") }
    }
}

@Composable
private fun EirpCalc() {
    var p by remember { mutableStateOf("") }
    var loss by remember { mutableStateOf("") }
    var gain by remember { mutableStateOf("") }
    CalcCard(Loc.calcEirp) {
        NumField(Loc.calcTxPower, p) { p = it }
        Spacer(Modifier.height(8.dp))
        NumField(Loc.calcCableLoss, loss) { loss = it }
        Spacer(Modifier.height(8.dp))
        NumField(Loc.calcGain, gain) { gain = it }
        val pw = p.toNum()
        if (pw != null && pw > 0) {
            val e = RadioMath.eirp(pw, loss.toNum() ?: 0.0, gain.toNum() ?: 0.0)
            Spacer(Modifier.height(10.dp))
            ResultRow("EIRP", "${fmt(e.eirpW, 1)} W  (${fmt(e.eirpDbm, 1)} dBm)")
            ResultRow(Loc.calcErp, "${fmt(e.erpW, 1)} W")
            Spacer(Modifier.height(6.dp))
            Text(Loc.calcHintN, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CoaxCalc() {
    var cable by remember { mutableStateOf(CoaxData.cables.first().name) }
    var len by remember { mutableStateOf("") }
    var f by remember { mutableStateOf("") }
    var p by remember { mutableStateOf("") }
    CalcCard(Loc.calcCoax) {
        Text(Loc.calcCable, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CoaxData.cables, key = { it.name }) { c ->
                FilterChip(selected = cable == c.name, onClick = { cable = c.name }, label = { Text(c.name) })
            }
        }
        Spacer(Modifier.height(10.dp))
        NumField(Loc.calcLengthM, len) { len = it }
        Spacer(Modifier.height(8.dp))
        NumField(Loc.calcFreqMhz, f) { f = it }
        Spacer(Modifier.height(8.dp))
        NumField(Loc.calcTxPower, p) { p = it }
        val cab = CoaxData.byName(cable)
        val lm = len.toNum(); val mhz = f.toNum()
        if (cab != null && lm != null && lm > 0 && mhz != null && mhz > 0) {
            val per100 = RadioMath.interpLossPer100m(cab.lossPer100m, mhz)
            val lossDb = per100 * lm / 100.0
            Spacer(Modifier.height(10.dp))
            ResultRow(Loc.calcLoss, "${fmt(lossDb, 2)} dB")
            p.toNum()?.let { if (it > 0) ResultRow(Loc.calcRemaining, "${fmt(RadioMath.powerAfterLossW(it, lossDb), 1)} W") }
        }
    }
}

// ------------------------------------------------------------------ Reference

@Composable
private fun ReferenceScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RefSection(Loc.refQ, Reference.qCodes())
        Spacer(Modifier.height(6.dp))
        RefSection(Loc.refRst, Reference.rst())
        Spacer(Modifier.height(6.dp))
        RefSection(Loc.refNato, Reference.nato())
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RefSection(title: String, entries: List<Reference.Entry>) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    entries.forEach { e ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                e.code,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(120.dp)
            )
            Text(e.meaning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

// -------------------------------------------------------------------- Logbook

@Composable
private fun LogbookScreen(viewModel: ToolsViewModel) {
    val qsos by viewModel.qsos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<QsoEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = Loc.logAdd)
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            FilledTonalButton(
                onClick = {
                    if (qsos.isEmpty()) {
                        Toast.makeText(context, Loc.logExportEmpty, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.exportAdif { adif -> shareAdif(context, adif) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(Loc.logExport) }

            Spacer(Modifier.height(12.dp))

            if (qsos.isEmpty()) {
                Text(Loc.logEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(qsos, key = { it.id }) { q ->
                        QsoRow(q, onDelete = { deleting = q })
                    }
                }
            }
        }
    }

    if (adding) {
        QsoDialog(onDismiss = { adding = false }, onSave = { viewModel.addQso(it); adding = false })
    }
    deleting?.let { q ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(Loc.logDeleteTitle) },
            text = { Text(Loc.logDeleteMsg) },
            confirmButton = { TextButton(onClick = { viewModel.deleteQso(q.id); deleting = null }) { Text(Loc.favDelete) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(Loc.favCancel) } }
        )
    }
}

@Composable
private fun QsoRow(q: QsoEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(q.callsign, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                val meta = listOfNotNull(
                    "${q.dateYmd} ${q.timeHm}z".trim(),
                    q.band, q.mode
                ).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val rst = listOfNotNull(
                    q.rstSent?.let { "S:$it" }, q.rstRcvd?.let { "R:$it" }, q.name, q.grid
                ).joinToString("  ")
                if (rst.isNotBlank()) Text(rst, style = MaterialTheme.typography.labelMedium)
                q.comment?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = Loc.favDelete, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QsoDialog(onDismiss: () -> Unit, onSave: (QsoEntity) -> Unit) {
    val utc = TimeZone.getTimeZone("UTC")
    val today = remember { SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = utc }.format(Date()) }
    val nowHm = remember { SimpleDateFormat("HHmm", Locale.US).apply { timeZone = utc }.format(Date()) }

    var call by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var time by remember { mutableStateOf(nowHm) }
    var band by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("SSB") }
    var rs by remember { mutableStateOf("59") }
    var rr by remember { mutableStateOf("59") }
    var name by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Loc.logAdd) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Field(Loc.logCallsign, call) { call = it.uppercase() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(Loc.logDate, date, Modifier.weight(1f), KeyboardType.Number) { date = it }
                    Field(Loc.logTime, time, Modifier.weight(1f), KeyboardType.Number) { time = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(Loc.logBand, band, Modifier.weight(1f)) { band = it }
                    Field(Loc.logMode, mode, Modifier.weight(1f)) { mode = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(Loc.logRstSent, rs, Modifier.weight(1f)) { rs = it }
                    Field(Loc.logRstRcvd, rr, Modifier.weight(1f)) { rr = it }
                }
                Field(Loc.logName, name) { name = it }
                Field(Loc.logGrid, grid) { grid = it.uppercase() }
                Field(Loc.logComment, comment) { comment = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = call.isNotBlank(),
                onClick = {
                    onSave(
                        QsoEntity(
                            callsign = call.trim().uppercase(),
                            dateYmd = date.trim(),
                            timeHm = time.trim(),
                            band = band.trim().ifBlank { null },
                            mode = mode.trim().ifBlank { null },
                            rstSent = rs.trim().ifBlank { null },
                            rstRcvd = rr.trim().ifBlank { null },
                            name = name.trim().ifBlank { null },
                            grid = grid.trim().ifBlank { null },
                            comment = comment.trim().ifBlank { null }
                        )
                    )
                }
            ) { Text(Loc.favSave) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Loc.favCancel) } }
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

private fun shareAdif(context: Context, adif: String) {
    try {
        val file = File(context.cacheDir, "rufzeichen_log.adi")
        file.writeText(adif)
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rufzeichen Log.adi")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, Loc.logShareTitle))
    } catch (e: Exception) {
        Toast.makeText(context, e.message ?: "Export error", Toast.LENGTH_SHORT).show()
    }
}
