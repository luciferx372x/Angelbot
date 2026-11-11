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

  /**
   * Server-API-Berechtigung, mit der ein Server den Bot erlaubt. Der Bot angelt, isst, tauscht
   * Inventarplätze und läuft für den Spieler, das gilt auf vielen Servern als unerlaubte
   * Client-Änderung. Deshalb ist die Berechtigung auf Servern standardmäßig aus (Vorgabe der
   * LabyMod-Prüfung). Im Einzelspieler, ohne Verbindung zu einem Server, gilt sie als erlaubt.
   */
  public static final String PERMISSION = "angelbot";

  private FishingController controller;
  private BotMessages messages;
  private FishingBot bot;
  private SpotStore spotStore;

  @Override
  protected void enable() {
    this.registerSettingCategory();

    // Aus als Standard: Nur ein Server kann den Bot erlauben, der Spieler kann es nicht erzwingen.
    this.labyAPI().permissionRegistry().register(PERMISSION, false);

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

  /**
   * Der Bot darf laufen: Der Spieler ist in keinem Server (Einzelspieler) oder der Server hat die
   * Berechtigung {@link #PERMISSION} erlaubt.
   */
  public boolean isPermitted() {
    return this.labyAPI().permissionRegistry().isPermissionEnabled(PERMISSION);
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
    } else if (!this.isPermitted()) {
      this.messages.show("angelbot.messages.notPermitted");
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

    // Auch während der Bot läuft: Wechselt der Spieler auf einen Server, der den Bot nicht erlaubt,
    // oder entzieht der Server die Berechtigung, hält der Bot sofort an.
    if (!this.isPermitted()) {
      this.bot.stop(StopReason.NOT_PERMITTED);
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
