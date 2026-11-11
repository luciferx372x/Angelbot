package de.angelbot.core;

import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget.KeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.annotation.SettingSection;

/** Einstellungen des Angelbots. Sie erscheinen in den LabyMod-Einstellungen unter "Addons". */
@ConfigName("settings")
public class AngelbotConfiguration extends AddonConfig {

  /**
   * Sichere, übliche Nahrung in der Reihenfolge der Vorliebe: erst Gebratenes, dann Brot und
   * Kartoffeln, dann Obst und Gemüse. Keine goldenen Äpfel, kein rohes Fleisch, nichts mit Nebenwirkung.
   */
  static final String DEFAULT_FOODS =
      "cooked_beef,cooked_porkchop,cooked_mutton,cooked_chicken,cooked_salmon,cooked_cod,"
          + "cooked_rabbit,bread,baked_potato,pumpkin_pie,rabbit_stew,mushroom_stew,beetroot_soup,"
          + "golden_carrot,carrot,apple,melon_slice,sweet_berries,dried_kelp,cookie,beetroot";

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

  @SettingSection("rod")
  @SwitchSetting
  private final ConfigProperty<Boolean> switchRods = new ConfigProperty<>(true);

  @SliderSetting(min = 0, max = 60, steps = 1)
  private final ConfigProperty<Integer> minRodDurability = new ConfigProperty<>(5);

  @SettingSection("food")
  @SwitchSetting
  private final ConfigProperty<Boolean> autoEat = new ConfigProperty<>(true);

  @SliderSetting(min = 1, max = 19, steps = 1)
  private final ConfigProperty<Integer> eatBelow = new ConfigProperty<>(14);

  @TextFieldSetting(maxLength = 500)
  private final ConfigProperty<String> foods = new ConfigProperty<>(DEFAULT_FOODS);

  @SettingSection("area")
  @SliderSetting(min = 0, max = 64, steps = 1)
  private final ConfigProperty<Integer> zoneRadius = new ConfigProperty<>(0);

  // Wird mit /angelbot zone set gesetzt. Das Textfeld zeigt den Wert und lässt ihn von Hand ändern.
  @TextFieldSetting(maxLength = 60)
  private final ConfigProperty<String> zoneCenter = new ConfigProperty<>("");

  @SettingSection("spots")
  @TextFieldSetting(maxLength = 200)
  private final ConfigProperty<String> emptyMessages = new ConfigProperty<>("keine fische,no fish");

  @SwitchSetting
  private final ConfigProperty<Boolean> walkToNextSpot = new ConfigProperty<>(false);

  @SliderSetting(min = 1, max = 60, steps = 1)
  private final ConfigProperty<Integer> spotCooldownMinutes = new ConfigProperty<>(10);

  // Werden mit /angelbot spot add gespeichert. Das Textfeld zeigt die Liste und lässt sie von Hand
  // ändern oder leeren: x,y,z,yaw,pitch, mehrere Stellen durch ; getrennt.
  @TextFieldSetting(maxLength = 1000)
  private final ConfigProperty<String> spots = new ConfigProperty<>("");

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

  public ConfigProperty<Boolean> switchRods() {
    return this.switchRods;
  }

  /** Eine Angel mit höchstens so vielen Benutzungen Rest gilt als verbraucht. */
  public ConfigProperty<Integer> minRodDurability() {
    return this.minRodDurability;
  }

  public ConfigProperty<Boolean> autoEat() {
    return this.autoEat;
  }

  /** Hungerwert (0 bis 20), bei dem oder darunter gegessen wird. */
  public ConfigProperty<Integer> eatBelow() {
    return this.eatBelow;
  }

  public ConfigProperty<String> foods() {
    return this.foods;
  }

  /** Radius des erlaubten Bereichs in Blöcken, 0 = keine Grenze. */
  public ConfigProperty<Integer> zoneRadius() {
    return this.zoneRadius;
  }

  public ConfigProperty<String> emptyMessages() {
    return this.emptyMessages;
  }

  public ConfigProperty<Boolean> walkToNextSpot() {
    return this.walkToNextSpot;
  }

  public ConfigProperty<Integer> spotCooldownMinutes() {
    return this.spotCooldownMinutes;
  }

  /** Mittelpunkt des Bereichs als {@code x,z}, leer = nicht gesetzt. */
  public ConfigProperty<String> zoneCenter() {
    return this.zoneCenter;
  }

  /** Gespeicherte Stellen, siehe {@code Spot}. */
  public ConfigProperty<String> spots() {
    return this.spots;
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
