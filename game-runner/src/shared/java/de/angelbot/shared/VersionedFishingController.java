package de.angelbot.shared;

import de.angelbot.core.FishingController;
import javax.inject.Singleton;
import net.labymod.api.Laby;
import net.labymod.api.client.entity.LivingEntity.Hand;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.models.Implements;
import net.labymod.target.MinecraftTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Spielzugriff des Angelbots für Minecraft 26.1 bis 26.3.
 *
 * <p>Fast alles hier benutzt öffentliche Minecraft-Mitglieder, die in allen unterstützten Versionen
 * gleich heißen. Zwei Stellen unterscheiden sich: Den offenen Bildschirm liest {@link
 * #isScreenOpen()} je nach Version aus ({@link MinecraftTarget}), und die Armbewegung läuft über die
 * versionsunabhängige LabyMod-API, weil {@code swing} in 26.3 eine andere Signatur hat.
 */
@Singleton
@Implements(FishingController.class)
public class VersionedFishingController implements FishingController {

  @Override
  public boolean isInGame() {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft.player != null && minecraft.level != null && minecraft.gameMode != null;
  }

  @Override
  public boolean isScreenOpen() {
    Minecraft minecraft = Minecraft.getInstance();

    // Ab 26.2 liegt der offene Bildschirm in der Gui, davor direkt in Minecraft.
    if (MinecraftTarget.V26_2.orNewer()) {
      return minecraft.gui.screen() != null;
    }

    return minecraft.screen != null;
  }

  @Override
  public boolean hasRod() {
    return this.findRodHand(Minecraft.getInstance().player) != null;
  }

  @Override
  public boolean isCast() {
    return this.hook() != null;
  }

  @Override
  public boolean isBobberInWater() {
    FishingHook hook = this.hook();
    ClientLevel level = Minecraft.getInstance().level;
    if (hook == null || level == null) {
      return false;
    }

    // So entscheidet auch FishingHook selbst. Entity.isInWater() taugt hier nicht: Der Schwimmer
    // schaukelt mit der Unterkante an der Wasseroberfläche und gilt dort nur etwa jeden zweiten Tick
    // als "im Wasser".
    return level.getFluidState(hook.blockPosition()).is(FluidTags.WATER);
  }

  @Override
  public boolean isHookedToEntity() {
    FishingHook hook = this.hook();
    return hook != null && hook.getHookedIn() != null;
  }

  @Override
  public double bobberVerticalSpeed() {
    FishingHook hook = this.hook();
    return hook == null ? 0.0D : hook.getDeltaMovement().y;
  }

  @Override
  public boolean useRod() {
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.gameMode == null) {
      return false;
    }

    InteractionHand hand = this.findRodHand(player);
    if (hand == null) {
      return false;
    }

    // Dasselbe Paket wie ein echter Rechtsklick; der Server wirft aus oder holt ein.
    InteractionResult result = minecraft.gameMode.useItem(player, hand);
    if (result instanceof InteractionResult.Success) {
      ClientPlayer clientPlayer = Laby.labyAPI().minecraft().getClientPlayer();
      if (clientPlayer != null) {
        clientPlayer.swingArm(hand == InteractionHand.MAIN_HAND ? Hand.MAIN_HAND : Hand.OFF_HAND);
      }
    }

    return true;
  }

  private FishingHook hook() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? null : player.fishing;
  }

  private InteractionHand findRodHand(LocalPlayer player) {
    if (player == null) {
      return null;
    }
    if (isRod(player.getMainHandItem())) {
      return InteractionHand.MAIN_HAND;
    }
    if (isRod(player.getOffhandItem())) {
      return InteractionHand.OFF_HAND;
    }
    return null;
  }

  private static boolean isRod(ItemStack stack) {
    return stack.getItem() == Items.FISHING_ROD;
  }
}
