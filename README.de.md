# Angelbot für LabyMod 4

Ein Addon für den Laby Client, das vollautomatisch angelt. Es erkennt den Biss, holt die Angel nach einer
kurzen, zufälligen Reaktionszeit ein (derselbe Rechtsklick wie von Hand), wartet eine zufällige Pause und
wirft sie wieder aus.

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2 und 26.3
- **Client:** Laby Client (LabyMod 4) als Addon. Kein Forge, kein Fabric.
- **Sprachen:** Deutsch und Englisch (richtet sich nach der Minecraft-Sprache). [English version](README.md)

> **Hinweis zu Servern:** Auf vielen Servern ist automatisches Angeln verboten und kann zum Bann führen.
> Auf dem eigenen Server und in Einzelspieler-Welten ist es unproblematisch. Die Reaktionszeiten sind
> zufällig, ein Schutz vor Erkennung ist das aber nicht.

## Installation

Über den Addon-Store im Laby Client (sobald das Addon dort gelistet ist) oder von Hand: die Datei
`angelbot-release.jar` in den Addon-Ordner legen und den Client neu starten.

| System | Addon-Ordner |
|---|---|
| Windows | `%APPDATA%\.minecraft\labymod-neo\addons` |
| Linux | `~/.minecraft/labymod-neo/addons` |
| macOS | `~/Library/Application Support/minecraft/labymod-neo/addons` |

## Benutzung

1. Eine Angel in die Haupt- oder Nebenhand nehmen und sich ans Wasser stellen.
2. Den Bot starten: mit dem Chat-Befehl `/angelbot` (schaltet um), `/angelbot on` oder `/angelbot off`,
   oder mit einer eigenen Taste (Einstellungen → Addons → Angelbot → *Taste zum Ein- und Ausschalten*).
   Bei offenem Chat oder Inventar passiert beim Tippen nichts.
3. `/angelbot status` zeigt Zustand, Bisse und Laufzeit.

Der Bot schaltet sich selbst ab (mit Benachrichtigung), wenn

- du die Welt oder den Server verlässt,
- 10 Sekunden lang keine Angel in der Hand liegt (zum Beispiel weil sie kaputt ist),
- der Schwimmer fünfmal hintereinander nicht im Wasser landet,
- der Server fünfmal hintereinander nicht auf die Angel reagiert,
- ein unerwarteter Fehler auftritt (Einzelheiten in `latest.log`).

**Im Hintergrund fischen:** Wechselst du das Fenster, öffnet Minecraft das Pausenmenü. Solange ein
Bildschirm offen ist, ruht der Bot (nach 10 Sekunden kommt ein Hinweis). Mit **F3 + P** schaltet man die
Pause beim Fensterwechsel aus, oder man schaltet *Pause bei offenem Bildschirm* in den Addon-Einstellungen aus.

## Einstellungen

Einstellungen → Addons → Angelbot.

| Einstellung | Standard | Bedeutung |
|---|---|---|
| Aktiviert | an | Schaltet das ganze Addon an oder aus |
| Taste zum Ein- und Ausschalten | keine | Startet und stoppt den Bot |
| Reaktionszeit, kürzeste / längste | 200 / 450 ms | Wartezeit nach dem Biss, bevor eingeholt wird (zufällig dazwischen) |
| Pause vor dem Auswerfen, kürzeste / längste | 700 / 1400 ms | Pause nach dem Einholen, bevor neu ausgeworfen wird |
| Zeitlimit ohne Biss | 60 s | Danach wird die Angel eingeholt und neu ausgeworfen |
| Pause bei offenem Bildschirm | an | Der Bot ruht bei offenem Chat, Inventar oder Menü |
| Ohne Angel abschalten | an | Schaltet den Bot nach 10 s ohne Angel in der Hand ab |
| Bissschwelle | 15 | Sinkgeschwindigkeit des Schwimmers (Hundertstel Blöcke pro Tick), ab der es als Biss zählt |
| Beruhigungszeit | 20 Ticks | So lange nach dem ersten Wasserkontakt wartet der Bot, bevor er auf Bisse achtet |
| Debug-Ausgabe | aus | Meldet im Chat und in `latest.log`, wenn der Schwimmer fast die Schwelle erreicht |

Der Fisch bleibt nach dem Biss nur **1 bis 2 Sekunden** am Haken. Die längste Reaktionszeit sollte darum
unter etwa 600 ms bleiben, bei hohem Ping noch darunter.

## So erkennt der Bot den Biss

Beißt ein Fisch, zieht Minecraft den Schwimmer mit einem Ruck nach unten (etwa 0,24 bis 0,40 Blöcke
pro Tick). Ohne Biss bewegt sich der Schwimmer nur leicht. Der Bot beobachtet die senkrechte Geschwindigkeit
des Schwimmers. Ist seit dem ersten Wasserkontakt mindestens die Beruhigungszeit vergangen und sinkt der
Schwimmer schneller als die Bissschwelle, zählt das als Biss.

Ob der Schwimmer im Wasser ist, prüft der Bot so wie Minecraft selbst: Wasser im Block des Schwimmers.
Ein Schwimmer, der nie Wasser berührt, wird nach 8 Sekunden eingeholt und neu ausgeworfen, einer, der das
Wasser wieder verlässt, nach 3 Sekunden. Bei jedem Einholen schreibt das Addon eine Zeile in `latest.log`
(zum Beispiel `Reeled in: bite after 312 ticks, sink speed -0.31`). So lässt sich nachvollziehen, warum
eingeholt wurde.

Feinabstimmung, falls nötig:

- **Holt ein, ohne dass etwas gebissen hat:** Bissschwelle erhöhen (zum Beispiel 20) oder Beruhigungszeit erhöhen.
- **Verpasst Bisse:** *Debug-Ausgabe* einschalten und zusehen. Meldet der Chat bei einem Biss zum Beispiel
  „Schwimmer sinkt mit -0.12“, die Schwelle aber 0.15 ist, die Bissschwelle auf 10 senken.

## Selbst bauen

Voraussetzungen: **JDK 25** (zum Beispiel Eclipse Temurin 25). Gradle lädt der mitgelieferte Wrapper
selbst (Version 9.7.1). Beim ersten Bau werden LabyGradle, die LabyMod-API und die
Minecraft-Dateien geladen (rund 1 bis 3 GB), das dauert einige Minuten.

PowerShell, im Projektordner:

```powershell
$env:JAVA_HOME = "C:\Pfad\zu\jdk-25"      # Ordner des JDK 25
.\gradlew.bat build
.\gradlew.bat createReleaseJar
```

Das fertige Addon liegt dann unter `build\libs\angelbot-release.jar`. Nur diese Datei mit dem
Zusatz `-release` funktioniert im normalen Client, die andere Jar in `build\libs` ist ein Zwischenergebnis.

Zum Ausprobieren ohne Installation startet `.\gradlew.bat :game-runner:client_v26.2` einen Entwicklungs-Client
mit dem Addon (ohne Konto, eigene Testwelt unter `run\client`). Für andere Versionen heißt die Aufgabe
`client_v26.1`, `client_v26.1.1`, `client_v26.1.2` oder `client_v26.3`. `.\gradlew.bat checkAddon` prüft die
Quellen gegen die Store-Richtlinien und auf fehlende Übersetzungen.

### Aufbau

```
build.gradle.kts, settings.gradle.kts, gradle.properties   Gradle-Setup (LabyGradle 0.11.0)
core/                                  versionsunabhängiger Teil
  src/main/java/de/angelbot/core/
    AngelbotAddon.java                 Hauptklasse (Start, Listener, Befehl)
    AngelbotConfiguration.java         Einstellungen
    FishingController.java             Schnittstelle zum Spiel
    bot/FishingBot.java                Zustandsautomat: auswerfen, Biss, einholen
    commands/, listener/               /angelbot, Tick und Taste
  src/main/resources/assets/angelbot/i18n/   Texte (en_us, de_de)
game-runner/src/shared/java/de/angelbot/shared/
  VersionedFishingController.java      Zugriff auf Angel, Schwimmer, Rechtsklick (Minecraft 26.x)
tests/FishingBotTest.java              Test der Bot-Logik mit simuliertem Spiel
```

Die Bot-Logik (`core/.../bot`) kennt weder Minecraft noch LabyMod. Der Test läuft ohne Gradle:

```powershell
& "$env:JAVA_HOME\bin\javac" -d tests\out core\src\main\java\de\angelbot\core\bot\*.java tests\FishingBotTest.java
& "$env:JAVA_HOME\bin\java" -cp tests\out FishingBotTest
```

### Eine weitere Minecraft-Version unterstützen

1. Die Version in `gradle.properties` bei `net.labymod.minecraft-versions` eintragen.
2. In `build.gradle.kts` den Versionsbereich `minecraftVersion = "26.1<26.3"` anpassen.
3. `gradlew build` ausführen. Fehlt in der neuen Version etwas, das `VersionedFishingController` nutzt,
   nennt LabyGradle die betroffene Version. Dann dort mit `MinecraftTarget` unterscheiden
   (siehe das [Wiki zu versionsabhängigem Code](https://github.com/LabyMod/labymod4-api-wiki/blob/master/docs/pages/addon/features/shared-version-code.md)).

## Fehlersuche

| Problem | Mögliche Ursache |
|---|---|
| Addon fehlt in der Liste | Falsche Jar (nur `-release.jar`), falscher Ordner, Minecraft-Version außerhalb 26.1 bis 26.3, Client nicht neu gestartet |
| `/angelbot` tut nichts | Addon in den Einstellungen ausgeschaltet; Meldung oben rechts beachten |
| Bot startet nicht | Keine Angel in der Haupt- oder Nebenhand, oder keine Welt geladen |
| Bot ruht | Ein Bildschirm ist offen (auch das Pausenmenü nach einem Fensterwechsel) |
| Bot wirft immer wieder neu aus | Schwimmer landet nicht im Wasser; anders zielen |
| Meldung „Server reagiert nicht“ | Auf dem Server ist Angeln eingeschränkt oder die Verbindung hängt |

## Lizenz

[MIT](LICENSE)
