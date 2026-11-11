package de.angelbot.core;

import de.angelbot.core.bot.BotSettings;

/** Verbindet die Bot-Logik mit den Einstellungen. Die Werte werden bei jedem Zugriff frisch gelesen. */
final class ConfigBotSettings implements BotSettings {

  private final AngelbotAddon addon;

  /** Der zuletzt gelesene Mittelpunkt-Text und was daraus wurde, damit nicht jeden Tick neu gelesen wird. */
  private String centerText;
  private boolean centerValid;
  private double centerX;
  private double centerZ;

  ConfigBotSettings(AngelbotAddon addon) {
    this.addon = addon;
  }

  private AngelbotConfiguration config() {
    return this.addon.configuration();
  }

  @Override
  public int reactionMinMs() {
    return this.config().reactionMinMs().get();
  }

  @Override
  public int reactionMaxMs() {
    return this.config().reactionMaxMs().get();
  }

  @Override
  public int recastMinMs() {
    return this.config().recastMinMs().get();
  }

  @Override
  public int recastMaxMs() {
    return this.config().recastMaxMs().get();
  }

  @Override
  public int timeoutSeconds() {
    return this.config().timeoutSeconds().get();
  }

  @Override
  public int settleTicks() {
    return this.config().settleTicks().get();
  }

  @Override
  public double biteThreshold() {
    return this.config().biteThreshold().get() / 100.0D;
  }

  @Override
  public boolean pauseWhileScreenOpen() {
    return this.config().pauseWhileScreenOpen().get();
  }

  @Override
  public boolean stopWithoutRod() {
    return this.config().stopWithoutRod().get();
  }

  @Override
  public boolean debug() {
    return this.config().debug().get();
  }

  @Override
  public int minRodDurability() {
    return this.config().minRodDurability().get();
  }

  @Override
  public boolean switchRods() {
    return this.config().switchRods().get();
  }

  @Override
  public boolean autoEat() {
    return this.config().autoEat().get();
  }

  @Override
  public int eatBelow() {
    return this.config().eatBelow().get();
  }

  @Override
  public String foods() {
    return this.config().foods().get();
  }

  @Override
  public int zoneRadius() {
    return this.config().zoneRadius().get();
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
    return this.config().emptyMessages().get();
  }

  @Override
  public boolean walkToNextSpot() {
    return this.config().walkToNextSpot().get();
  }

  @Override
  public int spotCooldownMinutes() {
    return this.config().spotCooldownMinutes().get();
  }

  @Override
  public String spots() {
    return this.config().spots().get();
  }

  private void refreshCenter() {
    String text = this.config().zoneCenter().get();
    if (text == null) {
      text = "";
    }
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
