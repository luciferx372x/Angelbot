package de.angelbot.fabric;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Stellen, die sich zwischen den Minecraft-Versionen unterscheiden (Fassung fÃ¼r 26.1 bis 26.1.2). */
final class Compat {

  private Compat() {}

  static boolean isScreenOpen(Minecraft minecraft) {
    return minecraft.gui.screen() != null;
  }

  static void swing(LocalPlayer player, InteractionHand hand) {
    player.swing(hand, player.getItemInHand(hand).getInteractAnimation(), true);
  }

  static KeyMapping.Category keyCategory() {
    return KeyMapping.Category.MISC;
  }
}

