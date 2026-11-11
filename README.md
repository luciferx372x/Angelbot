# Angelbot for LabyMod 4

An addon for the Laby Client that fishes for you. It detects the bite, reels in after a short random
reaction time, waits a random pause and casts again.

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2 and 26.3
- **Client:** Laby Client (LabyMod 4) as an addon. No Forge, no Fabric.
- **Languages:** English and German (follows the Minecraft language). [Deutsche Version](README.de.md)

> **Servers:** Automatic fishing is forbidden on many servers and can get you banned. Use it in single
> player or where it is allowed. The random delays are not a protection against detection.

## Install

Install it from the addon store in the Laby Client (once it is listed), or by hand: put
`angelbot-release.jar` into the addon folder and restart the client.

| System | Addon folder |
|---|---|
| Windows | `%APPDATA%\.minecraft\labymod-neo\addons` |
| Linux | `~/.minecraft/labymod-neo/addons` |
| macOS | `~/Library/Application Support/minecraft/labymod-neo/addons` |

## Use

1. Hold a fishing rod (main hand or off hand) and stand at water.
2. Start the bot with the command `/angelbot` (toggles), `/angelbot on` or `/angelbot off`, or with a key
   of your choice (Settings → Addons → Angelbot → *Toggle key*). The key does nothing while a screen such
   as the chat is open.
3. `/angelbot status` shows state, bites and run time.

The bot switches itself off (with a notification) when you leave the world, when no rod is held for 10
seconds (for example because it broke), when the bobber misses the water five times in a row, when the
server does not react to the rod five times in a row, or on an unexpected error.

**Fishing in the background:** When you switch windows, Minecraft opens the pause menu. While a screen is
open the bot rests (after 10 seconds you get a hint). Press **F3 + P** to stop Minecraft from pausing when
the window loses focus, or turn off *Pause while a screen is open* in the addon settings.

## Settings

Settings → Addons → Angelbot.

| Setting | Default | Meaning |
|---|---|---|
| Enabled | on | Switches the whole addon on or off |
| Toggle key | none | Starts and stops the bot |
| Reaction time, shortest / longest | 200 / 450 ms | Wait after the bite before reeling in (random in between) |
| Pause before casting, shortest / longest | 700 / 1400 ms | Pause after reeling in before casting again |
| Timeout without a bite | 60 s | Then the rod is reeled in and cast again |
| Pause while a screen is open | on | The bot rests while chat, inventory or a menu is open |
| Stop without a rod | on | Switches the bot off after 10 s without a rod in hand |
| Bite threshold | 15 | Sink speed of the bobber (hundredths of a block per tick) that counts as a bite |
| Settle time | 20 ticks | How long after first touching the water the bot waits before it watches for bites |
| Debug output | off | Reports in chat and in `latest.log` when the bobber almost reaches the threshold |

The fish stays on the hook for only **1 to 2 seconds** after the bite. Keep the longest reaction time
below about 600 ms, and lower with a high ping.

## How the bite is detected

When a fish bites, Minecraft pulls the bobber down with a jerk (about 0.24 to 0.40 blocks per tick).
Without a bite the bobber only moves slightly. The bot watches the vertical speed of the bobber. When at
least the settle time has passed since the bobber first touched the water and it sinks faster than the bite
threshold, that counts as a bite.

Whether the bobber is in water is checked the way Minecraft does it (water in the bobber's block). A bobber
that never touches water is reeled in after 8 seconds and cast again, one that leaves the water after
3 seconds. For every reel-in the addon writes one line to `latest.log`, for example
`Reeled in: bite after 312 ticks, sink speed -0.31`, so you can see why it reeled in.

Tuning, if needed:

- **Reels in without a bite:** raise the bite threshold (for example 20) or the settle time.
- **Misses bites:** turn on *Debug output* and watch. If the chat reports "Bobber sinks at -0.12" while
  the threshold is 0.15, lower the bite threshold to 10.

## Build it yourself

You need **JDK 25** (for example Eclipse Temurin 25). The included wrapper downloads Gradle 9.7.1. The
first build downloads LabyGradle, the LabyMod API and the Minecraft files (about 1 to 3 GB).

```bash
export JAVA_HOME=/path/to/jdk-25     # Windows PowerShell: $env:JAVA_HOME = "C:\path\to\jdk-25"
./gradlew build
./gradlew createReleaseJar
```

The addon is `build/libs/angelbot-release.jar`. Only the file with the `-release` suffix works in the
normal client.

`./gradlew :game-runner:client_v26.2` starts a development client with the addon (no account needed; the
other versions are `client_v26.1`, `client_v26.1.1`, `client_v26.1.2` and `client_v26.3`).
`./gradlew checkAddon` checks the sources against the addon store guidelines and for missing translations.

### Project layout

```
build.gradle.kts, settings.gradle.kts, gradle.properties   Gradle setup (LabyGradle 0.11.0)
core/                                version independent part
  src/main/java/de/angelbot/core/
    AngelbotAddon.java               main class (start, listeners, command)
    AngelbotConfiguration.java       settings
    FishingController.java           interface to the game
    bot/FishingBot.java              state machine: cast, bite, reel in
    commands/, listener/             /angelbot, tick and key
  src/main/resources/assets/angelbot/i18n/   texts (en_us, de_de)
game-runner/src/shared/java/de/angelbot/shared/
  VersionedFishingController.java    access to rod, bobber and right click (Minecraft 26.x)
tests/FishingBotTest.java            tests of the bot logic with a simulated game
```

The bot logic (`core/.../bot`) knows neither Minecraft nor LabyMod. The test runs without Gradle:

```bash
"$JAVA_HOME/bin/javac" -d tests/out core/src/main/java/de/angelbot/core/bot/*.java tests/FishingBotTest.java
"$JAVA_HOME/bin/java" -cp tests/out FishingBotTest
```

### Supporting another Minecraft version

1. Add the version to `net.labymod.minecraft-versions` in `gradle.properties`.
2. Adjust the range `minecraftVersion = "26.1<26.3"` in `build.gradle.kts`.
3. Run `./gradlew build`. If something `VersionedFishingController` uses is missing in the new version,
   LabyGradle names the affected version. Then branch with `MinecraftTarget` there (see the
   [wiki on version dependent code](https://github.com/LabyMod/labymod4-api-wiki/blob/master/docs/pages/addon/features/shared-version-code.md)).

## Troubleshooting

| Problem | Possible cause |
|---|---|
| Addon not in the list | Wrong jar (only `-release.jar`), wrong folder, Minecraft version outside 26.1 to 26.3, client not restarted |
| `/angelbot` does nothing | Addon switched off in the settings; look at the notification at the top right |
| Bot does not start | No fishing rod in main or off hand, or no world loaded |
| Bot rests | A screen is open (also the pause menu after switching windows) |
| Bot keeps casting again | The bobber does not land in water; aim differently |
| "Server does not react" | Fishing is restricted on the server or the connection hangs |

## License

[MIT](LICENSE)
