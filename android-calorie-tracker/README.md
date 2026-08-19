# KalTrack

Eine bewusst einfache Android-App zum Tracken von **Kalorien und Eiweiß** – mit
**Barcode-Scanner**, der die Nährwerte aus der offenen Produktdatenbank
[Open Food Facts](https://world.openfoodfacts.org) holt.

## Was die App kann

- **Tagesübersicht** mit Balken für Kalorien und Eiweiß gegen dein Tagesziel;
  mit den Pfeilen blätterst du durch die Tage, ein Tipp auf das Datum springt
  zurück auf heute.
- **Barcode scannen** (EAN-8/13, UPC): Kamera drauf halten, Produkt wird
  automatisch erkannt und nachgeschlagen.
- **Menge eintragen** in Gramm, mit Schnellwahl für Portionsgröße, 50/100/200 g.
  Kalorien und Eiweiß werden live vorgerechnet, bevor du speicherst.
- **Textsuche** in Open Food Facts, falls kein Barcode dran ist.
- **Manuell eintragen** für alles ohne Verpackung (selbst gekocht, Restaurant).
- **Zuletzt benutzt**: gescannte Produkte landen in einem lokalen Cache und sind
  beim nächsten Mal sofort da – auch offline.
- **Ziele** für kcal und Eiweiß pro Tag einstellbar.
- Löschen mit **Rückgängig**-Snackbar.

Alle Daten bleiben auf dem Gerät. Nach außen geht nur die Abfrage an Open Food
Facts (Barcode bzw. Suchbegriff).

## Bauen

Es braucht das Android SDK (API 35). Am einfachsten über Android Studio:
Ordner `android-calorie-tracker` öffnen, Gradle-Sync abwarten, auf Gerät oder
Emulator starten.

Auf der Kommandozeile, wenn das SDK schon da ist:

```bash
cd android-calorie-tracker
./gradlew assembleDebug          # APK unter app/build/outputs/apk/debug/
./gradlew installDebug           # direkt aufs angeschlossene Gerät
./gradlew test                   # Unit-Tests
```

Falls Gradle das SDK nicht findet, eine `local.properties` anlegen:

```properties
sdk.dir=/pfad/zum/Android/sdk
```

Mindestens Android 8.0 (API 26), Ziel ist API 35.

## Aufbau

```
app/src/main/java/com/kaltrack/app/
├── AppContainer.kt            Abhängigkeiten von Hand verdrahtet (keine DI-Bibliothek)
├── data/
│   ├── FoodRepository.kt      einzige Anlaufstelle der UI für Daten
│   ├── local/                 Room: Tagebucheinträge + Produkt-Cache
│   ├── remote/                Open-Food-Facts-Client (OkHttp + org.json)
│   ├── prefs/                 Tagesziele in SharedPreferences
│   └── model/                 Product, Goals
└── ui/
    ├── today/                 Tagesübersicht
    ├── scan/                  CameraX + ML Kit Barcode-Scanner
    ├── amount/                Menge eintragen nach dem Scan
    ├── add/                   Suche, „zuletzt benutzt“, Einstieg zum Scannen
    ├── manual/                freie Eingabe
    └── settings/              Tagesziele
```

Jetpack Compose (Material 3), Room, CameraX, ML Kit Barcode Scanning, OkHttp.
ViewModels bekommen ihre Abhängigkeiten über kleine `viewModelFactory`-Blöcke,
UI-Zustand läuft als `StateFlow` in die Compose-Screens.

Nährwerte werden immer **je 100 g** gespeichert, nie schon ausgerechnet – so
lässt sich die Menge später ändern, ohne die Werte neu zu suchen.

## Nährwertdaten

Quelle ist Open Food Facts, lizenziert unter der
[Open Database License](https://opendatacommons.org/licenses/odbl/1-0/).
Die Daten sind von der Community gepflegt: meistens gut, gelegentlich lückenhaft
oder falsch. Produkte ohne brauchbare Energieangabe werden gar nicht erst
angezeigt, statt eine 0 vorzutäuschen. Im Zweifel gilt die Verpackung.

Die API liefert die Felder erstaunlich uneinheitlich – mal Zahl, mal String, mal
nur Kilojoule statt Kilokalorien. `OpenFoodFactsClientTest` fixiert das Verhalten
für diese Fälle anhand echter Antwortformen.

## Noch nicht drin

Naheliegende nächste Schritte, bewusst weggelassen, damit die erste Version
klein bleibt:

- Mahlzeiten (Frühstück/Mittag/Abend) statt einer flachen Tagesliste
- Einträge nachträglich bearbeiten (bisher: löschen und neu anlegen)
- eigene Rezepte aus mehreren Zutaten
- Kohlenhydrate und Fett – die Werte kommen von der API schon mit
- Wochen-/Verlaufsansicht
- Beitrag zurück an Open Food Facts, wenn ein Barcode unbekannt ist
