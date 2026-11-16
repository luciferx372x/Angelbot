# Angelbot für Fabric (NoRisk Client)

Eine Fabric-Mod (nur Client), die vollautomatisch angelt. Sie erkennt den Biss, holt die Angel nach einer
kurzen, zufälligen Reaktionszeit ein, wartet eine zufällige Pause und wirft sie wieder aus. Dazu kann sie auf
eine Ersatzangel wechseln, bei Hunger essen, in einem erlaubten Bereich bleiben und, wenn du es willst, zur
nächsten Angelstelle laufen, sobald der Server meldet, dass die Stelle leer ist. Gleiche Bot-Logik wie das
[LabyMod-Addon](../README.de.md).

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2, 26.3 (eine Jar je Familie, siehe unten)
- **Braucht:** Fabric Loader 0.19+, Fabric API, Java 25
- **Sprachen:** Deutsch und Englisch. [English version](README.md)

> **Hinweis zu Servern:** Auf vielen Servern ist automatisches Angeln verboten, automatisches Laufen erst
> recht, und beides kann zum Bann führen. Diese Mod prüft keine Server-Berechtigung: Sie läuft überall, wo du
> sie startest, auf eigenes Risiko. Die zufälligen Zeiten sind kein Schutz vor Erkennung.

## Installation

Die Jar zu deiner Minecraft-Version in den Ordner `mods` deines Profils legen (NoRisk Client, Prism,
Standard-Launcher ...):

| Jar | Minecraft |
|---|---|
| `angelbot-fabric-1.1.0-mc26.1.x.jar` | 26.1, 26.1.1, 26.1.2 |
| `angelbot-fabric-1.1.0-mc26.2.jar` | 26.2 |
| `angelbot-fabric-1.1.0-mc26.3.jar` | 26.3 |

## Benutzung

Angel in die Hand nehmen, dann:

| Befehl | Wirkung |
|---|---|
| `/angelbot` | Starten oder stoppen |
| `/angelbot on`, `off`, `status` | Starten, stoppen, Zustand zeigen |
| `/angelbot spot add`, `list`, `remove <n>`, `clear` | Gespeicherte Angelstellen |
| `/angelbot zone set`, `clear` | Mittelpunkt des erlaubten Bereichs |
| `/angelbot config` | Listet alle Einstellungen |
| `/angelbot set <Name> <Wert>` | Ändert eine Einstellung, zum Beispiel `/angelbot set walkToNextSpot true` |

Unter Steuerung gibt es außerdem eine Taste („Angelbot starten / stoppen“), anfangs nicht belegt. Die
Einstellungen stehen in `config/angelbot.json`. Was sie bedeuten, wie das Laufen funktioniert und wo seine
Grenzen sind, steht in der [Readme des LabyMod-Addons](../README.de.md) (die Namen der Einstellungen sind
gleich).

## Selbst bauen

Braucht JDK 25 und Gradle 9.8 oder neuer. Ein Bau je Minecraft-Familie (Befehle siehe englische Readme). Die
Jar liegt danach in `build/libs`. Der Ordner `src/compat/<Familie>` enthält die wenigen Stellen, die sich
zwischen den Versionen unterscheiden.

## Stand

Baut für alle drei Familien. Im laufenden NoRisk Client noch nicht ausprobiert.

## Lizenz

[MIT](LICENSE)
