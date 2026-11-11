package de.angelbot.core.bot;

/** Gründe, aus denen sich der Bot beendet. */
public enum StopReason {

  /** Der Spieler hat den Bot per Befehl oder Taste gestoppt. */
  MANUAL("manual"),

  /** Der Spieler hat die Welt oder den Server verlassen. */
  LEFT_GAME("leftGame"),

  /** Es lag zu lange keine Angel in der Hand. */
  NO_ROD("noRod"),

  /** Die Angel ist verbraucht (Haltbarkeitsgrenze oder kaputt) und es gibt keine Ersatzangel. */
  NO_SPARE_ROD("noSpareRod"),

  /** Der Schwimmer landet mehrfach hintereinander nicht im Wasser. */
  NO_WATER("noWater"),

  /** Auswerfen oder Einholen wird mehrfach hintereinander nicht bestätigt. */
  NO_RESPONSE("noResponse"),

  /** Der Spieler ist weiter vom Mittelpunkt des erlaubten Bereichs entfernt als eingestellt. */
  OUT_OF_ZONE("outOfZone"),

  /** Der Server meldet, dass die Stelle leer ist, und es geht nicht zu einer anderen Stelle. */
  SPOT_EMPTY("spotEmpty"),

  /** Alle gespeicherten Stellen sind leer gefischt (oder noch nicht wieder bereit). */
  ALL_SPOTS_EMPTY("allSpotsEmpty"),

  /** Der Weg zur nächsten Stelle gibt es nicht oder der Spieler kommt nicht ans Ziel. */
  WALK_FAILED("walkFailed"),

  /** Beim Laufen gab es Schaden, Wasser oder zu wenig Leben. */
  WALK_DANGER("walkDanger"),

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
