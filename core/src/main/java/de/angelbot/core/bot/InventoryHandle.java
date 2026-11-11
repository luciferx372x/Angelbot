package de.angelbot.core.bot;

/**
 * Inventar des Spielers: Haltbarkeit der Angel, Wechsel auf eine Ersatzangel und Essen.
 *
 * <p>Alle Methoden haben eine Standardumsetzung, die nichts kann. So bleibt der Bot ohne diese
 * Fähigkeit voll funktionsfähig (und die älteren Tests bleiben gültig). Die Umsetzung für das echte
 * Spiel steht im Modul {@code game-runner}.
 */
public interface InventoryHandle {

  /**
   * Verbleibende Haltbarkeit der Angel, die {@link FishingHandle#useRod()} benutzen würde.
   *
   * @return {@link Integer#MAX_VALUE}, wenn die Angel nicht kaputtgehen kann oder keine in der Hand liegt
   */
  default int rodDurabilityLeft() {
    return Integer.MAX_VALUE;
  }

  /**
   * Sorgt dafür, dass eine Angel mit mehr als {@code minLeft} Haltbarkeit benutzt wird: erst die
   * Hotbar, dann das Inventar. Die verbrauchte Angel bleibt im Inventar.
   *
   * @return {@code true}, wenn jetzt eine solche Angel in der Hand liegt
   */
  default boolean equipSpareRod(int minLeft) {
    return false;
  }

  /** Hungerwert des Spielers (0 bis 20, 20 = satt). */
  default int foodLevel() {
    return 20;
  }

  /**
   * Beginnt zu essen. Welche Nahrung gewählt wird, bestimmt die Liste {@code foodIds} (Namen der
   * Gegenstände, durch Komma getrennt, in der Reihenfolge der Vorliebe).
   *
   * @return {@code false}, wenn nichts Passendes gefunden wurde
   */
  default boolean startEating(String foodIds) {
    return false;
  }

  /** Der Spieler isst gerade. */
  default boolean isEating() {
    return false;
  }

  /** Beendet das Essen und legt die Angel wieder in die Hand. */
  default void stopEating() {}
}
