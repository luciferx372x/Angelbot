package de.angelbot.core;

import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.StopReason;
import de.angelbot.core.commands.AngelbotCommand;
import de.angelbot.core.generated.DefaultReferenceStorage;
import de.angelbot.core.listener.AngelbotChatListener;
import de.angelbot.core.listener.AngelbotKeyListener;
import de.angelbot.core.listener.AngelbotTickListener;
import java.util.Random;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

/** Hauptklasse des Angelbots. Sie verbindet Einstellungen, Bot-Logik und Spielzugriff. */
@AddonMain
public class AngelbotAddon extends LabyAddon<AngelbotConfiguration> {

  private FishingController controller;
  private BotMessages messages;
  private FishingBot bot;
  private SpotStore spotStore;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    DefaultReferenceStorage storage = this.referenceStorageAccessor();
    this.controller = storage.fishingController();
    this.messages = new BotMessages(this);
    this.spotStore = new SpotStore(this);
    // Der Controller setzt Angel, Inventar und Laufen um; der Bot erkennt das selbst.
    this.bot =
        new FishingBot(this.controller, new ConfigBotSettings(this), this.messages, new Random());

    this.registerListener(new AngelbotTickListener(this));
    this.registerListener(new AngelbotKeyListener(this));
    this.registerListener(new AngelbotChatListener(this));
    this.registerCommand(new AngelbotCommand(this));

    this.logger().info("Angelbot loaded, game access: " + this.controller.getClass().getName());
  }

  @Override
  protected Class<AngelbotConfiguration> configurationClass() {
    return AngelbotConfiguration.class;
  }

  public FishingController controller() {
    return this.controller;
  }

  public FishingBot bot() {
    return this.bot;
  }

  /** Die gespeicherten Angelstellen und der Mittelpunkt des Bereichs. */
  public SpotStore spotStore() {
    return this.spotStore;
  }

  /** Eine Meldung des Servers (Chat oder Aktionsleiste) ist eingetroffen. */
  public void onServerMessage(String text) {
    // Eigene Zeilen des Addons (Debug-Ausgabe, Befehle) zählen nicht.
    if (text == null || text.contains("[Angelbot]") || text.contains("/angelbot")) {
      return;
    }

    this.bot.onChatMessage(text);
  }

  /**
   * Startet den Bot, wenn alle Voraussetzungen erfüllt sind. Andernfalls erscheint eine
   * Benachrichtigung mit dem Grund.
   */
  public void startBot() {
    if (!this.configuration().enabled().get()) {
      this.messages.show("angelbot.messages.addonDisabled");
    } else if (this.bot.isRunning()) {
      this.messages.show("angelbot.messages.alreadyRunning");
    } else if (!this.controller.isInGame()) {
      this.messages.show("angelbot.messages.notInGame");
    } else if (!this.controller.hasRod()) {
      this.messages.show("angelbot.messages.noRod");
    } else {
      this.bot.start();
      this.messages.show("angelbot.messages.started");
    }
  }

  /** Stoppt den Bot. Die Meldung dazu kommt aus {@link BotMessages#stopped}. */
  public void stopBot(StopReason reason) {
    this.bot.stop(reason);
  }

  public void toggleBot() {
    if (this.bot.isRunning()) {
      this.stopBot(StopReason.MANUAL);
    } else {
      this.startBot();
    }
  }

  /** Wird einmal pro Spiel-Tick aufgerufen. */
  public void onTick() {
    if (!this.configuration().enabled().get()) {
      this.bot.stop(StopReason.ADDON_DISABLED);
      return;
    }

    try {
      this.bot.tick();
    } catch (RuntimeException exception) {
      // Ein Fehler soll nicht jeden Tick wiederkehren und das Log fluten.
      this.logger().error("Angelbot was stopped because of an error", exception);
      this.bot.stop(StopReason.ERROR);
    }
  }
}
