package de.angelbot.core;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;

/** Einstellungen des Angelbots. Sie erscheinen in den LabyMod-Einstellungen unter "Addons". */
@ConfigName("settings")
public class AngelbotConfiguration extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @SettingSection("general")
  @KeyBindSetting
  private final ConfigProperty<Key> toggleKey = new ConfigProperty<>(Key.NONE);

  @SettingSection("timing")
  @SliderSetting(min = 50, max = 800, steps = 10)
  private final ConfigProperty<Integer> reactionMinMs = new ConfigProperty<>(200);

  @SliderSetting(min = 50, max = 800, steps = 10)
  private final ConfigProperty<Integer> reactionMaxMs = new ConfigProperty<>(450);

  @SliderSetting(min = 300, max = 3000, steps = 50)
  private final ConfigProperty<Integer> recastMinMs = new ConfigProperty<>(700);

  @SliderSetting(min = 300, max = 3000, steps = 50)
  private final ConfigProperty<Integer> recastMaxMs = new ConfigProperty<>(1400);

  @SettingSection("safety")
  @SliderSetting(min = 15, max = 180, steps = 5)
  private final ConfigProperty<Integer> timeoutSeconds = new ConfigProperty<>(60);

  @SwitchSetting
  private final ConfigProperty<Boolean> pauseWhileScreenOpen = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> stopWithoutRod = new ConfigProperty<>(true);

  @SettingSection("detection")
  @SliderSetting(min = 5, max = 40, steps = 1)
  private final ConfigProperty<Integer> biteThreshold = new ConfigProperty<>(15);

  @SliderSetting(min = 5, max = 100, steps = 5)
  private final ConfigProperty<Integer> settleTicks = new ConfigProperty<>(20);

  @SwitchSetting
  private final ConfigProperty<Boolean> debug = new ConfigProperty<>(false);

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ConfigProperty<Key> toggleKey() {
    return this.toggleKey;
  }

  public ConfigProperty<Integer> reactionMinMs() {
    return this.reactionMinMs;
  }

  public ConfigProperty<Integer> reactionMaxMs() {
    return this.reactionMaxMs;
  }

  public ConfigProperty<Integer> recastMinMs() {
    return this.recastMinMs;
  }

  public ConfigProperty<Integer> recastMaxMs() {
    return this.recastMaxMs;
  }

  public ConfigProperty<Integer> timeoutSeconds() {
    return this.timeoutSeconds;
  }

  public ConfigProperty<Boolean> pauseWhileScreenOpen() {
    return this.pauseWhileScreenOpen;
  }

  public ConfigProperty<Boolean> stopWithoutRod() {
    return this.stopWithoutRod;
  }

  /** Bissschwelle in Hundertstel Blöcken pro Tick. */
  public ConfigProperty<Integer> biteThreshold() {
    return this.biteThreshold;
  }

  public ConfigProperty<Integer> settleTicks() {
    return this.settleTicks;
  }

  public ConfigProperty<Boolean> debug() {
    return this.debug;
  }
}
