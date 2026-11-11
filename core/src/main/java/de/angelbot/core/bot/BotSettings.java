package de.angelbot.core.bot;

/** Einstellungen, die der Bot bei jedem Tick frisch liest. Alle Werte sind jederzeit änderbar. */
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
}
