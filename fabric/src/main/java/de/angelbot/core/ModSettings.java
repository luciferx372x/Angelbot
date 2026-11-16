package de.angelbot.core;

import de.angelbot.core.bot.BotSettings;

/** Verbindet die Bot-Logik mit den Einstellungen. Die Werte werden bei jedem Zugriff frisch gelesen. */
public final class ModSettings implements BotSettings {

  private final Config config;

  /** Der zuletzt gelesene Mittelpunkt-Text und was daraus wurde, damit nicht jeden Tick neu gelesen wird. */
  private String centerText;
  private boolean centerValid;
  private double centerX;
  private double centerZ;

  public ModSettings(Config config) {
    this.config = config;
  }

  @Override
  public int reactionMinMs() {
    return this.config.getInt("reactionMinMs");
  }

  @Override
  public int reactionMaxMs() {
    return this.config.getInt("reactionMaxMs");
  }

  @Override
  public int recastMinMs() {
    return this.config.getInt("recastMinMs");
  }

  @Override
  public int recastMaxMs() {
    return this.config.getInt("recastMaxMs");
  }

  @Override
  public int timeoutSeconds() {
    return this.config.getInt("timeoutSeconds");
  }

  @Override
  public int settleTicks() {
    return this.config.getInt("settleTicks");
  }

  @Override
  public double biteThreshold() {
    return this.config.getInt("biteThreshold") / 100.0D;
  }

  @Override
  public boolean pauseWhileScreenOpen() {
    return this.config.getBool("pauseWhileScreenOpen");
  }

  @Override
  public boolean stopWithoutRod() {
    return this.config.getBool("stopWithoutRod");
  }

  @Override
  public boolean debug() {
    return this.config.getBool("debug");
  }

  @Override
  public int minRodDurability() {
    return this.config.getInt("minRodDurability");
  }

  @Override
  public boolean switchRods() {
    return this.config.getBool("switchRods");
  }

  @Override
  public boolean autoEat() {
    return this.config.getBool("autoEat");
  }

  @Override
  public int eatBelow() {
    return this.config.getInt("eatBelow");
  }

  @Override
  public String foods() {
    return this.config.getText("foods");
  }

  @Override
  public int zoneRadius() {
    return this.config.getInt("zoneRadius");
  }

  @Override
  public boolean hasZoneCenter() {
    this.refreshCenter();
    return this.centerValid;
  }

  @Override
  public double zoneCenterX() {
    this.refreshCenter();
    return this.centerX;
  }

  @Override
  public double zoneCenterZ() {
    this.refreshCenter();
    return this.centerZ;
  }

  @Override
  public String emptyMessages() {
    return this.config.getText("emptyMessages");
  }

  @Override
  public boolean walkToNextSpot() {
    return this.config.getBool("walkToNextSpot");
  }

  @Override
  public int spotCooldownMinutes() {
    return this.config.getInt("spotCooldownMinutes");
  }

  @Override
  public String spots() {
    return this.config.getText("spots");
  }

  private void refreshCenter() {
    String text = this.config.getText("zoneCenter");
    if (text.equals(this.centerText)) {
      return;
    }

    this.centerText = text;
    this.centerValid = false;
    String[] parts = text.split(",");
    if (parts.length != 2) {
      return;
    }

    try {
      double x = Double.parseDouble(parts[0].trim());
      double z = Double.parseDouble(parts[1].trim());
      if (!Double.isNaN(x) && !Double.isNaN(z) && !Double.isInfinite(x) && !Double.isInfinite(z)) {
        this.centerX = x;
        this.centerZ = z;
        this.centerValid = true;
      }
    } catch (NumberFormatException exception) {
      this.centerValid = false;
    }
  }
}
