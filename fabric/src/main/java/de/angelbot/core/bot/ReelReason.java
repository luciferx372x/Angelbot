package de.angelbot.core.bot;

/** Gründe, aus denen der Bot die Angel einholt. */
public enum ReelReason {

  /** Ein Fisch hat angebissen. */
  BITE("bite"),

  /** Es hat lange nichts gebissen, die Angel wird neu ausgeworfen. */
  TIMEOUT("timeout"),

  /** Der Haken hängt an einem Lebewesen oder Gegenstand. */
  HOOKED("hooked"),

  /** Der Schwimmer hat nie Wasser berührt. */
  NO_WATER("noWater"),

  /** Der Schwimmer hat das Wasser wieder verlassen. */
  LEFT_WATER("leftWater");

  private final String key;

  ReelReason(String key) {
    this.key = key;
  }

  /** Kurzer Name für das Log. */
  public String key() {
    return this.key;
  }
}
