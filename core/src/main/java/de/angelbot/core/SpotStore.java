package de.angelbot.core;

import de.angelbot.core.bot.walk.Spot;
import java.util.List;
import java.util.Locale;
import net.labymod.api.configuration.exception.ConfigurationException;

/**
 * Verwaltet die gespeicherten Angelstellen und den Mittelpunkt des Bereichs. Beides liegt in der
 * Konfiguration des Addons und überlebt damit einen Neustart.
 */
public final class SpotStore {

  /** So viele Stellen lassen sich höchstens speichern. */
  public static final int MAX_SPOTS = 20;

  private final AngelbotAddon addon;

  public SpotStore(AngelbotAddon addon) {
    this.addon = addon;
  }

  private AngelbotConfiguration config() {
    return this.addon.configuration();
  }

  /** Die gespeicherten Stellen (eine Kopie, die sich frei ändern lässt). */
  public List<Spot> spots() {
    return Spot.parseList(this.config().spots().get());
  }

  /**
   * Fügt eine Stelle hinten an.
   *
   * @return die neue Nummer (ab 1) oder -1, wenn die Liste voll ist
   */
  public int add(Spot spot) {
    List<Spot> spots = this.spots();
    if (spots.size() >= MAX_SPOTS) {
      return -1;
    }

    spots.add(spot);
    this.config().spots().set(Spot.formatList(spots));
    this.save();
    return spots.size();
  }

  /** Entfernt die Stelle mit dieser Nummer (ab 1). */
  public boolean remove(int number) {
    List<Spot> spots = this.spots();
    if (number < 1 || number > spots.size()) {
      return false;
    }

    spots.remove(number - 1);
    this.config().spots().set(Spot.formatList(spots));
    this.save();
    return true;
  }

  public void clear() {
    this.config().spots().set("");
    this.save();
  }

  /** Mittelpunkt des Bereichs als {@code {x, z}}, oder {@code null}, wenn keiner gesetzt ist. */
  public double[] zoneCenter() {
    String text = this.config().zoneCenter().get();
    if (text == null) {
      return null;
    }

    String[] parts = text.split(",");
    if (parts.length != 2) {
      return null;
    }

    try {
      double x = Double.parseDouble(parts[0].trim());
      double z = Double.parseDouble(parts[1].trim());
      if (Double.isNaN(x) || Double.isNaN(z) || Double.isInfinite(x) || Double.isInfinite(z)) {
        return null;
      }
      return new double[] {x, z};
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  public void setZoneCenter(double x, double z) {
    this.config().zoneCenter().set(String.format(Locale.ROOT, "%.2f,%.2f", x, z));
    this.save();
  }

  public void clearZoneCenter() {
    this.config().zoneCenter().set("");
    this.save();
  }

  private void save() {
    try {
      this.addon.saveConfiguration();
    } catch (ConfigurationException exception) {
      this.addon.logger().error("Could not save the Angelbot settings", exception);
    }
  }
}
