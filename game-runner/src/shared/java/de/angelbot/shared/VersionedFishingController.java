package de.angelbot.shared;

import de.angelbot.core.FishingController;
import de.angelbot.core.bot.walk.WorldView;
import java.util.Locale;
import javax.inject.Singleton;
import net.labymod.api.Laby;
import net.labymod.api.client.entity.LivingEntity.Hand;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.models.Implements;
import net.labymod.target.MinecraftTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Spielzugriff des Angelbots für Minecraft 26.1 bis 26.3.
 *
 * <p>Fast alles hier benutzt öffentliche Minecraft-Mitglieder, die in allen unterstützten Versionen
 * gleich heißen (das wurde für 26.1, 26.2 und 26.3 geprüft). Zwei Stellen unterscheiden sich: Den
 * offenen Bildschirm liest {@link #isScreenOpen()} je nach Version aus ({@link MinecraftTarget}), und
 * die Armbewegung läuft über die versionsunabhängige LabyMod-API, weil {@code swing} in 26.3 eine
 * andere Signatur hat.
 *
 * <p>Wichtig für Inventar und Tasten: Der Bot ruht, solange ein Bildschirm offen ist. Deshalb ist
 * das Inventarmenü des Spielers (Container 0) immer das aktive Menü, wenn hier geklickt wird.
 */
@Singleton
@Implements(FishingController.class)
public class VersionedFishingController implements FishingController {

  private static final int HOTBAR_SIZE = 9;
  /** Das Hauptinventar umfasst die Plätze 9 bis 35. */
  private static final int MAIN_INVENTORY_END = 36;

  /** Eine Angel mit höchstens so viel Haltbarkeit gilt als verbraucht (siehe equipSpareRod). */
  private int rodMin;

  private boolean eating;
  private int restoreSlot = -1;
  private int swapSlot = -1;
  private int swapHotbar = -1;

  private final MinecraftWorldView worldView = new MinecraftWorldView();

  // ---- Spiel und Angel --------------------------------------------------------------------------

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

  /**
   * Die Hand, mit der {@link #useRod()} angelt: bevorzugt eine Angel mit mehr als {@link #rodMin}
   * Haltbarkeit, sonst irgendeine Angel (die Haupthand zuerst). So wird auch eine ausgeworfene,
   * verbrauchte Angel noch eingeholt.
   */
  private InteractionHand findRodHand(LocalPlayer player) {
    if (player == null) {
      return null;
    }

    ItemStack main = player.getMainHandItem();
    ItemStack off = player.getOffhandItem();
    boolean mainRod = isRod(main);
    boolean offRod = isRod(off);
    if (mainRod && durabilityLeft(main) > this.rodMin) {
      return InteractionHand.MAIN_HAND;
    }
    if (offRod && durabilityLeft(off) > this.rodMin) {
      return InteractionHand.OFF_HAND;
    }
    if (mainRod) {
      return InteractionHand.MAIN_HAND;
    }
    if (offRod) {
      return InteractionHand.OFF_HAND;
    }
    return null;
  }

  private static boolean isRod(ItemStack stack) {
    return stack.getItem() == Items.FISHING_ROD;
  }

  /** Verbleibende Benutzungen, {@link Integer#MAX_VALUE} bei Gegenständen ohne Haltbarkeit. */
  private static int durabilityLeft(ItemStack stack) {
    if (!stack.isDamageableItem()) {
      return Integer.MAX_VALUE;
    }
    return stack.getMaxDamage() - stack.getDamageValue();
  }

  private boolean isUsableRod(ItemStack stack) {
    return isRod(stack) && durabilityLeft(stack) > this.rodMin;
  }

  // ---- Ersatzangel ---------------------------------------------------------------------------------

  @Override
  public int rodDurabilityLeft() {
    LocalPlayer player = Minecraft.getInstance().player;
    InteractionHand hand = this.findRodHand(player);
    if (hand == null) {
      return Integer.MAX_VALUE;
    }
    return durabilityLeft(hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem());
  }

  @Override
  public boolean equipSpareRod(int minLeft) {
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.gameMode == null) {
      return false;
    }

    this.rodMin = Math.max(0, minLeft);

    // Liegt schon eine brauchbare Angel in einer Hand, ist nichts zu tun.
    if (this.isUsableRod(player.getMainHandItem()) || this.isUsableRod(player.getOffhandItem())) {
      return true;
    }

    Inventory inventory = player.getInventory();

    // Erst die Hotbar: Es genügt, den Platz zu wählen.
    for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
      if (this.isUsableRod(inventory.getItem(slot))) {
        inventory.setSelectedSlot(slot);
        return true;
      }
    }

    // Dann das Inventar: Die Angel wird mit dem gewählten Hotbar-Platz getauscht. Die verbrauchte
    // Angel wandert dabei in das Inventar.
    for (int slot = HOTBAR_SIZE; slot < MAIN_INVENTORY_END; slot++) {
      if (this.isUsableRod(inventory.getItem(slot))) {
        minecraft.gameMode.handleContainerInput(
            player.inventoryMenu.containerId,
            slot,
            inventory.getSelectedSlot(),
            ContainerInput.SWAP,
            player);
        return true;
      }
    }

    return false;
  }

  // ---- Essen -------------------------------------------------------------------------------------------

  @Override
  public int foodLevel() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 20 : player.getFoodData().getFoodLevel();
  }

  @Override
  public boolean startEating(String foodIds) {
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || minecraft.gameMode == null || foodIds == null) {
      return false;
    }
    if (this.eating) {
      return true;
    }

    Inventory inventory = player.getInventory();
    for (String raw : foodIds.split(",")) {
      String name = normalizeItemName(raw);
      if (name.isEmpty()) {
        continue;
      }

      // Nebenhand: Hier muss nichts gewechselt werden.
      if (matchesItem(player.getOffhandItem(), name)) {
        return this.beginEating(minecraft, player, InteractionHand.OFF_HAND);
      }

      for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
        if (matchesItem(inventory.getItem(slot), name)) {
          this.restoreSlot = inventory.getSelectedSlot();
          inventory.setSelectedSlot(slot);
          return this.beginEating(minecraft, player, InteractionHand.MAIN_HAND);
        }
      }

      for (int slot = HOTBAR_SIZE; slot < MAIN_INVENTORY_END; slot++) {
        if (matchesItem(inventory.getItem(slot), name)) {
          this.swapSlot = slot;
          this.swapHotbar = inventory.getSelectedSlot();
          minecraft.gameMode.handleContainerInput(
              player.inventoryMenu.containerId,
              slot,
              this.swapHotbar,
              ContainerInput.SWAP,
              player);
          return this.beginEating(minecraft, player, InteractionHand.MAIN_HAND);
        }
      }
    }

    return false;
  }

  /**
   * Fängt an zu essen. Die Benutzen-Taste bleibt gedrückt, bis {@link #stopEating()} sie loslässt:
   * Minecraft bricht das Essen ab, sobald die Taste nicht mehr gedrückt ist.
   */
  private boolean beginEating(Minecraft minecraft, LocalPlayer player, InteractionHand hand) {
    this.eating = true;
    minecraft.options.keyUse.setDown(true);
    minecraft.gameMode.useItem(player, hand);
    return true;
  }

  @Override
  public boolean isEating() {
    LocalPlayer player = Minecraft.getInstance().player;
    return this.eating && player != null && player.isUsingItem();
  }

  @Override
  public void stopEating() {
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (!this.eating) {
      return;
    }

    this.eating = false;
    minecraft.options.keyUse.setDown(false);
    if (player == null || minecraft.gameMode == null) {
      this.restoreSlot = -1;
      this.swapSlot = -1;
      this.swapHotbar = -1;
      return;
    }

    if (player.isUsingItem()) {
      minecraft.gameMode.releaseUsingItem(player);
    }

    // Alles wieder so hinlegen, wie es war: erst den Tausch zurück, dann den gewählten Platz.
    if (this.swapSlot >= 0 && this.swapHotbar >= 0) {
      minecraft.gameMode.handleContainerInput(
          player.inventoryMenu.containerId,
          this.swapSlot,
          this.swapHotbar,
          ContainerInput.SWAP,
          player);
    }
    if (this.restoreSlot >= 0) {
      player.getInventory().setSelectedSlot(this.restoreSlot);
    }

    this.restoreSlot = -1;
    this.swapSlot = -1;
    this.swapHotbar = -1;
  }

  /** Kleinbuchstaben ohne Leerzeichen und ohne Namensraum ("minecraft:bread" wird "bread"). */
  private static String normalizeItemName(String raw) {
    String name = raw.trim().toLowerCase(Locale.ROOT);
    int colon = name.indexOf(':');
    return colon >= 0 ? name.substring(colon + 1) : name;
  }

  private static boolean matchesItem(ItemStack stack, String name) {
    return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals(name);
  }

  // ---- Position und Laufen ---------------------------------------------------------------------------

  @Override
  public boolean canMove() {
    return this.isInGame();
  }

  @Override
  public double x() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 0.0D : player.getX();
  }

  @Override
  public double y() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 0.0D : player.getY();
  }

  @Override
  public double z() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 0.0D : player.getZ();
  }

  @Override
  public float yaw() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 0.0F : player.getYRot();
  }

  @Override
  public float pitch() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 0.0F : player.getXRot();
  }

  @Override
  public void setLook(float yaw, float pitch) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }

    player.setYRot(yaw);
    player.setXRot(Math.max(-90.0F, Math.min(90.0F, pitch)));
  }

  @Override
  public void setMovement(boolean forward, boolean jump) {
    // Wie ein Tastendruck: Minecraft liest die Tasten im nächsten Tick und bewegt den Spieler.
    Minecraft minecraft = Minecraft.getInstance();
    minecraft.options.keyUp.setDown(forward);
    minecraft.options.keyJump.setDown(jump);
  }

  @Override
  public boolean isOnGround() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player != null && player.onGround();
  }

  @Override
  public float health() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player == null ? 20.0F : player.getHealth();
  }

  @Override
  public boolean isInLiquid() {
    LocalPlayer player = Minecraft.getInstance().player;
    return player != null && (player.isInWater() || player.isInLava());
  }

  @Override
  public WorldView world() {
    return this.worldView;
  }
}
