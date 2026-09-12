# Backlog – Ideen für kommende Versionen

## Erledigt

- [x] **Koax-Rechner: Kabel „Hyperflex 5"** – umgesetzt in v1.14.0 mit den echten
      Dämpfungswerten aus dem M&P-Datenblatt (28/144/432/1296 MHz = 4,1 / 9,6 / 17,0 / 30,5 dB/100 m).
- [x] **Weitere Rufzeichen derselben Person** – umgesetzt in v1.14.0. Umkehrsuche über
      Name + Anschrift; DE aus der BNetzA-Rufzeichenliste (PDF → `de_afu.sqlite` via pdftotext,
      wöchentlicher Timer), HU/NO/RO/PL aus dem lokalen Halterdatensatz. Feld `related` in der
      API-Antwort, in der Detailansicht verlinkt. Beispiel DK6IEC ↔ DN6IEC bestätigt.
- [x] **QSO aus der Detailansicht anlegen** – umgesetzt in v1.14.1 (vorbelegtes Rufzeichen/Name/Locator).

## Offen – Werkzeuge / Funktionen

- [ ] Funkwetter / Ausbreitung (SFI, K/A-Index, Sonnenflecken, Grayline) über Backend-Proxy zu NOAA/HamQSL.
- [ ] Locator- & Antennen-Tools: Locator↔Koordinaten, Entfernung/Peilung zwischen zwei beliebigen Punkten,
      Kompass-Ausrichtung zum Ziel-Locator per Gerätesensor.
- [ ] Relaissuche „in der Nähe" (DE-Repeaterliste, Import wie die Länderregister, nach Entfernung/Band).
- [ ] Satelliten-Überflüge (TLE/SGP4) – aufwändigster Brocken.
- [ ] Optional: schnellerer/robusterer DE-Provider direkt aus `de_afu.sqlite` als Ergänzung zur
      Live-BNetzA-Abfrage (Datensatz liegt bereits auf dem Server).

## Offen – Technik / Release

- [ ] Native Debug-Symbole im Release-Build (`ndk { debugSymbolLevel = "FULL" }`) – behebt eine
      der beiden Play-Warnungen; risikolos.
- [ ] Optional: R8 (Verkleinern + Mapping-Datei) – nur mit Keep-Regeln und eigener Test-Beta.

## Offen – Unterstützung / Spenden

- Ko-fi: https://ko-fi.com/saarmesh — externe Unterstützen-Seite unter docs/unterstuetzen.html
  (GitHub Pages). **Bewusst NICHT in der App / nicht auf der app-verlinkten Datenschutzseite**
  (Google-Play-Anti-Steering; vgl. AnkiDroid-Fall). Nur extern teilen.
- [ ] Optional: freiwillige „Unterstützer-Version" über Google Play Billing (Trinkgeld-Kauf +
      kosmetisches Badge im Über-Bereich). Richtlinienkonform, aber Play-Billing-Integration nötig.

## Geprüft & verworfen

- Australien: seit 2024 Klassenlizenz → einzelne Halter nicht mehr im Register; nur Repeater/Baken
  und ~260 zugeteilte Calls. Nicht als offizielle Einzelabfrage-Quelle geeignet.
- Neuseeland, Dänemark, Kroatien, Slowenien, Irland, Belgien, Estland, Lettland: keine
  skriptbare Massenquelle (nur Einzelsuche oder aus Datenschutzgründen keine Halterdaten).
- Globaler Community-Layer (HamQTH/QRZ): pausiert – benötigt ein kostenloses HamQTH-Konto,
  das ein eigenes Rufzeichen/Lizenz voraussetzt.
