package de.hamlookup.rufzeichen.data.model

/** Identifies where a result came from. */
enum class DataSourceType(val label: String) {
    BNETZA("BNetzA"),
    HAMQTH("HamQTH"),
    OFFLINE("Offline-Analyse")
}

/**
 * A single call sign lookup result. Fields are nullable because different
 * data sources provide different levels of detail. [analysis] is always
 * populated from the offline analyzer so the UI can show something even
 * when no network source returns data.
 */
data class Callsign(
    val callsign: String,
    val holderName: String? = null,
    val licenceClass: String? = null,
    val qth: String? = null,
    val country: String? = null,
    val country2: String? = null,
    val extra: Map<String, String> = emptyMap(),
    val sources: Set<DataSourceType> = emptySet(),
    val analysis: CallsignAnalysis? = null
)

/** Purely offline-derived facts about a call sign. */
data class CallsignAnalysis(
    val normalized: String,
    val prefix: String,
    val number: String,
    val suffix: String,
    val country: String,
    val countryCode: String,
    val isGerman: Boolean,
    val germanClass: String? = null,
    val notes: List<String> = emptyList()
)
