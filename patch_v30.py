#!/usr/bin/env python3
import io, sys

def patch(path, old, new, count=1):
    with io.open(path, encoding="utf-8") as f:
        t = f.read()
    n = t.count(old)
    if n != count:
        print(f"ABORT {path}: expected {count} match, found {n}")
        sys.exit(1)
    t = t.replace(old, new, count)
    with io.open(path, "w", encoding="utf-8") as f:
        f.write(t)
    print(f"OK {path}")

loc = "app/src/main/java/de/hamlookup/rufzeichen/ui/Loc.kt"
settings = "app/src/main/java/de/hamlookup/rufzeichen/ui/favorites/SettingsScreen.kt"
gradle = "app/build.gradle.kts"

# 1) Loc.kt — title, subtitle, new optional hint, field labels
loc_old = (
'    val toggleHamqthTitle get() = s("HamQTH (Gerät, international)", "HamQTH (device, international)")\n'
'    val toggleHamqthSub get() = s(\n'
'        "Weltweite Community-Datenbank direkt vom Gerät. Kostenloses Konto erforderlich.",\n'
'        "Worldwide community database directly from the device. A free account is required."\n'
'    )\n'
'    val hamqthUser get() = s("HamQTH-Benutzername", "HamQTH username")\n'
'    val hamqthPass get() = s("HamQTH-Passwort", "HamQTH password")\n'
)
loc_new = (
'    val toggleHamqthTitle get() = s("HamQTH (Gerät, international) – optional", "HamQTH (device, international) – optional")\n'
'    val toggleHamqthSub get() = s(\n'
'        "Optionale weltweite Community-Datenbank. Nur wenn du dies aktivierst, ist ein " +\n'
'            "kostenloses HamQTH-Konto nötig. Die App funktioniert vollständig ohne Anmeldung.",\n'
'        "Optional worldwide community database. A free HamQTH account is only needed if you " +\n'
'            "enable this. The app works fully without any login."\n'
'    )\n'
'    val hamqthOptionalHint get() = s(\n'
'        "Optional – nur für die internationale Community-Suche. Zum Nutzen der App ist keine " +\n'
'            "Anmeldung nötig; alle Kernfunktionen laufen ohne Konto.",\n'
'        "Optional – only for the international community search. No login is required to use the " +\n'
'            "app; all core features work without an account."\n'
'    )\n'
'    val hamqthUser get() = s("HamQTH-Benutzername (optional)", "HamQTH username (optional)")\n'
'    val hamqthPass get() = s("HamQTH-Passwort (optional)", "HamQTH password (optional)")\n'
)
patch(loc, loc_old, loc_new)

# 2) SettingsScreen.kt — insert optional hint at top of the useHamQth block
set_old = (
'        if (settings.useHamQth) {\n'
'            Spacer(Modifier.height(8.dp))\n'
'            PersistedTextField(\n'
'                value = settings.hamQthUser,\n'
)
set_new = (
'        if (settings.useHamQth) {\n'
'            Spacer(Modifier.height(8.dp))\n'
'            Text(\n'
'                Loc.hamqthOptionalHint,\n'
'                style = MaterialTheme.typography.labelSmall,\n'
'                color = MaterialTheme.colorScheme.onSurfaceVariant\n'
'            )\n'
'            Spacer(Modifier.height(8.dp))\n'
'            PersistedTextField(\n'
'                value = settings.hamQthUser,\n'
)
patch(settings, set_old, set_new)

# 3) version bump
patch(gradle, "        versionCode = 29\n", "        versionCode = 30\n")
patch(gradle, '        versionName = "1.14.1"\n', '        versionName = "1.14.2"\n')

print("ALL PATCHES APPLIED")
