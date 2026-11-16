package de.angelbot.fabric;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

/** Stellen, die sich zwischen den Minecraft-Versionen unterscheiden (Fassung für 26.1 bis 26.1.2). */
final class Compat {

  private Compat() {}

  static boolean isScreenOpen(Minecraft minecraft) {
    return minecraft.screen != null;
  }

  static void swing(LocalPlayer player, InteractionHand hand) {
    player.swing(hand);
  }

  static KeyMapping.Category keyCategory() {
    return KeyMapping.Category.MISC;
  }
}
