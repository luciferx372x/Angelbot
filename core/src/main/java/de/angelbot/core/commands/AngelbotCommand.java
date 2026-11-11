package de.angelbot.core.commands;

import de.angelbot.core.AngelbotAddon;
import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.StopReason;
import java.util.Locale;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.util.I18n;

/** Der Befehl {@code /angelbot [on|off|status]}. Ohne Argument schaltet er den Bot um. */
public class AngelbotCommand extends Command {

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
      default ->
          this.displayMessage(
              Component.translatable("angelbot.messages.usage").color(NamedTextColor.GRAY));
    }

    return true;
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
}
