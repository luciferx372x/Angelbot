package de.angelbot.core;

import de.angelbot.core.bot.BotSettings;

/** Verbindet die Bot-Logik mit den Einstellungen. Die Werte werden bei jedem Zugriff frisch gelesen. */
final class ConfigBotSettings implements BotSettings {

  private final AngelbotAddon addon;

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
}
