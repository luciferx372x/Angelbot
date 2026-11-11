package de.angelbot.core.listener;

import de.angelbot.core.AngelbotAddon;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;

/** Gibt dem Bot einmal pro Spiel-Tick Gelegenheit zu arbeiten. */
public class AngelbotTickListener {

  private final AngelbotAddon addon;

  public AngelbotTickListener(AngelbotAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onGameTick(GameTickEvent event) {
    if (event.phase() != Phase.PRE) {
      return;
    }

    this.addon.onTick();
  }
}
