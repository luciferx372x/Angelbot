package de.angelbot.core;

import de.angelbot.core.bot.BotEvents;
import de.angelbot.core.bot.ReelReason;
import de.angelbot.core.bot.StopReason;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.notification.Notification;

/**
 * Meldungen an den Spieler: Benachrichtigungen oben rechts, Debug-Zeilen im Chat und Zeilen im
 * Spiel-Log. Alles, was der Spieler sieht, läuft über die Übersetzung.
 */
final class BotMessages implements BotEvents {

  private final AngelbotAddon addon;

  BotMessages(AngelbotAddon addon) {
    this.addon = addon;
  }

  @Override
  public void stopped(StopReason reason, int bites) {
    this.push(
        Component.translatable(
            "angelbot.messages.stopped." + reason.key(), Component.text(String.valueOf(bites))));
  }

  @Override
  public void paused() {
    this.show("angelbot.messages.paused");
  }

  @Override
  public void reeled(ReelReason reason, int waitTicks, double biteSpeed) {
    // Eine Zeile je Einholen im Spiel-Log: So lässt sich später nachvollziehen, warum eingeholt wurde.
    String speed = reason == ReelReason.BITE ? ", sink speed " + format(biteSpeed) : "";
    this.addon
        .logger()
        .info("Reeled in: " + reason.key() + " after " + waitTicks + " ticks" + speed);
  }

  @Override
  public void debugBite(double speed, int reactionTicks) {
    String speedText = format(speed);
    this.addon
        .logger()
        .info(
            "[Debug] Bite detected, sink speed " + speedText + ", reacting in " + reactionTicks
                + " ticks");
    this.chat("angelbot.debug.bite", speedText, String.valueOf(reactionTicks));
  }

  @Override
  public void debugNearMiss(double speed, double threshold) {
    String speedText = format(speed);
    String thresholdText = format(threshold);
    this.addon
        .logger()
        .info("[Debug] Bobber sinks at " + speedText + " (threshold -" + thresholdText + ")");
    this.chat("angelbot.debug.nearMiss", speedText, thresholdText);
  }

  /** Zeigt die Übersetzung zum Schlüssel als Benachrichtigung. */
  void show(String translationKey) {
    this.push(Component.translatable(translationKey));
  }

  private void push(Component text) {
    Laby.references()
        .notificationController()
        .push(
            Notification.builder()
                .title(Component.translatable("angelbot.notification.title"))
                .text(text)
                .build());
  }

  /** Grau eingefärbte Chatzeile aus einer Übersetzung mit Platzhaltern. */
  private void chat(String translationKey, String first, String second) {
    Laby.labyAPI()
        .minecraft()
        .chatExecutor()
        .displayClientMessage(
            Component.translatable(translationKey, Component.text(first), Component.text(second))
                .color(NamedTextColor.GRAY));
  }

  private static String format(double value) {
    return String.valueOf(Math.round(value * 1000.0D) / 1000.0D);
  }
}
