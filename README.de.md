# Angelbot für LabyMod 4

Ein Addon für den Laby Client, das vollautomatisch angelt. Es erkennt den Biss, holt die Angel nach einer
kurzen, zufälligen Reaktionszeit ein (derselbe Rechtsklick wie von Hand), wartet eine zufällige Pause und
wirft sie wieder aus. Dazu kann es auf eine Ersatzangel wechseln, bei Hunger essen, in einem erlaubten
Bereich bleiben und, wenn du es willst, zur nächsten Angelstelle laufen, sobald der Server meldet, dass die
Stelle leer ist.

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2 und 26.3
- **Client:** Laby Client (LabyMod 4) als Addon. Kein Forge, kein Fabric.
- **Sprachen:** Deutsch und Englisch (richtet sich nach der Minecraft-Sprache). [English version](README.md)

> **Hinweis zu Servern:** Auf vielen Servern ist automatisches Angeln verboten, automatisches Laufen erst
> recht, und beides kann zum Bann führen. Auf dem eigenen Server und in Einzelspieler-Welten ist es
> unproblematisch. Die Reaktionszeiten sind zufällig, ein Schutz vor Erkennung ist das aber nicht.

## Installation

Über den Addon-Store im Laby Client (sobald das Addon dort gelistet ist) oder von Hand: die Datei
`angelbot-release.jar` in den Ordner `addons` deiner Instanz legen und den Client neu starten.

| LabyMod Launcher | Addon-Ordner |
|---|---|
| Version 3 (Windows) | `%APPDATA%\LabyMod\instances\<Instanz-ID>\addons`, ein Ordner je Instanz |
| Ältere Versionen | Windows `%APPDATA%\.minecraft\labymod-neo\addons`, Linux `~/.minecraft/labymod-neo/addons`, macOS `~/Library/Application Support/minecraft/labymod-neo/addons` |

Erscheint das Addon nach einem Neustart nicht in der Liste, liegt die Jar meist im falschen Ordner.

## Benutzung

1. Eine Angel in die Haupt- oder Nebenhand nehmen und sich ans Wasser stellen.
2. Den Bot starten: mit dem Chat-Befehl `/angelbot` (schaltet um), `/angelbot on` oder `/angelbot off`,
   oder mit einer eigenen Taste (Einstellungen → Addons → Angelbot → *Taste zum Ein- und Ausschalten*).
   Bei offenem Chat oder Inventar passiert beim Tippen nichts.
3. `/angelbot status` zeigt Zustand, Bisse und Laufzeit.

| Befehl | Was er tut |
|---|---|
| `/angelbot` | Startet oder stoppt den Bot |
| `/angelbot on`, `/angelbot off` | Startet oder stoppt den Bot |
| `/angelbot status` | Zustand, Bisse und Laufzeit |
| `/angelbot spot add` | Speichert den Ort, an dem du stehst, mit deiner Blickrichtung als Angelstelle |
| `/angelbot spot list`, `spot remove <Nummer>`, `spot clear` | Zeigt, entfernt oder leert die gespeicherten Stellen |
| `/angelbot zone set` | Setzt den Mittelpunkt des erlaubten Bereichs auf den Ort, an dem du stehst |
| `/angelbot zone`, `zone clear` | Zeigt oder entfernt den Mittelpunkt |

Der Bot schaltet sich selbst ab (mit Benachrichtigung), wenn

- du die Welt oder den Server verlässt,
- 10 Sekunden lang keine Angel in der Hand liegt,
- die Angel verbraucht ist und es keine Ersatzangel gibt,
- du den erlaubten Bereich verlässt,
- der Schwimmer fünfmal hintereinander nicht im Wasser landet,
- der Server fünfmal hintereinander nicht auf die Angel reagiert,
- der Server meldet, dass die Stelle leer ist, und der Bot nicht zu einer anderen laufen kann,
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
| Auf Ersatzangel wechseln | an | Nimmt eine andere Angel, wenn die jetzige verbraucht ist oder kaputtgeht, sonst stoppt der Bot |
| Angel verbraucht bei (Benutzungen übrig) | 5 | Eine Angel mit so vielen Benutzungen Rest oder weniger wird nicht mehr benutzt. 0 = bis sie kaputtgeht |
| Automatisch essen | an | Isst zwischen zwei Würfen bei Hunger (nur, solange der Bot läuft) |
| Essen bei Hungerwert | 14 | Der Bot isst bei diesem Hungerwert (20 = satt) oder darunter |
| Erlaubtes Essen | übliches, sicheres Essen | Namen der Gegenstände, durch Komma getrennt, das Beste zuerst |
| Radius des erlaubten Bereichs | 0 (aus) | Der Bot stoppt, wenn du weiter als das vom Mittelpunkt des Bereichs entfernt bist |
| Chattext: Stelle ist leer | `keine fische,no fish` | Eine Servermeldung mit einem dieser Texte bedeutet: Die Stelle ist leer |
| Zur nächsten Stelle laufen | aus | Läuft bei leerer Stelle zur nächsten gespeicherten Stelle |
| Leere Stelle ruht | 10 Min. | So lange wird eine als leer gemeldete Stelle nicht wieder besucht |
| Bissschwelle | 15 | Sinkgeschwindigkeit des Schwimmers (Hundertstel Blöcke pro Tick), ab der es als Biss zählt |
| Beruhigungszeit | 20 Ticks | So lange nach dem ersten Wasserkontakt wartet der Bot, bevor er auf Bisse achtet |
| Debug-Ausgabe | aus | Meldet im Chat und in `latest.log`, wenn der Schwimmer fast die Schwelle erreicht |

Der Fisch bleibt nach dem Biss nur **1 bis 2 Sekunden** am Haken. Die längste Reaktionszeit sollte darum
unter etwa 600 ms bleiben, bei hohem Ping noch darunter.

## Angel: Haltbarkeit und Ersatzangel

Eine neue Angel hat 64 Benutzungen. Mit *Angel verbraucht bei* stellst du ein, wie viele Benutzungen
mindestens übrig bleiben sollen (Standard 5), damit eine gute Angel nicht bis zum Schluss benutzt wird. Der
Bot prüft vor jedem Auswerfen. Ist die Angel in deiner Hand verbraucht, nimmt er eine andere, die über der
Grenze liegt: zuerst aus der Hotbar, dann aus dem Inventar (in den gewählten Hotbar-Platz getauscht, die
verbrauchte Angel wandert ins Inventar). Gibt es keine Ersatzangel, stoppt der Bot und sagt es dir. Steht die
Grenze auf 0, wird die Angel bis zum Schluss benutzt, und der Bot wechselt (oder stoppt) genau dann, wenn sie
kaputtgeht. Schaltest du *Auf Ersatzangel wechseln* aus, stoppt der Bot, sobald die Angel verbraucht ist.

## Essen

Ist dein Hungerwert so hoch wie *Essen bei Hungerwert* oder niedriger, isst der Bot zwischen zwei Würfen,
nie bei ausgeworfenem Schwimmer. Er isst nur, was in *Erlaubtes Essen* steht (das Erste, das er findet, in
der Reihenfolge der Liste: Nebenhand, Hotbar, dann Inventar). Die Standardliste enthält übliches, sicheres
Essen, keine goldenen Äpfel. Danach legt der Bot die Angel wieder in die Hand. Findet er nichts aus der Liste,
bekommst du eine Benachrichtigung (höchstens einmal pro Minute) und der Bot angelt weiter. Ist der Bot aus,
passiert nichts.

## Erlaubter Bereich

*Radius des erlaubten Bereichs* begrenzt, wie weit du dich vom Mittelpunkt des Bereichs entfernen darfst.
Der Mittelpunkt ist die Stelle, an der du den Bot gestartet hast, oder ein fester Mittelpunkt, den du mit
`/angelbot zone set` setzt. Bist du weiter als der Radius (plus ein Block) entfernt, stoppt der Bot. Das
schützt auch, wenn dich etwas wegschubst oder teleportiert. 0 heißt: keine Grenze.

## Leere Angelstellen und Laufen

Manche Server melden, wenn eine Angelstelle leer gefischt ist. Kommt eine Meldung mit einem der Texte aus
*Chattext: Stelle ist leer* (im Chat oder in der Aktionsleiste, Meldungen von Spielern werden ignoriert),
reagiert der Bot:

- **Laufen aus (Standard):** Der Bot holt ein, stoppt und sagt es dir. Du gehst selbst weiter.
- **Laufen an:** Der Bot holt ein und läuft zur nächsten gespeicherten Stelle. Einmal einrichten:
  1. *Radius des erlaubten Bereichs* einstellen (zum Beispiel 15) und in die Mitte des Bereichs stellen:
     `/angelbot zone set`.
  2. An jede Stelle stellen, an der du angelst, aufs Wasser schauen und `/angelbot spot add` eingeben
     (mindestens zwei Stellen).
  3. *Zur nächsten Stelle laufen* einschalten und den Bot starten.

  Der Bot läuft zur nächsten Stelle der Liste, die nicht ruht, dreht sich in die gespeicherte Blickrichtung,
  wartet eine Sekunde und wirft aus. Eine als leer gemeldete Stelle ruht die eingestellte Zeit. Ruhen alle
  Stellen, stoppt der Bot.

**Wie der Bot läuft und was er meidet:** Den Weg findet er mit A\* über die geladenen Blöcke: gerade Schritte
in die vier Himmelsrichtungen, einen Block hinauf (mit Sprung) und bis zu zwei Blöcke hinunter. Er geht nie
durch Wasser, Lava, Feuer, Spinnennetze, Beerensträucher, Pulverschnee, Portale, Leitern oder Druckplatten,
steht nie auf Magma, Kaktus, Ackerboden, Honig, Schleim, Lagerfeuern oder Eis und hält einen Block Abstand zu
Lava und Kaktus. Er bleibt im erlaubten Bereich. Er hört auf, wenn er Schaden nimmt, ins Wasser gerät, nur noch
drei Herzen hat, wenn es keinen Weg gibt, wenn er hängenbleibt (vorher versucht er einen Sprung und zwei neue
Wege) oder nach 90 Sekunden.

**Grenzen, ehrlich gesagt:** Keine diagonalen Schritte, kein Schwimmen, kein Klettern, und Türen, Tore, Zäune,
Platten, Treppen und Teppiche gelten als Hindernis. Stellen müssen auf ganzen Blöcken liegen (Trampelpfade
gehen). Das Laufen wurde nur gegen eine simulierte Welt getestet, nicht auf jedem Gelände und Server. Probiere
es zuerst im Einzelspieler aus und bleibe am Anfang in der Nähe. Die Tasten werden wie bei einem echten
Spieler gedrückt, das Spielfenster muss also im Vordergrund bleiben (siehe F3 + P oben).

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
    AngelbotAddon.java                 Hauptklasse (Start, Listener, Befehle)
    AngelbotConfiguration.java         Einstellungen
    FishingController.java             Schnittstelle zum Spiel
    SpotStore.java                     gespeicherte Stellen und Mittelpunkt des Bereichs
    bot/FishingBot.java                Zustandsautomat: auswerfen, Biss, einholen, essen, laufen
    bot/*Handle.java                   was der Bot vom Spiel braucht (Angeln, Inventar, Bewegung)
    bot/walk/                          Wegfinder, Laufsteuerung, Stellen (ohne Minecraft)
    commands/, listener/               /angelbot, Tick, Taste und Chat
  src/main/resources/assets/angelbot/i18n/   Texte (en_us, de_de)
game-runner/src/shared/java/de/angelbot/shared/
  VersionedFishingController.java      Angel, Schwimmer, Inventar, Essen und Laufen (Minecraft 26.x)
  MinecraftWorldView.java              was als sicherer Boden und freier Raum gilt
tests/                                 FishingBotTest, WalkTest, FeaturesTest
```

Die Bot-Logik (`core/.../bot`) kennt weder Minecraft noch LabyMod. Die Tests laufen ohne Gradle:

```powershell
$dateien = (Get-ChildItem core\src\main\java\de\angelbot\core\bot, core\src\main\java\de\angelbot\core\bot\walk, tests -Filter *.java).FullName
& "$env:JAVA_HOME\bin\javac" -d tests\out $dateien
foreach ($t in "FishingBotTest", "WalkTest", "FeaturesTest") { & "$env:JAVA_HOME\bin\java" -cp tests\out $t }
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
| Addon fehlt in der Liste | Falsche Jar (nur `-release.jar`), falscher Ordner (siehe Installation), Minecraft-Version außerhalb 26.1 bis 26.3, Client nicht neu gestartet |
| `/angelbot` tut nichts | Addon in den Einstellungen ausgeschaltet; Meldung oben rechts beachten |
| Bot startet nicht | Keine Angel in der Haupt- oder Nebenhand, oder keine Welt geladen |
| Bot ruht | Ein Bildschirm ist offen (auch das Pausenmenü nach einem Fensterwechsel) |
| Bot wirft immer wieder neu aus | Schwimmer landet nicht im Wasser; anders zielen |
| Meldung „Server reagiert nicht“ | Auf dem Server ist Angeln eingeschränkt oder die Verbindung hängt |
| „Angel ist verbraucht“ | Die Angel hat die Grenze erreicht und es gibt keine Ersatzangel; Grenze senken oder eine Angel dazulegen |
| „Außerhalb des erlaubten Bereichs“ | Du bist weiter vom Mittelpunkt entfernt als der Radius; Radius erhöhen oder den Mittelpunkt neu setzen |
| „Weg zur nächsten Stelle versperrt“ | Kein Weg im Bereich (Hindernis, Wasser, Stelle auf einer Platte) oder der Bot steckt fest; siehe Log |

## Lizenz

[MIT](LICENSE)
