package de.angelbot.shared;

import de.angelbot.core.bot.walk.WorldView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Die Welt aus Sicht des Wegfinders: Wo darf der Bot gehen und stehen?
 *
 * <p>Die Regeln sind bewusst streng. Lieber gibt es keinen Weg, als dass der Bot in Gefahr läuft:
 * <ul>
 *   <li>Eine Zelle ist nur dann frei, wenn sie geladen ist, keine Flüssigkeit enthält, keine Kollision
 *       hat und nichts Schädliches oder Besonderes ist (Feuer, Spinnennetz, Beerenstrauch,
 *       Pulverschnee, Portale, Leitern, Druckplatten und so weiter).
 *   <li>Als Boden zählen nur tragfähige Blöcke mit (fast) voller Höhe. Magma, Kaktus, Ackerboden,
 *       Honig, Schleim, Lagerfeuer und Eis sind ausgeschlossen.
 *   <li>Neben einer Standzelle darf weder Lava noch ein Kaktus liegen.
 * </ul>
 *
 * <p>Alle Abfragen laufen auf dem Haupt-Thread im Spiel-Tick.
 */
final class MinecraftWorldView implements WorldView {

  private static final int[] DX = {1, -1, 0, 0};
  private static final int[] DZ = {0, 0, 1, -1};

  @Override
  public boolean isFree(int x, int y, int z) {
    ClientLevel level = Minecraft.getInstance().level;
    return level != null && this.isFree(level, x, y, z);
  }

  @Override
  public boolean canStand(int x, int y, int z) {
    ClientLevel level = Minecraft.getInstance().level;
    if (level == null) {
      return false;
    }

    return this.isFree(level, x, y, z)
        && this.isFree(level, x, y + 1, z)
        && this.isSolidGround(level, x, y - 1, z)
        && !this.isHazardNearby(level, x, y, z);
  }

  private boolean isFree(ClientLevel level, int x, int y, int z) {
    BlockPos pos = new BlockPos(x, y, z);
    if (level.isOutsideBuildHeight(y) || !level.isLoaded(pos)) {
      return false;
    }

    BlockState state = level.getBlockState(pos);
    if (!state.getFluidState().isEmpty()) {
      return false;
    }
    if (!state.getCollisionShape(level, pos).isEmpty()) {
      return false;
    }
    return !isHarmful(state);
  }

  private boolean isSolidGround(ClientLevel level, int x, int y, int z) {
    BlockPos pos = new BlockPos(x, y, z);
    if (level.isOutsideBuildHeight(y) || !level.isLoaded(pos)) {
      return false;
    }

    BlockState state = level.getBlockState(pos);
    if (!state.getFluidState().isEmpty()) {
      return false;
    }

    VoxelShape shape = state.getCollisionShape(level, pos);
    if (shape.isEmpty()) {
      return false;
    }

    // Die Oberkante muss (fast) auf voller Höhe liegen. Trampelpfade sind 15/16 hoch.
    double top = shape.max(Direction.Axis.Y);
    if (top < 0.93D || top > 1.0001D) {
      return false;
    }

    if (!state.isFaceSturdy(level, pos, Direction.UP) && !state.is(Blocks.DIRT_PATH)) {
      return false;
    }

    return !isBadGround(state);
  }

  /** Neben der Standzelle liegt Lava oder ein Kaktus, oder die Nachbarzelle ist nicht geladen. */
  private boolean isHazardNearby(ClientLevel level, int x, int y, int z) {
    for (int direction = 0; direction < 4; direction++) {
      int nx = x + DX[direction];
      int nz = z + DZ[direction];
      if (!level.isLoaded(new BlockPos(nx, y, nz))) {
        return true;
      }

      for (int dy = -1; dy <= 1; dy++) {
        if (level.getFluidState(new BlockPos(nx, y + dy, nz)).is(FluidTags.LAVA)) {
          return true;
        }
      }

      if (level.getBlockState(new BlockPos(nx, y, nz)).is(Blocks.CACTUS)
          || level.getBlockState(new BlockPos(nx, y + 1, nz)).is(Blocks.CACTUS)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isHarmful(BlockState state) {
    return state.is(Blocks.FIRE)
        || state.is(Blocks.SOUL_FIRE)
        || state.is(Blocks.COBWEB)
        || state.is(Blocks.SWEET_BERRY_BUSH)
        || state.is(Blocks.POWDER_SNOW)
        || state.is(Blocks.WITHER_ROSE)
        || state.is(Blocks.NETHER_PORTAL)
        || state.is(Blocks.END_PORTAL)
        || state.is(Blocks.END_GATEWAY)
        || state.is(Blocks.BUBBLE_COLUMN)
        || state.is(Blocks.TRIPWIRE)
        || state.is(Blocks.LADDER)
        || state.is(Blocks.VINE)
        || state.is(BlockTags.FIRE)
        || state.is(BlockTags.CLIMBABLE)
        || state.is(BlockTags.PRESSURE_PLATES);
  }

  private static boolean isBadGround(BlockState state) {
    return state.is(Blocks.MAGMA_BLOCK)
        || state.is(Blocks.CACTUS)
        || state.is(Blocks.FARMLAND)
        || state.is(Blocks.HONEY_BLOCK)
        || state.is(Blocks.SLIME_BLOCK)
        || state.is(Blocks.CAMPFIRE)
        || state.is(Blocks.SOUL_CAMPFIRE)
        || state.is(Blocks.POINTED_DRIPSTONE)
        || state.is(BlockTags.ICE)
        || state.is(BlockTags.CAMPFIRES)
        || state.is(BlockTags.FIRE)
        || state.is(BlockTags.CAULDRONS);
  }
}
