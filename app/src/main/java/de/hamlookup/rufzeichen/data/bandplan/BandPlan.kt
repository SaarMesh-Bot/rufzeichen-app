package de.hamlookup.rufzeichen.data.bandplan

import de.hamlookup.rufzeichen.ui.Loc

/**
 * Offline amateur band plans / privileges by licence class.
 *
 * Germany is given as a full per-band table with the maximum power for each
 * class (A / E / N) taken from the BNetzA frequency plan (AFuV, Vfg. as of
 * 2024/2025). USA and Canada are given as per-class privilege summaries, since
 * their allocations are defined by sub-bands rather than a compact grid.
 *
 * These tables are a convenience reference only — the authoritative source is
 * always the national regulator's official band plan.
 */
object BandPlan {

    /** One amateur band with the max power per German class ("–" = not allowed). */
    data class BandRow(
        val band: String,
        val range: String,
        val classA: String,
        val classE: String,
        val classN: String
    )

    /** A licence class with a short summary and its privileges (US/CA). */
    data class ClassInfo(
        val name: String,
        val summary: String
    )

    const val NO = "–"

    /** Germany — per-band maximum power by class (BNetzA). */
    val germany: List<BandRow> = listOf(
        BandRow("2200 m", "135,7–137,8 kHz", "1 W ERP", NO, NO),
        BandRow("630 m", "472–479 kHz", "1 W ERP", NO, NO),
        BandRow("160 m", "1,810–2,000 MHz", "750 W", "100 W", NO),
        BandRow("80 m", "3,500–3,800 MHz", "750 W", "100 W", NO),
        BandRow("60 m", "5351,5–5366,5 kHz", "15 W EIRP", NO, NO),
        BandRow("40 m", "7,000–7,200 MHz", "750 W", NO, NO),
        BandRow("30 m", "10,100–10,150 MHz", "150 W", NO, NO),
        BandRow("20 m", "14,000–14,350 MHz", "750 W", NO, NO),
        BandRow("17 m", "18,068–18,168 MHz", "750 W", NO, NO),
        BandRow("15 m", "21,000–21,450 MHz", "750 W", "100 W", NO),
        BandRow("12 m", "24,890–24,990 MHz", "750 W", NO, NO),
        BandRow("10 m", "28,000–29,700 MHz", "750 W", "100 W", "10 W ERP"),
        BandRow("6 m", "50,000–52,000 MHz", "750 W", NO, NO),
        BandRow("2 m", "144–146 MHz", "750 W", "75 W", "6,1 W ERP"),
        BandRow("70 cm", "430–440 MHz", "750 W", "75 W", "6,1 W ERP"),
        BandRow("23 cm", "1240–1300 MHz", "750 W", "75 W", NO),
        BandRow("13 cm", "2320–2450 MHz", "75 W", "5 W", NO),
        BandRow("≥ 9 cm", "3,4 GHz–250 GHz", "75 W", "5 W", NO)
    )

    /** USA — FCC privileges by class. */
    fun usa(): List<ClassInfo> = listOf(
        ClassInfo(
            "Technician",
            Loc.pick(
                "Alle VHF/UHF-Bänder. Auf Kurzwelle nur eingeschränkt: 10 m SSB (28,3–28,5 MHz), " +
                    "CW-Segmente auf 80/40/15 m, Digimodes auf 10 m. Bis 1500 W PEP (200 W auf KW).",
                "All VHF/UHF bands. Limited HF: 10 m SSB (28.3–28.5 MHz), CW segments on 80/40/15 m, " +
                    "digital on 10 m. Up to 1500 W PEP (200 W on HF)."
            )
        ),
        ClassInfo(
            "General",
            Loc.pick(
                "Große Teile aller Kurzwellenbänder (in Sub-Bändern) plus alle VHF/UHF-Bänder. Bis 1500 W PEP.",
                "Large portions of all HF bands (in sub-bands) plus all VHF/UHF bands. Up to 1500 W PEP."
            )
        ),
        ClassInfo(
            "Amateur Extra",
            Loc.pick(
                "Zugang zu allen Amateurfrequenzen ohne Einschränkung. Bis 1500 W PEP.",
                "Access to all amateur frequencies without restriction. Up to 1500 W PEP."
            )
        )
    )

    /** Canada — ISED privileges by class. */
    fun canada(): List<ClassInfo> = listOf(
        ClassInfo(
            "Basic",
            Loc.pick(
                "Alle Bänder oberhalb 30 MHz (VHF/UHF/SHF). Kurzwelle erst mit „Basic with Honours\" " +
                    "(≥ 80 %) oder Zusatzqualifikation. Bis 250 W.",
                "All bands above 30 MHz (VHF/UHF/SHF). HF only with \"Basic with Honours\" (≥ 80%) or " +
                    "further qualification. Up to 250 W."
            )
        ),
        ClassInfo(
            "Basic with Honours",
            Loc.pick(
                "Wie Basic, zusätzlich Zugang zu allen Kurzwellenbändern.",
                "Like Basic, plus access to all HF bands."
            )
        ),
        ClassInfo(
            "Advanced",
            Loc.pick(
                "Alle Amateurbänder, höchste Leistung (bis 2250 W DC-Input); darf Sender selbst bauen " +
                    "sowie Relais- und Klubstationen betreiben.",
                "All amateur bands, highest power (up to 2250 W DC input); may build transmitters and " +
                    "operate repeater and club stations."
            )
        )
    )
}
