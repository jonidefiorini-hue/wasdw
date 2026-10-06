# Auction HUD (TikTokSW auction)

Fabric-Client-Mod für Minecraft 1.21.8.

## Bauen
1. Auf https://fabricmc.net/develop/template ein Projekt für 1.21.8 erzeugen (liefert gradlew + Wrapper),
   oder lokal Gradle (>= 8.12) + JDK 21 installieren.
2. Den Ordner `src/` sowie `build.gradle` / `gradle.properties` aus diesem Projekt dorthin kopieren.
3. `./gradlew build` -> fertige Mod unter `build/libs/auction-hud-1.1.0.jar`.

## Bedienung
- G: Auktions-Menü (Item wählen, Mindestgebot, Dauer, Start/Abbruch, HUD an/aus, Farbe)
- H: HUD ein-/ausblenden
- Config: `config/auctionhud.json` (hudOffsetX/hudOffsetY zum Verschieben, hudColor, Zahlungs-Patterns)
