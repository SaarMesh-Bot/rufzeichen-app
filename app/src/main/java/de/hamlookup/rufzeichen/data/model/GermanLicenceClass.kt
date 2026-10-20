package de.hamlookup.rufzeichen.data.model

import de.hamlookup.rufzeichen.ui.Loc

/**
 * Zentrale Definition der deutschen Amateurfunk-Zeugnisklassen nach der
 * AFuV-Novelle 2024. Einzige Quelle für die Klassenbezeichnungen – Suche,
 * Detailansicht, Favoriten, Offline-Analyse und Bänder-Übersicht greifen alle
 * hierauf zu, damit die Stufen überall gleich heißen.
 *
 *  - N: Einstiegsklasse (seit 2024)
 *  - E: mittlere Stufe
 *  - A: volle Klasse
 */
enum class GermanLicenceClass(
    val code: Char,
    private val tierDe: String,
    private val tierEn: String
) {
    N('N', "Einstiegsklasse", "entry level"),
    E('E', "mittlere Stufe", "intermediate"),
    A('A', "volle Klasse", "full licence");

    /** Anzeigename, z. B. „Klasse E (mittlere Stufe)“. */
    val label: String get() = Loc.pick("Klasse $code ($tierDe)", "Class $code ($tierEn)")

    companion object {
        fun fromCode(c: Char): GermanLicenceClass? =
            entries.firstOrNull { it.code == c.uppercaseChar() }

        fun fromCode(code: String?): GermanLicenceClass? =
            code?.trim()?.singleOrNull()?.let { fromCode(it) }

        // Matches German labels incl. older app versions ("Klasse E (Einsteiger)")
        // and this app's own English labels. Foreign "Class A" etc. from other
        // countries' authorities are deliberately NOT matched.
        private val LABEL_DE = Regex("^Klasse\\s+([AEN])(?:\\s*\\([^)]*\\))?$", RegexOption.IGNORE_CASE)
        private val LABEL_EN = Regex("^Class\\s+([AEN])\\s*\\((?:entry level|intermediate|full licence)\\)$", RegexOption.IGNORE_CASE)

        /**
         * Bringt eine gespeicherte Klassenbezeichnung (z. B. aus Cache oder
         * Favoriten einer älteren App-Version) auf die aktuelle Bezeichnung.
         * Alles, was keine deutsche Klasse ist, bleibt unverändert.
         */
        fun normalize(label: String?): String? {
            if (label.isNullOrBlank()) return label
            val t = label.trim()
            val m = LABEL_DE.matchEntire(t) ?: LABEL_EN.matchEntire(t) ?: return label
            return fromCode(m.groupValues[1])?.label ?: label
        }
    }
}
