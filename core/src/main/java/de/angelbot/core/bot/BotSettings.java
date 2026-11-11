package de.angelbot.core.bot;

/**
 * Einstellungen, die der Bot bei jedem Tick frisch liest. Alle Werte sind jederzeit änderbar.
 *
 * <p>Die Angaben ab {@link #minRodDurability()} haben Standardwerte, bei denen die jeweilige
 * Fähigkeit aus ist.
 */
public interface BotSettings {

  /** Kürzeste Reaktionszeit nach dem Biss in Millisekunden. */
  int reactionMinMs();

  /** Längste Reaktionszeit nach dem Biss in Millisekunden. */
  int reactionMaxMs();

  /** Kürzeste Pause zwischen Einholen und erneutem Auswerfen in Millisekunden. */
  int recastMinMs();

  /** Längste Pause zwischen Einholen und erneutem Auswerfen in Millisekunden. */
  int recastMaxMs();

  /** Nach dieser Zeit ohne Biss wird die Angel eingeholt und neu ausgeworfen. */
  int timeoutSeconds();

  /** So viele Ticks muss der Schwimmer im Wasser liegen, bevor auf Bisse geachtet wird. */
  int settleTicks();

  /** Ab dieser Sinkgeschwindigkeit (Blöcke pro Tick, positiver Wert) zählt es als Biss. */
  double biteThreshold();

  /** Der Bot ruht, solange ein Bildschirm (Chat, Inventar ...) offen ist. */
  boolean pauseWhileScreenOpen();

  /** Der Bot schaltet sich ab, wenn länger keine Angel in der Hand liegt. */
  boolean stopWithoutRod();

  /** Zusätzliche Ausgaben für die Fehlersuche. */
  boolean debug();

  /**
   * Eine Angel mit höchstens so vielen Benutzungen Rest gilt als verbraucht und wird nicht mehr
   * benutzt. 0 heißt: bis sie kaputtgeht.
   */
  default int minRodDurability() {
    return 0;
  }

  /** Bei einer verbrauchten Angel auf eine andere wechseln, falls es eine gibt. */
  default boolean switchRods() {
    return false;
  }

  /** Bei Hunger automatisch essen. */
  default boolean autoEat() {
    return false;
  }

  /** Es wird gegessen, wenn der Hungerwert (0 bis 20) höchstens so hoch ist. */
  default int eatBelow() {
    return 14;
  }

  /** Namen der erlaubten Nahrung, durch Komma getrennt, in der Reihenfolge der Vorliebe. */
  default String foods() {
    return "";
  }

  /** Radius des erlaubten Bereichs in Blöcken. 0 heißt: keine Grenze. */
  default int zoneRadius() {
    return 0;
  }

  /** Der Mittelpunkt des Bereichs wurde festgelegt. Sonst ist es die Stelle, an der der Bot startet. */
  default boolean hasZoneCenter() {
    return false;
  }

  default double zoneCenterX() {
    return 0.0D;
  }

  default double zoneCenterZ() {
    return 0.0D;
  }

  /** Textteile (durch Komma getrennt), an denen eine Chatmeldung "Stelle leer" erkannt wird. */
  default String emptyMessages() {
    return "";
  }

  /** Bei leerer Stelle zur nächsten gespeicherten Stelle laufen. */
  default boolean walkToNextSpot() {
    return false;
  }

  /** So lange gilt eine leere Stelle als leer, bevor der Bot sie wieder besucht (Minuten). */
  default int spotCooldownMinutes() {
    return 10;
  }

  /** Die gespeicherten Stellen als Text (siehe {@code Spot}). */
  default String spots() {
    return "";
  }
}
