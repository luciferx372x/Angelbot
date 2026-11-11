package de.angelbot.core.listener;

import de.angelbot.core.AngelbotAddon;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

/** Schaltet den Bot mit der eingestellten Taste ein und aus. */
public class AngelbotKeyListener {

  private final AngelbotAddon addon;

  public AngelbotKeyListener(AngelbotAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    if (event.state() != KeyEvent.State.PRESS) {
      return;
    }

    Key toggleKey = this.addon.configuration().toggleKey().get();
    if (toggleKey == null || Key.NONE.equals(toggleKey) || !toggleKey.equals(event.key())) {
      return;
    }

    // Bei offenem Chat oder Inventar soll die Taste normal tippen und nichts auslösen.
    if (this.addon.controller().isScreenOpen()) {
      return;
    }

    this.addon.toggleBot();
  }
}
