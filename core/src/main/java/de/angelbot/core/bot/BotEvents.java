package de.angelbot.core.bot;

/**
 * Rückmeldungen des Bots an die Oberfläche. Der Bot selbst erzeugt keine Texte für den Spieler, die
 * Umsetzung übernimmt die Übersetzung.
 */
public interface BotEvents {

  /** Der Bot hat sich beendet (gewollt oder wegen eines Problems). */
  void stopped(StopReason reason, int bites);

  /** Der Bot ruht seit einigen Sekunden, weil ein Bildschirm offen ist. Kommt je Pause einmal. */
  void paused();

  /**
   * Der Bot hat die Angel eingeholt. Kommt bei jedem Einholen, damit sich im Log nachvollziehen
   * lässt, warum eingeholt wurde.
   *
   * @param reason Grund fürs Einholen
   * @param waitTicks Ticks seit dem Auswerfen (der Schwimmer war da)
   * @param biteSpeed Sinkgeschwindigkeit beim Biss in Blöcken pro Tick (negativ), sonst 0
   */
  void reeled(ReelReason reason, int waitTicks, double biteSpeed);

  /** Nur bei eingeschaltetem Debug-Modus: Ein Biss wurde erkannt. */
  void debugBite(double speed, int reactionTicks);

  /**
   * Nur bei eingeschaltetem Debug-Modus: Der Schwimmer sinkt auffällig, erreicht die Bissschwelle
   * aber nicht.
   */
  void debugNearMiss(double speed, double threshold);
}
