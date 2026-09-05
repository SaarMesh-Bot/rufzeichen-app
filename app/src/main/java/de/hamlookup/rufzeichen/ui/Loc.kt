package de.hamlookup.rufzeichen.ui

import java.util.Locale

/**
 * Lightweight in-app localisation. Returns English when the device language is
 * English, otherwise German (the app's default). Kept as a single object so all
 * user-facing UI strings live in one place and can be used from Composables as
 * well as from non-Composable layers (ViewModel / repository).
 */
object Loc {
    private val en: Boolean get() = Locale.getDefault().language == "en"
    private fun s(de: String, en: String): String = if (this.en) en else de
    /** Public bilingual picker for data layers (e.g. band-plan text). */
    fun pick(de: String, en: String): String = s(de, en)

    // ---- navigation / general ------------------------------------------------
    val appBarTitle get() = s("Rufzeichen · Amateurfunk", "Rufzeichen · Amateur Radio")
    val tabSearch get() = s("Suche", "Search")
    val tabFavorites get() = s("Favoriten", "Favorites")
    val tabSettings get() = s("Einstellungen", "Settings")
    val tabBands get() = s("Bänder", "Bands")
    val amateurRadio get() = s("Amateurfunk", "Amateur Radio")

    // ---- search --------------------------------------------------------------
    val searchLabel get() = s("Rufzeichen", "Call sign")
    val searchPlaceholder get() = s("z. B. DL1ABC, W1AW oder db2*k", "e.g. DL1ABC, W1AW or db2*k")
    val clearInput get() = s("Eingabe löschen", "Clear input")
    val searchHistory get() = s("Suchverlauf", "Search history")
    val searching get() = s("Suche läuft …", "Searching …")
    fun errorPrefix(e: String) = s("Fehler: $e", "Error: $e")
    val emptySearch get() = s(
        "Gib ein Rufzeichen ein. '*' ist als Platzhalter für ein Zeichen erlaubt. " +
            "Beim Tippen erscheint dein Verlauf; das Verlauf-Symbol zeigt die ganze Liste.",
        "Enter a call sign. '*' is a wildcard for one character. Your history appears " +
            "as you type; the history icon shows the full list."
    )
    val noResults get() = s("Keine Treffer gefunden.", "No matches found.")
    val noHistory get() = s("Noch keine Suchanfragen.", "No searches yet.")
    val historyTitle get() = s("Verlauf", "History")
    val clearAll get() = s("Alle löschen", "Clear all")
    fun deleteFromHistory(q: String) = s("„$q“ aus dem Verlauf löschen", "Remove “$q” from history")

    // ---- favorites -----------------------------------------------------------
    val emptyFavorites get() = s(
        "Noch keine Favoriten gespeichert. Tippe in der Detailansicht auf den Stern.",
        "No favorites yet. Tap the star in the detail view."
    )

    // ---- provenance chips ----------------------------------------------------
    fun officialSource(name: String) = s("✓ Offizielle Quelle: $name", "✓ Official source: $name")
    fun communityData(name: String) = s("Community-Daten: $name", "Community data: $name")
    fun sourceGeneric(name: String) = s("Quelle: $name", "Source: $name")

    // ---- detail --------------------------------------------------------------
    val share get() = s("Teilen", "Share")
    val removeFavorite get() = s("Aus Favoriten entfernen", "Remove from favorites")
    val addFavorite get() = s("Zu Favoriten", "Add to favorites")
    val sectionAllocation get() = s("Zuteilungsdaten", "Allocation data")
    val rowHolder get() = s("Inhaber", "Holder")
    val rowClass get() = s("Klasse", "Class")
    val rowStatus get() = s("Status", "Status")
    val rowLocation get() = s("Standort", "Location")
    val rowCountry get() = s("Land", "Country")
    val rowLocator get() = s("Locator", "Locator")
    val callsignLabel get() = s("Rufzeichen", "Call sign")
    val sectionMap get() = s("Standort (Karte)", "Location (map)")
    val mapAttrPrefix get() = s(
        "Karte: © OpenStreetMap-Mitwirkende (ODbL). Position gemäß ",
        "Map: © OpenStreetMap contributors (ODbL). Position based on "
    )
    val mapAttrAddress get() = s("ermittelter Anschrift.", "the resolved address.")
    val mapAttrLocator get() = s("QTH-Locator (ungefähr).", "the QTH locator (approx.).")
    val mapAttrLine get() = s(
        " Blaue Linie: Großkreis von deinem Standort.",
        " Blue line: great-circle path from your location."
    )
    val openInMap get() = s("In Karte öffnen", "Open in map")
    val ownStation get() = s("Das ist dein eigener Standort.", "This is your own location.")
    val sectionDistBearing get() = s("Entfernung & Peilung", "Distance & bearing")
    val rowDistance get() = s("Entfernung", "Distance")
    val rowBearingShort get() = s("Peilung (Kurzpfad)", "Bearing (short path)")
    val rowBearingLong get() = s("Peilung (Langpfad)", "Bearing (long path)")
    val rowFromYourQth get() = s("von deinem Standort", "from your location")
    val tipSetLocator get() = s(
        "Tipp: Hinterlege deinen Standort (Locator) in den Einstellungen, um " +
            "Entfernung und Peilung zu dieser Station zu sehen.",
        "Tip: set your location (locator) in Settings to see distance and bearing to this station."
    )
    val sectionAnalysis get() = s("Rufzeichen-Analyse (offline)", "Call sign analysis (offline)")
    val rowPrefix get() = s("Präfix", "Prefix")
    val rowDigit get() = s("Ziffer", "Digit")
    val rowSuffix get() = s("Suffix", "Suffix")
    val rowCountryItu get() = s("Land (ITU)", "Country (ITU)")
    val rowContinent get() = s("Kontinent", "Continent")
    val rowCqZone get() = s("CQ-Zone", "CQ zone")
    val rowItuZone get() = s("ITU-Zone", "ITU zone")
    val rowClassEstimated get() = s("Klasse (geschätzt)", "Class (estimated)")
    val rowCurrentLocation get() = s("Aktueller Standort", "Current location")
    val rowOperatingMode get() = s("Betriebsart", "Operating mode")
    fun copied(label: String) = s("$label kopiert", "$label copied")

    fun continentName(code: String) = when (code.uppercase()) {
        "EU" -> s("Europa (EU)", "Europe (EU)")
        "AS" -> s("Asien (AS)", "Asia (AS)")
        "NA" -> s("Nordamerika (NA)", "North America (NA)")
        "SA" -> s("Südamerika (SA)", "South America (SA)")
        "AF" -> s("Afrika (AF)", "Africa (AF)")
        "OC" -> s("Ozeanien (OC)", "Oceania (OC)")
        "AN" -> s("Antarktis (AN)", "Antarctica (AN)")
        else -> code
    }

    // ---- share text ----------------------------------------------------------
    fun shareClass(v: String) = s("Klasse: $v", "Class: $v")
    fun shareCountry(v: String) = s("Land: $v", "Country: $v")
    fun shareLocation(v: String) = s("Standort: $v", "Location: $v")
    fun shareLocator(v: String) = s("Locator: $v", "Locator: $v")
    fun shareCurrentIn(v: String) = s("Aktuell in: $v", "Currently in: $v")
    fun shareMode(v: String) = s("Betriebsart: $v", "Operating mode: $v")
    fun shareSource(name: String, official: Boolean) =
        s("Quelle: $name" + if (official) " (offiziell)" else "",
          "Source: $name" + if (official) " (official)" else "")
    val shareFooter get() = s("via Rufzeichen – Amateurfunk", "via Rufzeichen – Amateur Radio")

    // ---- settings ------------------------------------------------------------
    val headMyLocation get() = s("Mein Standort", "My location")
    val myLocationHint get() = s(
        "Für Entfernung und Peilung zu gesuchten Stationen. Der Locator ist ein " +
            "Maidenhead-Kenner (z. B. JN39 oder JN39KF).",
        "For distance and bearing to searched stations. The locator is a Maidenhead " +
            "grid square (e.g. JN39 or JN39KF)."
    )
    val ownCallsignLabel get() = s("Eigenes Rufzeichen (optional)", "Your call sign (optional)")
    val ownLocatorLabel get() = s("Eigener Locator (Maidenhead)", "Your locator (Maidenhead)")
    val locatorPlaceholder get() = s("z. B. JN39KF", "e.g. JN39KF")
    val pickOnMap get() = s("Standort auf Karte wählen", "Choose location on map")
    val headSources get() = s("Datenquellen", "Data sources")
    val toggleServerTitle get() = s("Internationale Suche (Server)", "International search (server)")
    val toggleServerSub get() = s(
        "Erkennt das Land am Präfix und fragt automatisch die beste Quelle ab: " +
            "offiziell für Deutschland (BNetzA), USA (FCC/Callook), Kanada (ISED), " +
            "Finnland (Traficom), Ungarn (NMHH), Tschechien (ČTÚ), Norwegen (Nkom), " +
            "Litauen (RRT), Rumänien (ANCOM) und Polen (UKE). Andere Länder nur, wenn serverseitig " +
            "eine Community-Quelle konfiguriert ist.",
        "Detects the country from the prefix and automatically queries the best source: " +
            "official for Germany (BNetzA), USA (FCC/Callook), Canada (ISED), " +
            "Finland (Traficom), Hungary (NMHH), Czechia (ČTÚ), Norway (Nkom), " +
            "Lithuania (RRT), Romania (ANCOM) and Poland (UKE). Other countries only if a " +
            "community source is configured on the server."
    )
    val toggleBnetzaTitle get() = s("BNetzA-Onlineabfrage (Gerät)", "BNetzA online lookup (device)")
    val toggleBnetzaSub get() = s(
        "Direkte Abfrage der Bundesnetzagentur vom Gerät – auch für die " +
            "Platzhalter-Suche (z. B. db2*k) und als Fallback ohne Server.",
        "Direct lookup at the German regulator (BNetzA) from the device – also for the " +
            "wildcard search (e.g. db2*k) and as a fallback without the server."
    )
    val toggleHamqthTitle get() = s("HamQTH (Gerät, international)", "HamQTH (device, international)")
    val toggleHamqthSub get() = s(
        "Weltweite Community-Datenbank direkt vom Gerät. Kostenloses Konto erforderlich.",
        "Worldwide community database directly from the device. A free account is required."
    )
    val hamqthUser get() = s("HamQTH-Benutzername", "HamQTH username")
    val hamqthPass get() = s("HamQTH-Passwort", "HamQTH password")
    val headOffline get() = s("Offline", "Offline")
    val offlineText get() = s(
        "Die Rufzeichen-Analyse (Präfix, Land, geschätzte Klasse) funktioniert immer offline. " +
            "Zuvor online gefundene Rufzeichen werden lokal gespeichert und sind ohne Internet durchsuchbar.",
        "The call sign analysis (prefix, country, estimated class) always works offline. " +
            "Call signs found online before are stored locally and remain searchable without internet."
    )
    val noteSources get() = s(
        "Hinweis: Offizielle Halterdaten stammen aus den amtlichen Quellen (BNetzA, FCC). " +
            "Community-Datenbanken sind als solche gekennzeichnet und werden nie als „behördlich " +
            "bestätigt“ dargestellt. Ein vorübergehender Ausfall einer Quelle bedeutet nicht, " +
            "dass ein Rufzeichen nicht existiert.",
        "Note: official holder data comes from the official sources (BNetzA, FCC). Community " +
            "databases are marked as such and are never shown as “officially confirmed”. A temporary " +
            "outage of a source does not mean a call sign does not exist."
    )
    val osmAttr get() = s(
        "Geodaten © OpenStreetMap-Mitwirkende (ODbL). Der QTH-Locator wird " +
            "serverseitig aus der Anschrift über OpenStreetMap/Nominatim ermittelt.",
        "Geodata © OpenStreetMap contributors (ODbL). The QTH locator is derived on the " +
            "server from the address via OpenStreetMap/Nominatim."
    )
    val headAbout get() = s("Über", "About")
    fun versionLine(v: String) = s("Version $v", "Version $v")
    val developedBy get() = s("Entwickelt von Mathias Kasper", "Developed by Mathias Kasper")
    val contactFeedback get() = s("Kontakt & Feedback: app@saarmesh.de", "Contact & feedback: app@saarmesh.de")
    fun feedbackSubject(v: String) = s("Rufzeichen $v – Feedback", "Rufzeichen $v – feedback")
    val privacyPolicy get() = s("Datenschutzerklärung", "Privacy policy")
    val pickTitle get() = s("Standort auf der Karte wählen", "Choose location on the map")
    fun pickHint(sel: String) = s("Tippe auf deine Position. Gewählt: $sel", "Tap your position. Selected: $sel")
    val cancel get() = s("Abbrechen", "Cancel")
    val apply get() = s("Übernehmen", "Apply")

    // ---- messages (repository / viewmodel) -----------------------------------
    val unknownError get() = s("Unbekannter Fehler", "Unknown error")
    val offlineResults get() = s(
        "Offline – Ergebnisse aus lokalem Cache und Analyse.",
        "Offline – results from local cache and analysis."
    )
    val noOnlineHits get() = s(
        "Keine Online-Treffer – nur Offline-Analyse.",
        "No online matches – offline analysis only."
    )

    // ---- band plans ----------------------------------------------------------
    val bandsTitle get() = s("Bandpläne & Rechte", "Band plans & privileges")
    val bandsIntroDE get() = s(
        "Maximale Sendeleistung je Amateurfunkband nach Zeugnisklasse (A / E / N).",
        "Maximum transmit power per amateur band by licence class (A / E / N)."
    )
    val bandsColBand get() = s("Band", "Band")
    val bandsColRange get() = s("Frequenz", "Frequency")
    val bandsClassSummary get() = s("Klassen im Überblick", "Classes at a glance")
    val bandsClassN get() = s(
        "Klasse N (Einsteiger, seit 2024): 10 m, 2 m, 70 cm – nur in Deutschland gültig.",
        "Class N (entry, since 2024): 10 m, 2 m, 70 cm – valid in Germany only."
    )
    val bandsClassE get() = s(
        "Klasse E: Kurzwelle (160/80/15/10 m) plus alle UKW-Bänder, bis 100 W. CEPT-Novice.",
        "Class E: HF (160/80/15/10 m) plus all VHF/UHF bands, up to 100 W. CEPT Novice."
    )
    val bandsClassA get() = s(
        "Klasse A: alle Amateurbänder, bis 750 W. CEPT/HAREC (weltweit).",
        "Class A: all amateur bands, up to 750 W. CEPT/HAREC (worldwide)."
    )
    val bandsDisclaimer get() = s(
        "Angaben ohne Gewähr. Maßgeblich ist der amtliche Frequenzplan des jeweiligen Landes " +
            "(DE: BNetzA, US: FCC, CA: ISED).",
        "Provided without guarantee. The authoritative source is each country's official band plan " +
            "(DE: BNetzA, US: FCC, CA: ISED)."
    )

    // ---- favorites: distance & notes -----------------------------------------
    val favSortAdded get() = s("Zuletzt hinzugefügt", "Recently added")
    val favSortDistance get() = s("Nach Entfernung", "By distance")
    val favSortLabel get() = s("Sortierung", "Sort")
    val favNote get() = s("Notiz", "Note")
    val favNoteHint get() = s("Notiz zu diesem Rufzeichen …", "Note for this call sign …")
    val favEditNote get() = s("Notiz bearbeiten", "Edit note")
    val favSave get() = s("Speichern", "Save")
    val favCancel get() = s("Abbrechen", "Cancel")
    fun favDistanceKm(km: Int) = s("$km km entfernt", "$km km away")
    val favEditFavorite get() = s("Favorit bearbeiten", "Edit favorite")
    val favListAll get() = s("Alle", "All")
    val favListLabel get() = s("Liste", "List")
    val favNoList get() = s("Keine", "None")
    val favNewList get() = s("Neue Liste", "New list")
    val favNewListShort get() = s("+ Neu", "+ New")
    val favListNameHint get() = s("Listenname", "List name")
    val favCreate get() = s("Anlegen", "Create")
    val favDelete get() = s("Löschen", "Delete")
    val favDeleteListTitle get() = s("Liste löschen", "Delete list")
    fun favDeleteListMsg(name: String) = s(
        "Liste „$name“ löschen? Die enthaltenen Favoriten bleiben erhalten und werden nur nicht mehr gruppiert.",
        "Delete list “$name”? The favorites in it are kept and simply become ungrouped."
    )
    val favManageHint get() = s("Lange auf eine Liste tippen, um sie zu löschen.", "Long-press a list to delete it.")
    val favNoQth get() = s(
        "Für die Entfernungssortierung im Reiter „Einstellungen\" deinen Standort setzen.",
        "Set your location in the Settings tab to sort favorites by distance."
    )
}
