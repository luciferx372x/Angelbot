package de.angelbot.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.angelbot.core.AngelbotAddon;
import de.angelbot.core.Config;
import de.angelbot.core.commands.AngelbotCommand;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;


/** Einstiegspunkt des Angelbots als Fabric-Mod (Client). */
public final class AngelbotClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    Config config = new Config(FabricLoader.getInstance().getConfigDir().resolve("angelbot.json"));
    AngelbotAddon addon = new AngelbotAddon(new FabricFishingController(), config);
    AngelbotCommand command = new AngelbotCommand(addon);

    KeyMapping toggleKey =
        KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.angelbot.toggle", InputConstants.UNKNOWN.getValue(), Compat.keyCategory()));

    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (toggleKey.consumeClick()) {
            addon.toggleBot();
          }
          addon.onTick();
        });

    // Meldungen des Servers (Chat-Systemzeilen und Aktionsleiste); Spielerchat gehört nicht dazu.
    ClientReceiveMessageEvents.GAME.register(
        (message, overlay) -> addon.onServerMessage(message.getString()));

    SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> suggestions =
        (context, builder) -> {
          String input = context.getInput();
          int space = input.indexOf(' ');
          String rest = space < 0 ? "" : input.substring(space + 1);
          String[] arguments = rest.isEmpty() ? new String[0] : rest.split(" ", -1);
          List<String> options = command.complete(arguments);
          String last = rest.contains(" ") ? rest.substring(rest.lastIndexOf(' ') + 1) : rest;
          net.minecraft.commands.SharedSuggestionProvider.suggest(
              options,
              builder.createOffset(builder.getStart() + (input.length() - builder.getStart() - last.length())));
          return builder.buildFuture();
        };

    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommands.literal("angelbot")
                    .executes(
                        context -> {
                          command.execute("angelbot", new String[0]);
                          return 1;
                        })
                    .then(
                        ClientCommands.argument("arguments", StringArgumentType.greedyString())
                            .suggests(suggestions)
                            .executes(
                                context -> {
                                  String text = StringArgumentType.getString(context, "arguments").trim();
                                  command.execute("angelbot", text.isEmpty() ? new String[0] : text.split("\\s+"));
                                  return 1;
                                }))));
  }
}
