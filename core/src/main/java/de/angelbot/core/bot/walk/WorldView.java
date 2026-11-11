package de.angelbot.core.bot.walk;

/**
 * Was der Wegfinder über die Welt wissen muss. Alle Koordinaten sind Blockkoordinaten, {@code y} ist
 * die Höhe der Füße. Die Umsetzung für das echte Spiel steht im Modul {@code game-runner}.
 */
public interface WorldView {

  /**
   * Die Zelle ist frei und ungefährlich für den Körper: keine Kollision, keine Flüssigkeit, nichts
   * Schädliches (Feuer, Kaktus, Lava und so weiter). Nicht geladene Zellen gelten als nicht frei.
   */
  boolean isFree(int x, int y, int z);

  /**
   * Der Spieler kann hier stehen: Füße und Kopf (die Zelle darüber) sind frei und der Block darunter
   * trägt ihn und ist ungefährlich.
   */
  boolean canStand(int x, int y, int z);
}
