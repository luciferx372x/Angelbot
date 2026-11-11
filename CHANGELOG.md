# Changelog

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
