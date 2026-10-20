# Backlog – Ideen für kommende Versionen

## Status

- **2.0 = v1.14.2 (30)** – als erster Produktions-Release eingereicht (18./19.09.2026), in Prüfung.
  Erste Einreichung (v29) wurde abgelehnt, weil der Prüfer die optionalen HamQTH-Felder als
  Login-Wand gewertet hat → in v30 klar als optional gekennzeichnet + „Anmeldedaten: Nein" gespeichert.

## Geplant für 2.1

- [x] **R8 aktiviert** in v1.15.1 (32), zusammen mit activity-compose 1.10.1 wegen Edge-to-Edge-Hinweis – (`isMinifyEnabled = true`, `isShrinkResources = true`) inkl. Keep-Regeln für
      Room, osmdroid und JSON-Parsing. Play meldet aktuell: DEX-Optimierung „Niedrig",
      unkomprimierte DEX-Größe 21,8 MB. Mapping-Datei wird dann automatisch mit hochgeladen.
      **Nur mit eigener geschlossener Test-Beta vor Produktion.**
- [ ] **Native Debug-Symbole** im Release-Build (`ndk { debugSymbolLevel = "FULL" }`) – behebt die
      zweite Play-Warnung; risikolos.
- [ ] **Relaissuche „in der Nähe"** – Hybrid:
      - Standardquelle für alle: **repeatermap.de (DK3ML)** – Anfrage auf Datenzugang/API am
        20.10.2026 über das Kontaktformular verschickt – **Antwort abwarten**.
        repeatermap.de hat eine JSON-API (repeatermap.de/api.php): Token nur auf Anfrage (Kontaktformular),
        Umkreissuche weltweit, Rate-Limits ohne Zahlen. Endpunkte/Felder/Lizenz nicht öffentlich dokumentiert –
        mit dem Token erfragen. Token gehört auf den eigenen Server, nicht in die App. Import wie die Länderregister
        (`repeaters.sqlite`, wöchentlicher Timer), Endpunkt „Relais in der Nähe" mit
        Entfernung/Peilung, Filter nach Band/Modus, Quellenangabe „Daten: repeatermap.de (DK3ML)".
      - Optional: **RepeaterBook Connect** – On-Device-ContentProvider
        (`content://com.zbm2.repeaterbook.RBContentProvider/repeaters`), nur für Nutzer mit
        installierter RepeaterBook-App + Connect-Abo. Umschalter wie bei HamQTH; Daten verlassen
        das Gerät nicht. Android-11+-Paketsichtbarkeit (`<queries>`) im Manifest nötig.
      - Alternative/Zweitanfrage: DL3EL (FM-Relaisliste DL).
- [x] **Klassenbezeichnungen vereinheitlicht** (Nutzerhinweis DO6KAP): zentrale Definition
      `data/model/GermanLicenceClass.kt` nach AFuV-Novelle 2024 – N = Einstiegsklasse,
      E = mittlere Stufe, A = volle Klasse. Suche, Detail, Offline-Analyse und Bänder-Übersicht
      nutzen sie; alte Bezeichnungen aus Cache/Favoriten werden beim Lesen umgesetzt.
- [x] **Logbuch: Frequenzfeld** (Nutzerhinweis DO6KAP): optionales Feld „Frequenz (MHz)“,
      Band wird daraus abgeleitet (`data/tools/AmateurBands.kt`), Band als Dropdown mit
      ADIF-Bandnamen, ADIF-Export schreibt BAND und FREQ. DB-Version 5 (Spalte `qso.freqMhz`).
      **Vor Release testen:** Update einer bestehenden Installation mit Logbuch-Einträgen
      (Migration 4→5), damit keine QSOs verloren gehen.
- [ ] **Englischer Store-Eintrag (en-US)** nachtragen – erst nach der 2.0-Freigabe, damit die
      laufende Prüfung nicht neu startet. Texte liegen im Store-Kit.

## Später (evtl. AGP-Upgrade)

- [ ] Android Gradle Plugin 9.0 (von Play empfohlen, bringt weitere R8-Optimierungen) – eigener Schritt.

## Erledigt

- [x] **Koax-Rechner: Kabel „Hyperflex 5"** – umgesetzt in v1.14.0 mit den echten
      Dämpfungswerten aus dem M&P-Datenblatt (28/144/432/1296 MHz = 4,1 / 9,6 / 17,0 / 30,5 dB/100 m).
- [x] **Weitere Rufzeichen derselben Person** – umgesetzt in v1.14.0. Umkehrsuche über
      Name + Anschrift; DE aus der BNetzA-Rufzeichenliste (PDF → `de_afu.sqlite` via pdftotext,
      wöchentlicher Timer), HU/NO/RO/PL aus dem lokalen Halterdatensatz. Feld `related` in der
      API-Antwort, in der Detailansicht verlinkt. Beispiel DK6IEC ↔ DN6IEC bestätigt.
- [x] **QSO aus der Detailansicht anlegen** – umgesetzt in v1.14.1 (vorbelegtes Rufzeichen/Name/Locator).
- [x] **HamQTH-Felder als optional gekennzeichnet** – v1.14.2 (Play-Review-Fix).

## Offen – Werkzeuge / Funktionen

- [ ] Funkwetter / Ausbreitung (SFI, K/A-Index, Sonnenflecken, Grayline) über Backend-Proxy zu NOAA/HamQSL.
- [ ] Locator- & Antennen-Tools: Locator↔Koordinaten, Entfernung/Peilung zwischen zwei beliebigen Punkten,
      Kompass-Ausrichtung zum Ziel-Locator per Gerätesensor.
- [ ] Satelliten-Überflüge (TLE/SGP4) – aufwändigster Brocken.
- [ ] Optional: schnellerer/robusterer DE-Provider direkt aus `de_afu.sqlite` als Ergänzung zur
      Live-BNetzA-Abfrage (Datensatz liegt bereits auf dem Server).

## Offen – Unterstützung / Spenden

- Ko-fi: https://ko-fi.com/saarmesh — externe Unterstützen-Seite unter docs/unterstuetzen.html
  (GitHub Pages). **Bewusst NICHT in der App / nicht auf der app-verlinkten Datenschutzseite**
  (Google-Play-Anti-Steering; vgl. AnkiDroid-Fall). Nur extern teilen.
- [ ] Optional: freiwillige „Unterstützer-Version" über Google Play Billing (Trinkgeld-Kauf +
      kosmetisches Badge im Über-Bereich). Richtlinienkonform, aber Play-Billing-Integration nötig.

## Geprüft & verworfen

- **Gesamtkarte aller Rufzeicheninhaber (DE)**: nicht umsetzen. BNetzA-Liste ist Opt-out (Anschrift
  nur ohne Widerspruch), aber eine durchblätterbare Wohnort-Karte wäre eine neue Verarbeitung
  (Zweckbindung/DSGVO) und riskant bei Google Play. Alternativen: Dichte-Heatmap pro
  Locator-Feld oder Karte nur mit Klubstationen.
- **OpenStreetMap als Repeater-Quelle**: Overpass-Abfrage für ganz DE (09/2026) liefert nur 4
  Repeater-Objekte, keines mit Frequenz → unbrauchbar.
- **RepeaterBook Export-API (HTTP)**: Genehmigung nötig; Bedingungen verbieten Caching,
  Spiegelung und Weiterverteilung, erlauben nur private Ansicht → passt nicht zu unserem
  Server-Import/Proxy.
- Australien: seit 2024 Klassenlizenz → einzelne Halter nicht mehr im Register; nur Repeater/Baken
  und ~260 zugeteilte Calls. Nicht als offizielle Einzelabfrage-Quelle geeignet.
- Neuseeland, Dänemark, Kroatien, Slowenien, Irland, Belgien, Estland, Lettland: keine
  skriptbare Massenquelle (nur Einzelsuche oder aus Datenschutzgründen keine Halterdaten).
- Globaler Community-Layer (HamQTH/QRZ): pausiert – benötigt ein kostenloses HamQTH-Konto,
  das ein eigenes Rufzeichen/Lizenz voraussetzt.
