package de.angelbot.core.bot;

import de.angelbot.core.bot.walk.WorldView;

/**
 * Position, Blickrichtung und Tastensteuerung des Spielers. Der Bot braucht sie nur zum Laufen zur
 * nächsten Angelstelle und für die Bereichsgrenze.
 *
 * <p>Alle Methoden haben eine Standardumsetzung, die nichts kann. So bleibt der Bot ohne diese
 * Fähigkeit voll funktionsfähig (und die älteren Tests bleiben gültig). Die Umsetzung für das echte
 * Spiel steht im Modul {@code game-runner}.
 */
public interface MovementHandle {

  /** Position und Steuerung stehen zur Verfügung (Spieler und Welt sind geladen). */
  default boolean canMove() {
    return false;
  }

  /** Position des Spielers (Füße). */
  default double x() {
    return 0.0D;
  }

  default double y() {
    return 0.0D;
  }

  default double z() {
    return 0.0D;
  }

  /** Blickrichtung wie in Minecraft: 0 = Süden (+z), 90 = Westen (-x), 180 = Norden, -90 = Osten. */
  default float yaw() {
    return 0.0F;
  }

  /** Neigung: 0 = geradeaus, positive Werte schauen nach unten. */
  default float pitch() {
    return 0.0F;
  }

  /** Dreht den Spieler. */
  default void setLook(float yaw, float pitch) {}

  /**
   * Setzt die Tasten für diesen Tick: vorwärts laufen und springen. {@code (false, false)} lässt alle
   * Tasten los.
   */
  default void setMovement(boolean forward, boolean jump) {}

  /** Der Spieler steht auf dem Boden. */
  default boolean isOnGround() {
    return true;
  }

  /** Lebenspunkte (20 = voll). */
  default float health() {
    return 20.0F;
  }

  /** Der Spieler schwimmt oder steckt in einer Flüssigkeit (Wasser, Lava). */
  default boolean isInLiquid() {
    return false;
  }

  /** Blick auf die Welt für den Wegfinder, oder {@code null}, wenn nicht verfügbar. */
  default WorldView world() {
    return null;
  }
}
