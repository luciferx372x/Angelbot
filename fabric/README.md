# Angelbot for Fabric (NoRisk Client)

A client-side Fabric mod that fishes for you. It detects the bite, reels in after a short random reaction
time, waits a random pause and casts again. It can also switch to a spare rod, eat when you are hungry, stay
inside an allowed area and, if you want, walk to the next fishing spot when the server says that the spot
is empty. Same bot logic as the [LabyMod addon](../README.md).

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2, 26.3 (one jar per family, see below)
- **Needs:** Fabric Loader 0.19+, Fabric API, Java 25
- **Languages:** English and German. [Deutsche Version](README.de.md)

> **Servers:** Automatic fishing, and even more so automatic walking, is forbidden on many servers and can
> get you banned. This mod has no server permission check: it runs wherever you start it. Use it at your own
> risk. The random delays are not a protection against detection.

## Install

Pick the jar for your Minecraft version and put it into the `mods` folder of your profile (NoRisk Client,
Prism, vanilla launcher ...):

| Jar | Minecraft |
|---|---|
| `angelbot-fabric-1.1.0-mc26.1.x.jar` | 26.1, 26.1.1, 26.1.2 |
| `angelbot-fabric-1.1.0-mc26.2.jar` | 26.2 |
| `angelbot-fabric-1.1.0-mc26.3.jar` | 26.3 |

## Use

Hold a fishing rod, then:

| Command | Effect |
|---|---|
| `/angelbot` | Start or stop |
| `/angelbot on`, `off`, `status` | Start, stop, show state |
| `/angelbot spot add`, `list`, `remove <n>`, `clear` | Saved fishing spots |
| `/angelbot zone set`, `clear` | Center of the allowed area |
| `/angelbot config` | Lists all settings |
| `/angelbot set <name> <value>` | Changes a setting, e.g. `/angelbot set walkToNextSpot true` |

There is also a key in the controls menu ("Start / stop Angelbot"), unbound by default. Settings are saved in
`config/angelbot.json`. What the settings mean, how walking works and its limits: see the
[readme of the LabyMod addon](../README.md) (the setting names are the same).

## Build

Needs JDK 25 and Gradle 9.8 or newer. One build per Minecraft family:

```
gradle clean build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2 -Pcompat=v26_1 "-Pmc_range=>=26.1 <26.2"
gradle clean build -Pminecraft_version=26.2   -Pfabric_api_version=0.161.0+26.2   -Pcompat=v26_2 "-Pmc_range=>=26.2 <26.3"
gradle clean build -Pminecraft_version=26.3   -Pfabric_api_version=0.166.2+26.3   -Pcompat=v26_3 "-Pmc_range=>=26.3 <26.4"
```

The jar is in `build/libs`. The folder `src/compat/<family>` holds the few places that differ between
versions (open screen, arm swing, key category). The bot logic in `de.angelbot.core.bot` is shared with the
LabyMod addon.

## Status

Compiles for all three families. Not yet tested in a running NoRisk Client.

## License

[MIT](LICENSE)
