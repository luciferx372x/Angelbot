package de.angelbot.core.bot.walk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Eine gespeicherte Angelstelle: Position der Füße und Blickrichtung beim Speichern.
 *
 * <p>Gespeichert wird als Text: {@code x,y,z,yaw,pitch}, mehrere Stellen durch {@code ;} getrennt.
 */
public final class Spot {

  public final double x;
  public final double y;
  public final double z;
  public final float yaw;
  public final float pitch;

  public Spot(double x, double y, double z, float yaw, float pitch) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  /** Waagerechter Abstand zu einem Punkt. */
  public double distanceTo(double otherX, double otherZ) {
    return Math.hypot(this.x - otherX, this.z - otherZ);
  }

  public String format() {
    return String.format(
        Locale.ROOT, "%.2f,%.2f,%.2f,%.1f,%.1f", this.x, this.y, this.z, this.yaw, this.pitch);
  }

  /** Liest eine Stelle aus {@code x,y,z,yaw,pitch}. Ungültiges ergibt {@code null}. */
  public static Spot parse(String text) {
    if (text == null) {
      return null;
    }

    String[] parts = text.trim().split(",");
    if (parts.length != 5) {
      return null;
    }

    try {
      double x = Double.parseDouble(parts[0].trim());
      double y = Double.parseDouble(parts[1].trim());
      double z = Double.parseDouble(parts[2].trim());
      float yaw = Float.parseFloat(parts[3].trim());
      float pitch = Float.parseFloat(parts[4].trim());
      if (!isFinite(x) || !isFinite(y) || !isFinite(z) || !isFinite(yaw) || !isFinite(pitch)) {
        return null;
      }
      return new Spot(x, y, z, yaw, pitch);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  /** Liest eine Liste; ungültige Einträge werden übersprungen. */
  public static List<Spot> parseList(String text) {
    ArrayList<Spot> spots = new ArrayList<>();
    if (text == null || text.isBlank()) {
      return spots;
    }

    for (String part : text.split(";")) {
      Spot spot = parse(part);
      if (spot != null) {
        spots.add(spot);
      }
    }
    return spots;
  }

  public static String formatList(List<Spot> spots) {
    StringBuilder builder = new StringBuilder();
    for (Spot spot : spots) {
      if (builder.length() > 0) {
        builder.append(';');
      }
      builder.append(spot.format());
    }
    return builder.toString();
  }

  private static boolean isFinite(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value);
  }
}
