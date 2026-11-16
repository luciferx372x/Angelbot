package de.angelbot.core;

import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.StopReason;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hauptklasse des Angelbots. Sie verbindet Einstellungen, Bot-Logik und Spielzugriff. */
public final class AngelbotAddon {

  private static final Logger LOGGER = LoggerFactory.getLogger("angelbot");

  private final FishingController controller;
  private final Config config;
  private final ModSettings settings;
  private final BotMessages messages;
  private final FishingBot bot;
  private final SpotStore spotStore;

  public AngelbotAddon(FishingController controller, Config config) {
    this.controller = controller;
    this.config = config;
    this.settings = new ModSettings(config);
    this.messages = new BotMessages(this);
    this.spotStore = new SpotStore(config);
    this.bot = new FishingBot(controller, this.settings, this.messages, new Random());
    LOGGER.info("Angelbot loaded, game access: {}", controller.getClass().getName());
  }

  public Logger logger() {
    return LOGGER;
  }

  public FishingController controller() {
    return this.controller;
  }

  public FishingBot bot() {
    return this.bot;
  }

  public SpotStore spotStore() {
    return this.spotStore;
  }

  public Config config() {
    return this.config;
  }

  public ModSettings settings() {
    return this.settings;
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
   * Startet den Bot, wenn alle Voraussetzungen erfüllt sind. Andernfalls erscheint eine Meldung mit
   * dem Grund.
   */
  public void startBot() {
    if (!this.config.getBool("enabled")) {
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
    if (!this.config.getBool("enabled")) {
      this.bot.stop(StopReason.ADDON_DISABLED);
      return;
    }

    try {
      this.bot.tick();
    } catch (RuntimeException exception) {
      // Ein Fehler soll nicht jeden Tick wiederkehren und das Log fluten.
      LOGGER.error("Angelbot was stopped because of an error", exception);
      this.bot.stop(StopReason.ERROR);
    }
  }
}
