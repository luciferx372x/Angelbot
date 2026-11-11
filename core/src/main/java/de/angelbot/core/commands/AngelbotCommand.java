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
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.util.I18n;

/**
 * Der Befehl {@code /angelbot [on|off|status|spot|zone]}. Ohne Argument schaltet er den Bot um.
 *
 * <p>{@code spot} verwaltet die Angelstellen ({@code add}, {@code list}, {@code remove <Nummer>},
 * {@code clear}), {@code zone} den Mittelpunkt des erlaubten Bereichs ({@code set}, {@code clear}).
 */
public class AngelbotCommand extends Command {

  private static final String[] MAIN_OPTIONS = {"on", "off", "status", "spot", "zone"};
  private static final String[] SPOT_OPTIONS = {"add", "list", "remove", "clear"};
  private static final String[] ZONE_OPTIONS = {"set", "clear"};

  private final AngelbotAddon addon;

  public AngelbotCommand(AngelbotAddon addon) {
    super("angelbot");
    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    String action = arguments.length == 0 ? "" : arguments[0].toLowerCase(Locale.ROOT);

    switch (action) {
      case "" -> this.addon.toggleBot();
      case "on", "an", "start" -> this.addon.startBot();
      case "off", "aus", "stop" -> this.stop();
      case "status" -> this.sendStatus();
      case "spot", "stelle" -> this.spot(arguments);
      case "zone", "bereich" -> this.zone(arguments);
      default -> this.info("angelbot.messages.usage");
    }

    return true;
  }

  @Override
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
    return new ArrayList<>();
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
    String state = I18n.translate("angelbot.state." + stateKey);
    int seconds = bot.runSeconds();
    String runtime = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.status",
            Component.text(state),
            Component.text(String.valueOf(bot.bites())),
            Component.text(runtime)));
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

    int radius = this.addon.configuration().zoneRadius().get();
    double[] center = store.zoneCenter();
    if (radius > 0 && center != null && spot.distanceTo(center[0], center[1]) > radius) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.spot.outside", Component.text(String.valueOf(radius)))
              .color(NamedTextColor.GRAY));
      return;
    }

    int number = store.add(spot);
    if (number < 0) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.spot.full", Component.text(String.valueOf(SpotStore.MAX_SPOTS)))
              .color(NamedTextColor.GRAY));
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.spot.added",
            Component.text(String.valueOf(number)),
            Component.text(number(spot.x)),
            Component.text(number(spot.y)),
            Component.text(number(spot.z))));

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
            "angelbot.messages.spot.listHeader", Component.text(String.valueOf(spots.size()))));
    for (int index = 0; index < spots.size(); index++) {
      Spot spot = spots.get(index);
      this.displayMessage(
          Component.text(
                  (index + 1) + ": " + number(spot.x) + " / " + number(spot.y) + " / " + number(spot.z))
              .color(NamedTextColor.GRAY));
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
              "angelbot.messages.spot.removed", Component.text(String.valueOf(number))));
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
            Component.text(number(game.x())),
            Component.text(number(game.z()))));
    if (this.addon.configuration().zoneRadius().get() <= 0) {
      this.info("angelbot.messages.zone.setNoRadius");
    }
  }

  private void showZone(SpotStore store) {
    int radius = this.addon.configuration().zoneRadius().get();
    double[] center = store.zoneCenter();
    if (center == null) {
      this.displayMessage(
          Component.translatable(
                  "angelbot.messages.zone.showNone", Component.text(String.valueOf(radius)))
              .color(NamedTextColor.GRAY));
      return;
    }

    this.displayMessage(
        Component.translatable(
            "angelbot.messages.zone.show",
            Component.text(number(center[0])),
            Component.text(number(center[1])),
            Component.text(String.valueOf(radius))));
  }

  private void info(String translationKey) {
    this.displayMessage(Component.translatable(translationKey).color(NamedTextColor.GRAY));
  }

  private static String number(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }
}
