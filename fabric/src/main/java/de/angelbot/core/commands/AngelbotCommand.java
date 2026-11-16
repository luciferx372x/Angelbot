package de.angelbot.core.commands;

import de.angelbot.core.AngelbotAddon;
import de.angelbot.core.FishingController;
import de.angelbot.core.SpotStore;
import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.StopReason;
import de.angelbot.core.bot.walk.Spot;
import de.angelbot.core.bot.walk.WorldView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/**
 * Der Befehl {@code /angelbot [on|off|status|spot|zone]}. Ohne Argument schaltet er den Bot um.
 *
 * <p>{@code spot} verwaltet die Angelstellen ({@code add}, {@code list}, {@code remove <Nummer>},
 * {@code clear}), {@code zone} den Mittelpunkt des erlaubten Bereichs ({@code set}, {@code clear}).
 */
public class AngelbotCommand {

  private static final String[] MAIN_OPTIONS = {"on", "off", "status", "spot", "zone", "set", "config"};
  private static final String[] SPOT_OPTIONS = {"add", "list", "remove", "clear"};
  private static final String[] ZONE_OPTIONS = {"set", "clear"};

  private final AngelbotAddon addon;

  public AngelbotCommand(AngelbotAddon addon) {
    this.addon = addon;
  }

  public boolean execute(String prefix, String[] arguments) {
    String action = arguments.length == 0 ? "" : arguments[0].toLowerCase(Locale.ROOT);

    switch (action) {
      case "" -> this.addon.toggleBot();
      case "on", "an", "start" -> this.addon.startBot();
      case "off", "aus", "stop" -> this.stop();
      case "status" -> this.sendStatus();
      case "spot", "stelle" -> this.spot(arguments);
      case "zone", "bereich" -> this.zone(arguments);
      case "config", "einstellungen" -> this.showConfig();
      case "set", "setzen" -> this.setConfig(arguments);
      default -> this.info("angelbot.messages.usage");
    }

    return true;
  }

  public List<String> complete(String[] arguments) {
    if (arguments.length <= 1) {
      return matching(MAIN_OPTIONS, arguments.length == 0 ? "" : arguments[0]);
    }

    String first = arguments[0].toLowerCase(Locale.ROOT);
    if (first.equals("spot") || first.equals("stelle")) {
      return matching(SPOT_OPTIONS, arguments[1]);
    }
    if (first.equals("zone") || first.equals("bereich")) {
      return matching(ZONE_OPTIONS, arguments[1]);
    }
    if ((first.equals("set") || first.equals("setzen")) && arguments.length == 2) {
      return matching(this.addon.config().names(), arguments[1]);
    }
    return new ArrayList<>();
  }

  private static List<String> matching(List<String> options, String typed) {
    return matching(options.toArray(new String[0]), typed);
  }

  private static List<String> matching(String[] options, String typed) {
    String start = typed.toLowerCase(Locale.ROOT);
    ArrayList<String> result = new ArrayList<>();
    for (String option : options) {
      if (option.startsWith(start)) {
        result.add(option);
      }
    }
    return result;
  }

  private void stop() {
    if (this.addon.bot().isRunning()) {
      this.addon.stopBot(StopReason.MANUAL);
    } else {
      this.displayMessage(Component.translatable("angelbot.messages.statusOff"));
    }
  }

  private void sendStatus() {
    FishingBot bot = this.addon.bot();
    if (!bot.isRunning()) {
      this.displayMessage(Component.translatable("angelbot.messages.statusOff"));
      return;
    }

    String stateKey = bot.isPaused() ? "paused" : bot.state().key();
    String state = I18n.get("angelbot.state." + stateKey);
    int seconds = bot.runSeconds();
    String runtime = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.status",
            Component.literal(state),
            Component.literal(String.valueOf(bot.bites())),
            Component.literal(runtime)));
  }

  // ---- Angelstellen --------------------------------------------------------------------------------

  private void spot(String[] arguments) {
    String sub = arguments.length < 2 ? "list" : arguments[1].toLowerCase(Locale.ROOT);
    SpotStore store = this.addon.spotStore();

    switch (sub) {
      case "add", "neu", "speichern" -> this.addSpot(store);
      case "list", "liste" -> this.listSpots(store);
      case "remove", "delete", "entfernen" -> this.removeSpot(store, arguments);
      case "clear", "leeren" -> {
        store.clear();
        this.info("angelbot.messages.spot.cleared");
      }
      default -> this.info("angelbot.messages.spot.usage");
    }
  }

  private void addSpot(SpotStore store) {
    FishingController game = this.addon.controller();
    if (!game.isInGame()) {
      this.info("angelbot.messages.notInGame");
      return;
    }

    // In der Luft wäre die Höhe falsch und der Bot fände später keinen Weg dorthin.
    if (!game.isOnGround()) {
      this.info("angelbot.messages.spot.notOnGround");
      return;
    }

    Spot spot = new Spot(game.x(), game.y(), game.z(), game.yaw(), game.pitch());

    int radius = this.addon.settings().zoneRadius();
    double[] center = store.zoneCenter();
    if (radius > 0 && center != null && spot.distanceTo(center[0], center[1]) > radius) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.spot.outside", Component.literal(String.valueOf(radius)))
              .withStyle(ChatFormatting.GRAY));
      return;
    }

    int number = store.add(spot);
    if (number < 0) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.spot.full", Component.literal(String.valueOf(SpotStore.MAX_SPOTS)))
              .withStyle(ChatFormatting.GRAY));
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.spot.added",
            Component.literal(String.valueOf(number)),
            Component.literal(number(spot.x)),
            Component.literal(number(spot.y)),
            Component.literal(number(spot.z))));

    // Der Bot läuft nur zu Stellen, auf denen er sicher stehen kann. Eine Warnung hilft, das früh zu sehen.
    WorldView world = game.world();
    if (world != null
        && !world.canStand(
            (int) Math.floor(spot.x), (int) Math.ceil(spot.y - 0.1D), (int) Math.floor(spot.z))) {
      this.info("angelbot.messages.spot.notWalkable");
    }
  }

  private void listSpots(SpotStore store) {
    List<Spot> spots = store.spots();
    if (spots.isEmpty()) {
      this.info("angelbot.messages.spot.none");
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.spot.listHeader", Component.literal(String.valueOf(spots.size()))));
    for (int index = 0; index < spots.size(); index++) {
      Spot spot = spots.get(index);
      this.displayMessage(
          Component.literal(
                  (index + 1) + ": " + number(spot.x) + " / " + number(spot.y) + " / " + number(spot.z))
              .withStyle(ChatFormatting.GRAY));
    }
  }

  private void removeSpot(SpotStore store, String[] arguments) {
    int number = -1;
    if (arguments.length >= 3) {
      try {
        number = Integer.parseInt(arguments[2].trim());
      } catch (NumberFormatException exception) {
        number = -1;
      }
    }

    if (arguments.length < 3) {
      this.info("angelbot.messages.spot.usage");
    } else if (store.remove(number)) {
      this.displayMessage(
          Component.translatable(
              "angelbot.messages.spot.removed", Component.literal(String.valueOf(number))));
    } else {
      this.info("angelbot.messages.spot.notFound");
    }
  }

  // ---- Bereich -------------------------------------------------------------------------------------

  private void zone(String[] arguments) {
    String sub = arguments.length < 2 ? "show" : arguments[1].toLowerCase(Locale.ROOT);
    SpotStore store = this.addon.spotStore();

    switch (sub) {
      case "set", "setzen" -> this.setZone(store);
      case "clear", "leeren" -> {
        store.clearZoneCenter();
        this.info("angelbot.messages.zone.cleared");
      }
      case "show", "anzeigen" -> this.showZone(store);
      default -> this.info("angelbot.messages.zone.usage");
    }
  }

  private void setZone(SpotStore store) {
    FishingController game = this.addon.controller();
    if (!game.isInGame()) {
      this.info("angelbot.messages.notInGame");
      return;
    }

    store.setZoneCenter(game.x(), game.z());
    this.displayMessage(
        Component.translatable(
            "angelbot.messages.zone.set",
            Component.literal(number(game.x())),
            Component.literal(number(game.z()))));
    if (this.addon.settings().zoneRadius() <= 0) {
      this.info("angelbot.messages.zone.setNoRadius");
    }
  }

  private void showZone(SpotStore store) {
    int radius = this.addon.settings().zoneRadius();
    double[] center = store.zoneCenter();
    if (center == null) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.zone.showNone", Component.literal(String.valueOf(radius)))
              .withStyle(ChatFormatting.GRAY));
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.zone.show",
            Component.literal(number(center[0])),
            Component.literal(number(center[1])),
            Component.literal(String.valueOf(radius))));
  }

  private void info(String translationKey) {
    this.displayMessage(Component.translatable(translationKey).withStyle(ChatFormatting.GRAY));
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  // ---- Einstellungen -------------------------------------------------------------------------------

  private void showConfig() {
    this.info("angelbot.messages.config.header");
    for (String name : this.addon.config().names()) {
      this.displayMessage(
          Component.literal(name + " = " + this.addon.config().describe(name))
              .withStyle(ChatFormatting.GRAY));
    }
    this.info("angelbot.messages.config.usage");
  }

  private void setConfig(String[] arguments) {
    if (arguments.length < 3) {
      this.info("angelbot.messages.config.usage");
      return;
    }

    StringBuilder value = new StringBuilder();
    for (int index = 2; index < arguments.length; index++) {
      value.append(index > 2 ? " " : "").append(arguments[index]);
    }

    String error = this.addon.config().set(arguments[1], value.toString());
    if (error != null) {
      this.info("angelbot.messages.config." + error);
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.config.set",
            arguments[1],
            this.addon.config().describe(arguments[1])));
  }

  private void displayMessage(Component message) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null) {
      minecraft.player.sendSystemMessage(message);
    }
  }
}
