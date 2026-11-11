# Angelbot for LabyMod 4

An addon for the Laby Client that fishes for you. It detects the bite, reels in after a short random
reaction time, waits a random pause and casts again. It can also switch to a spare rod, eat when you are
hungry, stay inside an allowed area and, if you want, walk to the next fishing spot when the server says
that the current spot is empty.

- **Minecraft:** 26.1, 26.1.1, 26.1.2, 26.2 and 26.3
- **Client:** Laby Client (LabyMod 4) as an addon. No Forge, no Fabric.
- **Languages:** English and German (follows the Minecraft language). [Deutsche Version](README.de.md)

> **Servers:** Automatic fishing, and even more so automatic walking, is forbidden on many servers and can
> get you banned. That is why the addon follows the LabyMod Server API: in single player it always works,
> on a server it only runs when the server has enabled the permission **`angelbot`** (off by default, see
> [Server permission](#server-permission)). The random delays are not a protection against detection.

## Install

Install it from the addon store in the Laby Client (once it is listed), or by hand: put
`angelbot-release.jar` into the `addons` folder of your instance and restart the client.

| LabyMod Launcher | Addon folder |
|---|---|
| Version 3 (Windows) | `%APPDATA%\LabyMod\instances\<instance id>\addons`, one folder per instance |
| Older versions | Windows `%APPDATA%\.minecraft\labymod-neo\addons`, Linux `~/.minecraft/labymod-neo/addons`, macOS `~/Library/Application Support/minecraft/labymod-neo/addons` |

If the addon is not in the list after a restart, the jar is most likely in the wrong folder.

## Use

1. Hold a fishing rod (main hand or off hand) and stand at water.
2. Start the bot with the command `/angelbot` (toggles), `/angelbot on` or `/angelbot off`, or with a key
   of your choice (Settings → Addons → Angelbot → *Toggle key*). The key does nothing while a screen such
   as the chat is open.
3. `/angelbot status` shows state, bites and run time.

| Command | What it does |
|---|---|
| `/angelbot` | Starts or stops the bot |
| `/angelbot on`, `/angelbot off` | Starts or stops the bot |
| `/angelbot status` | State, bites and run time |
| `/angelbot spot add` | Saves the place where you stand, with your viewing direction, as a fishing spot |
| `/angelbot spot list`, `spot remove <number>`, `spot clear` | Shows, removes or clears the saved spots |
| `/angelbot zone set` | Sets the center of the allowed area to the place where you stand |
| `/angelbot zone`, `zone clear` | Shows or removes the center |

The bot switches itself off (with a notification) when you leave the world, when no rod is held for 10
seconds, when the rod is used up and there is no spare rod, when you leave the allowed area, when the bobber
misses the water five times in a row, when the server does not react to the rod five times in a row, when
the server says the spot is empty and the bot cannot walk to another one, or on an unexpected error.

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
| Switch to a spare rod | on | Takes another rod when the current one is used up or breaks, otherwise the bot stops |
| Rod is used up at (uses left) | 5 | A rod with this many uses left or fewer is not used any more. 0 = until it breaks |
| Eat automatically | on | Eats between two casts when you are hungry (only while the bot runs) |
| Eat at food level | 14 | The bot eats at this food level (20 = full) or below |
| Allowed food | common safe food | Item names, separated by commas, best first |
| Allowed area radius | 0 (off) | The bot stops when you are farther than this from the center of the area |
| Chat text: spot is empty | `keine fische,no fish` | A server message with one of these texts means that the spot is empty |
| Walk to the next spot | off | Walks to the next saved spot when the spot is empty |
| Empty spot rests for | 10 min | An empty spot is not visited again for this long |
| Bite threshold | 15 | Sink speed of the bobber (hundredths of a block per tick) that counts as a bite |
| Settle time | 20 ticks | How long after first touching the water the bot waits before it watches for bites |
| Debug output | off | Reports in chat and in `latest.log` when the bobber almost reaches the threshold |

The fish stays on the hook for only **1 to 2 seconds** after the bite. Keep the longest reaction time
below about 600 ms, and lower with a high ping.

## Fishing rod: durability and spare rods

A new rod has 64 uses. *Rod is used up at* sets how many uses have to be left at least (default 5), so a
good rod is not used until it breaks. The bot checks before every cast. When the rod in your hand is used
up, it takes another rod that is better than the limit: first from the hotbar, then from the inventory
(swapped into the selected hotbar slot, the used-up rod goes to the inventory). Without a spare rod the bot
stops and tells you. With the limit at 0 the rod is used until it breaks, and the bot switches (or stops)
right when it does. If you turn off *Switch to a spare rod*, the bot stops as soon as the rod is used up.

## Eating

When your food level is at or below *Eat at food level*, the bot eats between two casts, never while the
bobber is out. It only eats the items in *Allowed food* (the first one it finds, in the order of the list:
off hand, hotbar, then inventory). The default list is common safe food, without golden apples. The bot puts
the rod back in your hand afterwards. If nothing from the list is found, you get a notification (at most once
a minute) and the bot keeps fishing. Nothing happens while the bot is off.

## Allowed area

*Allowed area radius* limits how far you may get from the center of the area. The center is the place where
you started the bot, or a fixed center that you set with `/angelbot zone set`. When you are farther away than
the radius (plus one block), the bot stops. This also protects you when something pushes or teleports you.
0 means no limit.

## Empty fishing spots and walking

Some servers tell you when a fishing spot is used up. When a message with one of the texts in *Chat text:
spot is empty* arrives (in the chat or the action bar, messages from players are ignored), the bot reacts:

- **Walking off (default):** the bot reels in, stops and tells you. You move on yourself.
- **Walking on:** the bot reels in and walks to the next saved spot. Set it up once:
  1. Set *Allowed area radius* (for example 15) and stand in the middle of the area: `/angelbot zone set`.
  2. Stand at every spot where you fish, look at the water and use `/angelbot spot add` (at least two spots).
  3. Turn on *Walk to the next spot* and start the bot.

  The bot walks to the next spot in the list that is not resting, turns to the saved viewing direction,
  waits a second and casts. A spot that was reported as empty rests for the set time. When all spots rest,
  the bot stops.

**How the bot walks, and what it avoids:** the path is found with A\* over the loaded blocks: straight
steps in four directions, one block up (with a jump) and down by up to two blocks. It never walks through
water, lava, fire, cobwebs, berry bushes, powder snow, portals, ladders or pressure plates, never stands on
magma, cactus, farmland, honey, slime, campfires or ice, and keeps one block away from lava and cactus. It
stays inside the allowed area. It stops when it takes damage, gets into water, has three hearts or fewer, when
there is no way, when it gets stuck (it tries a jump and two new paths first) or after 90 seconds.

**Limits, honestly:** no diagonal steps, no swimming, no climbing, and doors, gates, fences, slabs, stairs and
carpets count as obstacles. Spots have to be on full blocks (dirt paths work). Walking has only been tested
against a simulated world, not on every kind of terrain and server. Try it in single player first and stay
close to the bot at the beginning. Keys are pressed like a real player would, so the game window has to stay
focused (see F3 + P above).

## Server permission

The addon registers the permission **`angelbot`** in the LabyMod Server API. It is **off by default** and
decides whether the bot may run at all:

- **Single player (no server):** always allowed.
- **On a server:** only when the server has enabled the permission `angelbot`. Without it, `/angelbot on` and
  the toggle key only show a message. A bot that is already running stops as soon as you join such a
  server or the server withdraws the permission.

Players cannot turn the permission on themselves, only the server can. Server owners who want to allow the
bot enable the permission `angelbot` for their players through the LabyMod Server API.

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
    AngelbotAddon.java               main class (start, listeners, commands)
    AngelbotConfiguration.java       settings
    FishingController.java           interface to the game
    SpotStore.java                   saved spots and the center of the area
    bot/FishingBot.java              state machine: cast, bite, reel in, eat, walk
    bot/*Handle.java                 what the bot needs from the game (fishing, inventory, movement)
    bot/walk/                        path finder, walker, spots (no Minecraft inside)
    commands/, listener/             /angelbot, tick, key and chat
  src/main/resources/assets/angelbot/i18n/   texts (en_us, de_de)
game-runner/src/shared/java/de/angelbot/shared/
  VersionedFishingController.java    rod, bobber, inventory, eating and walking (Minecraft 26.x)
  MinecraftWorldView.java            what counts as safe ground and free space
tests/                               FishingBotTest, WalkTest, FeaturesTest
```

The bot logic (`core/.../bot`) knows neither Minecraft nor LabyMod. The tests run without Gradle:

```bash
"$JAVA_HOME/bin/javac" -d tests/out core/src/main/java/de/angelbot/core/bot/*.java \
    core/src/main/java/de/angelbot/core/bot/walk/*.java tests/*.java
for t in FishingBotTest WalkTest FeaturesTest; do "$JAVA_HOME/bin/java" -cp tests/out $t; done
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
| Addon not in the list | Wrong jar (only `-release.jar`), wrong folder (see Install), Minecraft version outside 26.1 to 26.3, client not restarted |
| `/angelbot` does nothing | Addon switched off in the settings; look at the notification at the top right |
| Bot does not start | No fishing rod in main or off hand, or no world loaded |
| Bot does not start on a server | The server has not enabled the permission `angelbot` (LabyMod Server API); in single player it always works |
| Bot rests | A screen is open (also the pause menu after switching windows) |
| Bot keeps casting again | The bobber does not land in water; aim differently |
| "Server does not react" | Fishing is restricted on the server or the connection hangs |
| "Rod is used up" | The rod reached the limit and there is no spare rod; lower the limit or add a rod |
| "Outside the allowed area" | You are farther from the center than the radius; raise the radius or set the center again |
| "Could not walk to the next spot" | No way inside the area (obstacle, water, a spot on a slab) or the bot got stuck; see the log |

## License

[MIT](LICENSE)
