# Changelog

## 1.1.0

- New command `/angelbot force`: switches the permission `angelbot` on for the current server, at your own
  risk (same effect as "Force enable" in the settings). The permission can be forced by the player, it is
  still off by default and servers can enable it.
- The "Enabled" switch is linked to the permission `angelbot`, so "Force enable" is also available with a
  right click on that setting.
- When the bot cannot walk to another spot, it now says why (switch off, no radius, no center, too few
  spots, ...).
- Walking looks smoother: turns slow down smoothly instead of at a fixed speed, the bot looks a little
  toward the next waypoint before a corner and walks while still finishing a turn.
- The default texts for "spot is empty" now also match "kaum noch Fische" and "überfischt" (an already
  saved text is kept, change it in the settings).

## 1.0.1

Changes after the LabyMod review.

- Server API permission `angelbot` (off by default): on a server the bot only runs when the server has
  enabled it, and it stops when the permission is missing or withdrawn. In single player it is always
  allowed.
- Replaced a deprecated call (`ActionBarReceiveEvent#message()` with `getMessage()`).

## 1.0.0

First release.

- Fishes automatically: detects the bite, reels in after a short random reaction time, waits a random
  pause and casts again.
- Works with Minecraft 26.1, 26.1.1, 26.1.2, 26.2 and 26.3.
- Start and stop with `/angelbot [on|off|status]` or a key of your choice.
- Safety stops: no rod in hand, bobber never reaches water, server does not react, leaving the world,
  unexpected error. Pauses while a screen is open.
- Fishing rod durability: a rod with only a few uses left is not used any more (limit adjustable). The bot
  switches to a spare rod from the hotbar or the inventory and stops when there is none.
- Eats automatically between two casts when you are hungry (allowed food and level adjustable).
- Allowed area: the bot stops when you get farther from the center than the radius you set.
- Empty fishing spots: recognizes the server message (text adjustable) and either stops or, if you turn it
  on, walks to the next saved spot (`/angelbot spot`, `/angelbot zone`). Walking avoids water, lava and other
  dangers and stops on damage, when stuck or when there is no way.
- Every setting can be changed in game. English and German translations.
