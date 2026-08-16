# Rufzeichen – Amateurfunk-Rufzeichensuche (Android)

Native Android-App (Kotlin + Jetpack Compose, Material 3) zum Suchen und
Analysieren von Amateurfunk-Rufzeichen. Kombiniert die BNetzA-Onlineabfrage,
eine optionale internationale Quelle (HamQTH) und eine vollständig offline
nutzbare Rufzeichen-Analyse.

## Funktionen
- Suche mit Detailansicht (Inhaber, Klasse, Standort), Wildcard `*`
- Favoriten und Suchverlauf (Room-Datenbank)
- Offline-Analyse (Präfix, Land nach ITU, geschätzte deutsche Klasse) + Cache
- Kombinierte Datenquellen mit automatischem Fallback

## APK bauen
Die APK wird per GitHub Actions automatisch gebaut: Reiter **Actions** →
Workflow **Build APK** → Artefakt `rufzeichen-debug-apk`. Alternativ lokal in
Android Studio öffnen und *Build → Build APK* oder `./gradlew assembleDebug`.

## Zur BNetzA-Datenquelle
Die Bundesnetzagentur bietet keine offizielle API. Die App fragt die öffentliche
Suchmaske (`ans.bundesnetzagentur.de/Amateurfunk/Rufzeichen.aspx`) inoffiziell
ab und fällt bei Website-Änderungen auf Cache und Offline-Analyse zurück.
Bitte die Nutzungs- und Datenschutzhinweise der BNetzA beachten.

Min SDK 24 · Target/Compile SDK 35 · JDK 17.
