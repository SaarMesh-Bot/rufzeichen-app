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

      Machbarkeit / Umsetzung:
      - Umkehrsuche über den **Halternamen** (name → Liste der Calls, aktuellen Call ausschließen).
        Direkt machbar für Länder mit lokalem Volldatensatz inkl. Name: **HU, NO, RO, PL** (Klub) –
        je Provider eine Methode `byHolder(name)` bzw. Backend-Endpoint `/callsign/by-holder`.
      - **Deutschland (BNetzA):** wird aktuell nur pro Call live abgefragt, kein Bulk-Halterdatensatz
        auf dem Server. Für DE zuerst prüfen, ob die BNetzA eine herunterladbare Gesamtliste anbietet;
        sonst ist eine Namens-Umkehrsuche für DE nicht ohne Weiteres möglich.
      - **Datenschutz/Genauigkeit:** Namensgleichheit ist nicht eindeutig (verschiedene Personen,
        gleicher Name). Nur bei exakter Namensübereinstimmung gruppieren und als „mögliche" weitere
        Rufzeichen kennzeichnen; keine Zusammenführung über Länder hinweg.
