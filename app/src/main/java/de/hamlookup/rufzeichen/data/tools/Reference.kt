package de.hamlookup.rufzeichen.data.tools

import de.hamlookup.rufzeichen.ui.Loc

/** Offline operating reference: Q-codes, the RST system and the NATO alphabet. */
object Reference {

    data class Entry(val code: String, val meaning: String)

    /** Common amateur Q-codes with a short plain-language meaning. */
    fun qCodes(): List<Entry> = listOf(
        Entry("QRL", Loc.pick("Bist du beschäftigt? / Frequenz belegt", "Are you busy? / frequency in use")),
        Entry("QRM", Loc.pick("Störung durch andere Stationen", "Interference from other stations")),
        Entry("QRN", Loc.pick("Atmosphärische Störungen (Prasseln)", "Atmospheric noise / static")),
        Entry("QRO", Loc.pick("Sendeleistung erhöhen", "Increase transmit power")),
        Entry("QRP", Loc.pick("Sendeleistung verringern / Kleinleistung", "Reduce power / low power")),
        Entry("QRT", Loc.pick("Sendebetrieb einstellen", "Stop sending / going off air")),
        Entry("QRV", Loc.pick("Bin bereit / empfangsbereit", "I am ready")),
        Entry("QRX", Loc.pick("Warte / rufe wieder um …", "Stand by / wait")),
        Entry("QRZ", Loc.pick("Wer ruft mich?", "Who is calling me?")),
        Entry("QRG", Loc.pick("Genaue Frequenz", "Your exact frequency")),
        Entry("QSB", Loc.pick("Signalschwankungen (Fading)", "Signal fading")),
        Entry("QSL", Loc.pick("Empfang bestätigt / Bestätigungskarte", "Acknowledge receipt / QSL card")),
        Entry("QSO", Loc.pick("Funkverbindung / Gespräch", "A contact / conversation")),
        Entry("QSY", Loc.pick("Frequenz wechseln", "Change frequency")),
        Entry("QTH", Loc.pick("Standort", "Location / position")),
        Entry("QTC", Loc.pick("Ich habe Nachrichten", "I have messages to send")),
        Entry("QRK", Loc.pick("Lesbarkeit meines Signals", "Readability of your signal")),
        Entry("QSK", Loc.pick("Höre in Sendepausen (Break-in)", "I can hear you between my signals"))
    )

    /** RST reporting system explained. */
    fun rst(): List<Entry> = listOf(
        Entry("R — Readability 1–5", Loc.pick("Lesbarkeit: 1 unlesbar … 5 einwandfrei lesbar", "Readability: 1 unreadable … 5 perfectly readable")),
        Entry("S — Strength 1–9", Loc.pick("Signalstärke: 1 kaum hörbar … 9 sehr stark", "Signal strength: 1 barely perceptible … 9 very strong")),
        Entry("T — Tone 1–9", Loc.pick("Tonqualität (nur CW): 1 rau … 9 reiner Ton", "Tone (CW only): 1 rough … 9 pure tone")),
        Entry(Loc.pick("Beispiel: 59", "Example: 59"), Loc.pick("Gut lesbar und sehr stark (SSB). Bei CW z. B. 599.", "Perfectly readable and very strong (SSB). CW e.g. 599."))
    )

    /** NATO / ICAO phonetic alphabet (language-neutral). */
    fun nato(): List<Entry> = listOf(
        Entry("A", "Alfa"), Entry("B", "Bravo"), Entry("C", "Charlie"), Entry("D", "Delta"),
        Entry("E", "Echo"), Entry("F", "Foxtrot"), Entry("G", "Golf"), Entry("H", "Hotel"),
        Entry("I", "India"), Entry("J", "Juliett"), Entry("K", "Kilo"), Entry("L", "Lima"),
        Entry("M", "Mike"), Entry("N", "November"), Entry("O", "Oscar"), Entry("P", "Papa"),
        Entry("Q", "Quebec"), Entry("R", "Romeo"), Entry("S", "Sierra"), Entry("T", "Tango"),
        Entry("U", "Uniform"), Entry("V", "Victor"), Entry("W", "Whiskey"), Entry("X", "X-ray"),
        Entry("Y", "Yankee"), Entry("Z", "Zulu"),
        Entry("0", "Zero"), Entry("1", "One"), Entry("2", "Two"), Entry("3", "Three"),
        Entry("4", "Four"), Entry("5", "Five"), Entry("6", "Six"), Entry("7", "Seven"),
        Entry("8", "Eight"), Entry("9", "Nine")
    )
}
