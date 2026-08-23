package de.hamlookup.rufzeichen.data.model

/** Identifies where a result came from. */
enum class DataSourceType(val label: String) {
    BNETZA("BNetzA"),
    HAMQTH("HamQTH"),
    BACKEND("Server"),      // international lookup via the hamapi backend
    OFFLINE("Offline-Analyse")
}

/**
 * A single call sign lookup result. Fields are nullable because different
 * data sources provide different levels of detail. [analysis] is always
 * populated from the offline analyzer so the UI can show something even
 * when no network source returns data.
 *
 * The lower block of fields is populated by the international backend
 * ([sourceName]/[official] carry the concrete provider and whether it is an
 * authoritative/official source vs. a community database).
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
    val analysis: CallsignAnalysis? = null,
    // --- international / backend enrichment (all optional) ---
    val countryCode: String? = null,
    val licenseStatus: String? = null,
    val locator: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sourceName: String? = null,       // e.g. "BNetzA", "Callook", "QRZ.com"
    val official: Boolean? = null         // true = official authority, false = community
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
    val continent: String? = null,      // EU, AS, NA, SA, AF, OC, AN
    val cqZone: Int? = null,            // nominal CQ zone (null if it varies)
    val ituZone: Int? = null,           // nominal ITU zone (null if it varies)
    val notes: List<String> = emptyList()
)
