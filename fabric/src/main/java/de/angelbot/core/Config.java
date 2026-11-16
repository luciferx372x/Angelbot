package de.angelbot.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Die Einstellungen des Angelbots. Sie liegen als JSON-Datei im Konfigurationsordner und lassen sich
 * im Spiel mit {@code /angelbot set <Name> <Wert>} ändern ({@code /angelbot config} zeigt alle).
 */
public final class Config {

  /**
   * Kartoffeln, dann Obst und Gemüse. Keine goldenen Äpfel, kein rohes Fleisch, nichts mit
   * Nebenwirkung.
   */
  static final String DEFAULT_FOODS =
      "cooked_beef,cooked_porkchop,cooked_mutton,cooked_chicken,cooked_salmon,cooked_cod,"
          + "cooked_rabbit,bread,baked_potato,pumpkin_pie,rabbit_stew,mushroom_stew,beetroot_soup,"
          + "golden_carrot,carrot,apple,melon_slice,sweet_berries,dried_kelp,cookie,beetroot";

  private enum Type {
    BOOL,
    INT,
    TEXT
  }

  private static final class Setting {
    final Type type;
    final int min;
    final int max;
    final Object defaultValue;
    Object value;

    Setting(Type type, Object defaultValue, int min, int max) {
      this.type = type;
      this.defaultValue = defaultValue;
      this.value = defaultValue;
      this.min = min;
      this.max = max;
    }
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private final Map<String, Setting> settings = new LinkedHashMap<>();
  private final Path file;

  public Config(Path file) {
    this.file = file;

    this.bool("enabled", true);
    this.number("reactionMinMs", 200, 50, 800);
    this.number("reactionMaxMs", 450, 50, 800);
    this.number("recastMinMs", 700, 300, 3000);
    this.number("recastMaxMs", 1400, 300, 3000);
    this.number("timeoutSeconds", 60, 15, 180);
    this.bool("pauseWhileScreenOpen", true);
    this.bool("stopWithoutRod", true);
    this.bool("switchRods", true);
    this.number("minRodDurability", 5, 0, 60);
    this.bool("autoEat", true);
    this.number("eatBelow", 14, 1, 19);
    this.text("foods", DEFAULT_FOODS);
    this.number("zoneRadius", 0, 0, 64);
    this.text("zoneCenter", "");
    this.text("emptyMessages", "kaum noch fische,überfischt,keine fische,no fish");
    this.bool("walkToNextSpot", false);
    this.number("spotCooldownMinutes", 10, 1, 60);
    this.text("spots", "");
    this.number("biteThreshold", 15, 5, 40);
    this.number("settleTicks", 20, 5, 100);
    this.bool("debug", false);

    this.load();
  }

  private void bool(String name, boolean value) {
    this.settings.put(name, new Setting(Type.BOOL, value, 0, 0));
  }

  private void number(String name, int value, int min, int max) {
    this.settings.put(name, new Setting(Type.INT, value, min, max));
  }

  private void text(String name, String value) {
    this.settings.put(name, new Setting(Type.TEXT, value, 0, 0));
  }

  public List<String> names() {
    return new ArrayList<>(this.settings.keySet());
  }

  public boolean getBool(String name) {
    return (Boolean) this.settings.get(name).value;
  }

  public int getInt(String name) {
    return (Integer) this.settings.get(name).value;
  }

  public String getText(String name) {
    return (String) this.settings.get(name).value;
  }

  /** Der Wert als Text, bei Zahlen mit dem erlaubten Bereich. */
  public String describe(String name) {
    Setting setting = this.settings.get(name);
    if (setting == null) {
      return "";
    }
    if (setting.type == Type.INT) {
      return setting.value + " (" + setting.min + " - " + setting.max + ")";
    }
    return String.valueOf(setting.value);
  }

  /** Setzt einen Text ohne Prüfung (für die gespeicherten Stellen und den Mittelpunkt). */
  void setText(String name, String value) {
    this.settings.get(name).value = value;
  }

  /**
   * Ändert eine Einstellung aus Befehlstext.
   *
   * @return {@code null} bei Erfolg, sonst {@code "unknown"} oder {@code "invalid"}
   */
  public String set(String name, String text) {
    Setting setting = this.settings.get(name);
    if (setting == null) {
      for (Map.Entry<String, Setting> entry : this.settings.entrySet()) {
        if (entry.getKey().equalsIgnoreCase(name)) {
          setting = entry.getValue();
          break;
        }
      }
    }
    if (setting == null) {
      return "unknown";
    }

    String trimmed = text.trim();
    switch (setting.type) {
      case BOOL -> {
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("true") || lower.equals("an") || lower.equals("on") || lower.equals("1")) {
          setting.value = true;
        } else if (lower.equals("false") || lower.equals("aus") || lower.equals("off") || lower.equals("0")) {
          setting.value = false;
        } else {
          return "invalid";
        }
      }
      case INT -> {
        try {
          int number = Integer.parseInt(trimmed);
          if (number < setting.min || number > setting.max) {
            return "invalid";
          }
          setting.value = number;
        } catch (NumberFormatException exception) {
          return "invalid";
        }
      }
      default -> setting.value = trimmed;
    }

    this.save();
    return null;
  }

  private void load() {
    if (!Files.isRegularFile(this.file)) {
      return;
    }

    try {
      String json = Files.readString(this.file, StandardCharsets.UTF_8);
      JsonObject object = GSON.fromJson(json, JsonObject.class);
      if (object == null) {
        return;
      }

      for (Map.Entry<String, Setting> entry : this.settings.entrySet()) {
        JsonElement element = object.get(entry.getKey());
        Setting setting = entry.getValue();
        if (element == null || !element.isJsonPrimitive()) {
          continue;
        }

        try {
          switch (setting.type) {
            case BOOL -> setting.value = element.getAsBoolean();
            case INT -> setting.value = Math.max(setting.min, Math.min(setting.max, element.getAsInt()));
            default -> setting.value = element.getAsString();
          }
        } catch (RuntimeException exception) {
          setting.value = setting.defaultValue;
        }
      }
    } catch (IOException | RuntimeException exception) {
      // Eine beschädigte Datei darf den Start nicht verhindern: es gelten dann die Standardwerte.
    }
  }

  public void save() {
    JsonObject object = new JsonObject();
    for (Map.Entry<String, Setting> entry : this.settings.entrySet()) {
      Object value = entry.getValue().value;
      if (value instanceof Boolean bool) {
        object.addProperty(entry.getKey(), bool);
      } else if (value instanceof Integer number) {
        object.addProperty(entry.getKey(), number);
      } else {
        object.addProperty(entry.getKey(), String.valueOf(value));
      }
    }

    try {
      Files.createDirectories(this.file.getParent());
      Files.writeString(this.file, GSON.toJson(object), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      // Speichern ist nicht kritisch; der Wert gilt bis zum Ende des Spiels.
    }
  }
}
