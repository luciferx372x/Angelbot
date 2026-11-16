# Changelog

## 1.1.0

First release of the Fabric version, same bot logic as the LabyMod addon 1.1.0.

- Fishes automatically: detects the bite, reels in after a short random reaction time, waits a random
  pause and casts again.
- Spare rods, automatic eating, allowed area, detection of "spot is empty" and walking to the next saved
  spot (smooth turns).
- Commands `/angelbot`, `on`, `off`, `status`, `spot`, `zone`, `config` and `set <name> <value>`, plus a key
  in the controls menu. Settings live in `config/angelbot.json`.
- No server permission check and no settings screen (unlike the LabyMod addon).
- Builds for Minecraft 26.1.x, 26.2 and 26.3.
