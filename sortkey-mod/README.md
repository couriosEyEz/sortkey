# Sort Key – Minecraft 26.3 (Fabric)

Eine einzige Taste (Standard: **R**, im Menü *Steuerung → Tastenbelegung → Sortier-Taste* änderbar).

- Truhe / Fass / Enderchest / Shulker / Trichter / Werfer offen → sortiert den Container **und** dein Inventar
- Nur Inventar offen → sortiert dein Inventar (ohne Hotbar und Rüstung)
- Rein clientseitig: läuft auf jedem Server, auch ohne Server-Mod
- Fügt zuerst angebrochene Stapel zusammen, ordnet danach alphabetisch nach Namen

## Jar bauen (ohne Programmierkenntnisse): GitHub

1. Kostenloses Konto auf github.com, neues Repository anlegen.
2. Den kompletten Inhalt dieses Ordners hochladen (inkl. des versteckten Ordners `.github`).
3. Reiter **Actions** → Workflow **build** → grün abwarten (ca. 2–4 Minuten).
4. Im fertigen Lauf unter **Artifacts** `sortkey-mod` herunterladen → die `.jar` ist die Mod.

## Jar bauen lokal

Voraussetzung: JDK 25 und Gradle 9.6.0.

    gradle build

Die Mod liegt danach in `build/libs/sortkey-1.0.0.jar` (nicht die `-sources.jar`).

## Installieren

1. Fabric Loader 0.19.5 oder neuer für Minecraft 26.3 installieren (fabricmc.net/use).
2. Fabric API 0.163.0+26.3 (oder neuer) in den `mods`-Ordner legen.
3. `sortkey-1.0.0.jar` ebenfalls in den `mods`-Ordner legen.
