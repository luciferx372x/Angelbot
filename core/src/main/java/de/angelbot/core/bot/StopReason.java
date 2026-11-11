package de.angelbot.core.bot;

/** Gründe, aus denen sich der Bot beendet. */
public enum StopReason {

  /** Der Spieler hat den Bot per Befehl oder Taste gestoppt. */
  MANUAL("manual"),

  /** Der Spieler hat die Welt oder den Server verlassen. */
  LEFT_GAME("leftGame"),

  /** Es lag zu lange keine Angel in der Hand. */
  NO_ROD("noRod"),

  /** Der Schwimmer landet mehrfach hintereinander nicht im Wasser. */
  NO_WATER("noWater"),

  /** Auswerfen oder Einholen wird mehrfach hintereinander nicht bestätigt. */
  NO_RESPONSE("noResponse"),

  /** Das Addon wurde in den Einstellungen ausgeschaltet. */
  ADDON_DISABLED("addonDisabled"),

  /** Ein unerwarteter Fehler ist aufgetreten (Einzelheiten stehen im Spiel-Log). */
  ERROR("error");

  private final String key;

  StopReason(String key) {
    this.key = key;
  }

  /** Teil des Übersetzungsschlüssels ({@code angelbot.messages.stopped.<key>}). */
  public String key() {
    return this.key;
  }
}
