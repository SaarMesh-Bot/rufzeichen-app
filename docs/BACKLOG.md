# Backlog – Ideen für kommende Versionen

Vorgemerkt, aber noch nicht umgesetzt.

## Werkzeuge / Rechner

- [ ] **Koax-Rechner: Kabel „Hyperflex 5" ergänzen** (Messi & Paoloni Hyperflex 5, ~5 mm Low-Loss).
      Ort: `app/src/main/java/de/hamlookup/rufzeichen/data/tools/CoaxData.kt` – als weiteres
      `Cable(...)` in die Liste aufnehmen. Dämpfung (dB/100 m) an den Stützstellen 28 / 144 / 432 / 1296 MHz
      **aus dem offiziellen M&P-Datenblatt übernehmen** (die untenstehenden Werte sind grobe Richtwerte und
      vor dem Einbau zu prüfen):
      `Cable("Hyperflex 5", listOf(28.0 to 4.0, 144.0 to 9.2, 432.0 to 16.5, 1296.0 to 30.0))`
      Notiert: 2026-09-08.

## Weitere Werkzeug-Ideen (aus der Multitool-Diskussion)

- [ ] Funkwetter / Ausbreitung (SFI, K/A-Index, Sonnenflecken, Grayline) über Backend-Proxy zu NOAA/HamQSL.
- [ ] Locator- & Antennen-Tools: Locator↔Koordinaten, Entfernung/Peilung zwischen zwei beliebigen Punkten,
      Kompass-Ausrichtung zum Ziel-Locator per Gerätesensor.
- [ ] Relaissuche „in der Nähe" (DE-Repeaterliste, Import wie die Länderregister, nach Entfernung/Band).
- [ ] Satelliten-Überflüge (TLE/SGP4) – aufwändigster Brocken.

## Suche / Detailansicht

- [ ] **Weitere Rufzeichen derselben Person anzeigen.** Wenn ein Rufzeichen mit Halterdaten
      gefunden wurde, in der Detailansicht einen Abschnitt „Weitere Rufzeichen dieser Person"
      zeigen und die zugehörigen Calls verlinken (antippbar → Detailansicht).
      Beispiel: DK6IEC (Andreas Bender) hält auch DN6IEC (Ausbildungsrufzeichen).
      Notiert: 2026-09-09.

      Zuordnungsregel (Wunsch): Gleichheit **nur bei Name UND Anschrift** festlegen (nicht Name allein),
      um Verwechslungen bei Namensgleichheit auszuschließen.

      Machbarkeit / Umsetzung:
      - Umkehrsuche über **Name + Anschrift** (→ Liste der Calls, aktuellen Call ausschließen).
        Direkt machbar für Länder mit lokalem Volldatensatz inkl. Name/Ort: **HU, NO, RO, PL** (Klub).
      - **Deutschland (BNetzA): Quelle gefunden.** Die BNetzA veröffentlicht die vollständige
        „Rufzeichenliste" als PDF (öffentlich, gemeinfrei nach § 5 UrhG):
        https://data.bundesnetzagentur.de/Bundesnetzagentur/SharedDocs/Downloads/DE/Sachgebiete/Telekommunikation/Unternehmen_Institutionen/Frequenzen/Amateurfunk/Rufzeichenliste/rufzeichenliste_afu.pdf
        Stand geprüft 2026-09-09: ~9,6 MB, 684 Seiten, datiert 26.08.2026 (wird periodisch aktualisiert).
        Zeilenformat je Eintrag: `RUFZEICHEN , KLASSE, Name[; Straße Nr, PLZ Ort]`
        Beispiel: `DA1AA , A, Norman Czora; Leipziger Str. 212, 38124 Braunschweig`.
        Hinweise: Zeilen brechen im PDF um (beim Parsen zusammenfügen, bis zum nächsten Call-Muster);
        Anschrift ist optional (nicht alle Halter geben sie frei) → dann kann kein DE-Match über Adresse
        erfolgen; mehrere Abschnitte (personengebundene Rufzeichen, Klubstationen, Ausbildungscalls DN…).
      - Umsetzungsidee DE: PDF serverseitig parsen (pypdf/pdftotext) und `de_afu.sqlite`
        (callsign, class, name, address) bauen (wöchentlicher Timer wie die anderen Länder).
        Das ermöglicht (a) die Personen-Verknüpfung über exakt gleiche (Name+Anschrift) und
        optional (b) einen schnelleren/robusteren DE-Provider als Ergänzung zur Live-BNetzA-Abfrage.
      - **Datenschutz/Genauigkeit:** nur bei exakter Übereinstimmung von Name **und** normalisierter
        Anschrift gruppieren; als „mögliche weitere Rufzeichen" kennzeichnen; keine länderübergreifende
        Zusammenführung; nur ohnehin amtlich veröffentlichte Daten verwenden.
