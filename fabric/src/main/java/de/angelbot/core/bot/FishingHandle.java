package de.angelbot.core.bot;

/**
 * Schmale Schnittstelle zwischen der Bot-Logik und dem Spiel.
 *
 * <p>Die Bot-Logik ({@link FishingBot}) kennt nur diese Methoden und ist damit frei von Minecraft-
 * und LabyMod-Klassen. Die Umsetzung für das echte Spiel steht im Modul {@code game-runner}.
 */
public interface FishingHandle {

  /** Spieler und Welt sind geladen. */
  boolean isInGame();

  /** Ein Bildschirm (Chat, Inventar, Menü ...) ist offen. */
  boolean isScreenOpen();

  /** Eine Angel liegt in der Haupt- oder der Nebenhand. */
  boolean hasRod();

  /** Die Angel ist ausgeworfen, der Schwimmer existiert. */
  boolean isCast();

  /**
   * Der Schwimmer ist im Wasser, also Wasser im Block des Schwimmers (so wie Minecraft selbst
   * entscheidet). Beim Schaukeln an der Oberfläche darf der Wert gelegentlich kurz abweichen, die
   * Bot-Logik kommt damit zurecht.
   */
  boolean isBobberInWater();

  /** Der Haken hängt an einem Lebewesen oder Gegenstand. */
  boolean isHookedToEntity();

  /** Senkrechte Geschwindigkeit des Schwimmers in Blöcken pro Tick (negativ = nach unten). */
  double bobberVerticalSpeed();

  /**
   * Rechtsklick mit der Angel: wirft aus oder holt ein.
   *
   * @return {@code false}, wenn nichts gesendet werden konnte (zum Beispiel keine Angel in der Hand)
   */
  boolean useRod();
}
