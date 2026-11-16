package de.angelbot.core.bot.walk;

/** Der Bereich, in dem der Bot laufen darf (Blockkoordinaten, von oben gesehen). */
@FunctionalInterface
public interface Area {

  /** Die Zelle mit diesen Blockkoordinaten liegt im erlaubten Bereich. */
  boolean contains(int x, int z);

  /** Kreis um einen Mittelpunkt. Maßgeblich ist die Mitte der Zelle. */
  static Area circle(double centerX, double centerZ, double radius) {
    double limit = radius * radius;
    return (x, z) -> {
      double dx = x + 0.5D - centerX;
      double dz = z + 0.5D - centerZ;
      return dx * dx + dz * dz <= limit;
    };
  }
}
