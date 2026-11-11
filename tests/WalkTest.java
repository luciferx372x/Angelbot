import de.angelbot.core.bot.MovementHandle;
import de.angelbot.core.bot.walk.Area;
import de.angelbot.core.bot.walk.PathFinder;
import de.angelbot.core.bot.walk.Spot;
import de.angelbot.core.bot.walk.Walker;
import de.angelbot.core.bot.walk.WorldView;
import java.util.List;

/**
 * Prüft Wegfinder, Laufsteuerung und Stellenliste gegen eine kleine simulierte Welt (ohne
 * Minecraft, ohne LabyMod).
 *
 * <p>Start (zusammen mit den anderen Tests):
 * <pre>
 *   javac -d tests/out core/src/main/java/de/angelbot/core/bot/*.java \
 *       core/src/main/java/de/angelbot/core/bot/walk/*.java tests/*.java
 *   java -cp tests/out WalkTest
 * </pre>
 */
public class WalkTest {

  private static int checks;
  private static int failures;

  static void check(String name, boolean ok) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FEHLER: " + name);
    }
  }

  /** Würfelwelt: Standardmäßig Luft, mit Boden aus Stein bei y = 0 (Füße stehen also auf y = 1). */
  static final class GridWorld implements WorldView {
    static final int SIZE = 48;
    static final int HEIGHT = 16;
    final boolean[][][] solid = new boolean[SIZE][HEIGHT][SIZE];
    final boolean[][][] hazard = new boolean[SIZE][HEIGHT][SIZE];

    GridWorld() {
      for (int x = 0; x < SIZE; x++) {
        for (int z = 0; z < SIZE; z++) {
          this.solid[x][0][z] = true;
        }
      }
    }

    boolean inBounds(int x, int y, int z) {
      return x >= 0 && x < SIZE && y >= 0 && y < HEIGHT && z >= 0 && z < SIZE;
    }

    boolean isSolid(int x, int y, int z) {
      return this.inBounds(x, y, z) && this.solid[x][y][z];
    }

    void set(int x, int y, int z, boolean value) {
      this.solid[x][y][z] = value;
    }

    /** Senkrechte Wand über dem Boden. */
    void wall(int x, int z, int height) {
      for (int y = 1; y <= height; y++) {
        this.solid[x][y][z] = true;
      }
    }

    void hole(int x, int z, int depth) {
      for (int y = 0; y > -depth && y >= 0; y--) {
        this.solid[x][y][z] = false;
      }
    }

    public boolean isFree(int x, int y, int z) {
      return this.inBounds(x, y, z) && !this.solid[x][y][z] && !this.hazard[x][y][z];
    }

    public boolean canStand(int x, int y, int z) {
      return this.isFree(x, y, z)
          && this.isFree(x, y + 1, z)
          && this.inBounds(x, y - 1, z)
          && this.solid[x][y - 1][z]
          && !this.hazard[x][y - 1][z];
    }
  }

  /** Spieler mit einfacher Physik: Gehen, Springen, Fallen, Kollision mit festen Blöcken. */
  static class PhysicsPlayer implements MovementHandle {
    final GridWorld world;
    double x;
    double y;
    double z;
    float yaw;
    float pitch;
    double vy;
    boolean onGround = true;
    boolean forward;
    boolean jump;
    float health = 20.0F;
    boolean inLiquid;
    /** Unsichtbare Wand: Die Welt meldet frei, die Physik blockiert (für den Test "hängt fest"). */
    int phantomX = -1;
    int phantomZ = -1;
    int jumps;
    int forwardTicks;

    PhysicsPlayer(GridWorld world, double x, double y, double z) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public boolean canMove() {
      return true;
    }

    public double x() {
      return this.x;
    }

    public double y() {
      return this.y;
    }

    public double z() {
      return this.z;
    }

    public float yaw() {
      return this.yaw;
    }

    public float pitch() {
      return this.pitch;
    }

    public void setLook(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
    }

    public void setMovement(boolean forward, boolean jump) {
      this.forward = forward;
      this.jump = jump;
    }

    public boolean isOnGround() {
      return this.onGround;
    }

    public float health() {
      return this.health;
    }

    public boolean isInLiquid() {
      return this.inLiquid;
    }

    public WorldView world() {
      return this.world;
    }

    /** Der Körper (0,6 breit, 1,8 hoch) mit Füßen bei dieser Position steckt in einem festen Block. */
    private boolean blockedAt(double px, double py, double pz) {
      for (int cornerX = -1; cornerX <= 1; cornerX += 2) {
        for (int cornerZ = -1; cornerZ <= 1; cornerZ += 2) {
          int cx = (int) Math.floor(px + 0.29D * cornerX);
          int cz = (int) Math.floor(pz + 0.29D * cornerZ);
          if (cx == this.phantomX && cz == this.phantomZ) {
            return true;
          }
          for (int level = 0; level < 3; level++) {
            int cy = (int) Math.floor(py + 0.05D + level * 0.85D);
            if (!this.world.inBounds(cx, cy, cz) || this.world.isSolid(cx, cy, cz)) {
              return true;
            }
          }
        }
      }
      return false;
    }

    /** Unter einer der vier Fußecken liegt in dieser Höhe ein fester Block. */
    private boolean solidAtCorners(int cellY) {
      for (int cornerX = -1; cornerX <= 1; cornerX += 2) {
        for (int cornerZ = -1; cornerZ <= 1; cornerZ += 2) {
          int cx = (int) Math.floor(this.x + 0.29D * cornerX);
          int cz = (int) Math.floor(this.z + 0.29D * cornerZ);
          if (this.world.isSolid(cx, cellY, cz)) {
            return true;
          }
        }
      }
      return false;
    }

    private boolean groundBelow() {
      return this.solidAtCorners((int) Math.floor(this.y - 0.05D));
    }

    /** Ein Spiel-Tick Physik. Die Reihenfolge ist wie in Minecraft: erst bewegen, dann bremsen. */
    void step() {
      if (this.jump && this.onGround) {
        this.vy = 0.42D;
        this.onGround = false;
        this.jumps++;
      }

      if (this.forward) {
        this.forwardTicks++;
        double radians = Math.toRadians(this.yaw);
        double dx = -Math.sin(radians) * 0.2158D;
        double dz = Math.cos(radians) * 0.2158D;
        if (!this.blockedAt(this.x + dx, this.y, this.z)) {
          this.x += dx;
        }
        if (!this.blockedAt(this.x, this.y, this.z + dz)) {
          this.z += dz;
        }
      }

      if (!this.onGround) {
        double used = this.vy;
        double newY = this.y + used;
        this.vy = (this.vy - 0.08D) * 0.98D;
        if (used > 0.0D) {
          if (this.blockedAt(this.x, newY, this.z)) {
            this.vy = 0.0D;
          } else {
            this.y = newY;
          }
        } else {
          int cellY = (int) Math.floor(newY - 0.001D);
          if (this.solidAtCorners(cellY)) {
            this.y = cellY + 1.0D;
            this.vy = 0.0D;
            this.onGround = true;
          } else {
            this.y = newY;
          }
        }
      } else if (!this.groundBelow()) {
        this.onGround = false;
        this.vy = 0.0D;
      }
    }
  }

  /** Läuft den Walker, bis er fertig ist oder die Tickgrenze erreicht ist. */
  static Walker.Status walk(PhysicsPlayer player, Walker walker, int maxTicks, int[] ticksUsed) {
    Walker.Status status = Walker.Status.WALKING;
    int ticks = 0;
    while (status == Walker.Status.WALKING && ticks < maxTicks) {
      status = walker.tick();
      player.step();
      ticks++;
    }
    ticksUsed[0] = ticks;
    return status;
  }

  static Area open() {
    return (x, z) -> true;
  }

  public static void main(String[] args) {
    pathFlat();
    pathAroundWall();
    pathSteps();
    pathDrops();
    pathZone();
    pathHazards();
    pathNodeLimit();
    walkerStraight();
    walkerAroundObstacle();
    walkerStepsAndDrops();
    walkerKeysReleased();
    walkerStuck();
    walkerNoPath();
    walkerOffPath();
    walkerAlign();
    spotParsing();

    System.out.println(checks + " Prüfungen, " + failures + " Fehler");
    if (failures > 0) {
      System.exit(1);
    }
  }

  // ---- Wegfinder ---------------------------------------------------------------------------------

  private static boolean contiguous(List<int[]> path) {
    for (int i = 1; i < path.size(); i++) {
      int[] a = path.get(i - 1);
      int[] b = path.get(i);
      int dx = Math.abs(a[0] - b[0]);
      int dz = Math.abs(a[2] - b[2]);
      int dy = b[1] - a[1];
      if (dx + dz != 1 || dy > 1 || dy < -PathFinder.MAX_DROP) {
        return false;
      }
    }
    return true;
  }

  private static void pathFlat() {
    GridWorld world = new GridWorld();
    List<int[]> path = PathFinder.find(world, open(), 5, 1, 5, 12, 1, 9, 5000);
    check("Weg: gefunden", path != null);
    check("Weg: Start und Ziel stimmen", path != null
        && path.get(0)[0] == 5 && path.get(0)[2] == 5
        && path.get(path.size() - 1)[0] == 12 && path.get(path.size() - 1)[2] == 9);
    check("Weg: kürzester Weg (Manhattan + 1), Länge " + (path == null ? -1 : path.size()),
        path != null && path.size() == 7 + 4 + 1);
    check("Weg: lückenlos", path != null && contiguous(path));

    List<int[]> same = PathFinder.find(world, open(), 5, 1, 5, 5, 1, 5, 100);
    check("Weg: Start gleich Ziel ergibt eine Zelle", same != null && same.size() == 1);
  }

  private static void pathAroundWall() {
    GridWorld world = new GridWorld();
    for (int z = 0; z < GridWorld.SIZE; z++) {
      world.wall(10, z, 3);
    }
    world.set(10, 1, 7, false);
    world.set(10, 2, 7, false);
    world.set(10, 3, 7, false);

    List<int[]> path = PathFinder.find(world, open(), 6, 1, 5, 14, 1, 5, 5000);
    check("Wand mit Lücke: Weg gefunden", path != null);
    boolean through = false;
    if (path != null) {
      for (int[] cell : path) {
        if (cell[0] == 10) {
          through = cell[2] == 7;
        }
      }
    }
    check("Wand mit Lücke: Weg geht durch die Lücke", through);

    world.wall(10, 7, 3);
    check("Geschlossene Wand: kein Weg",
        PathFinder.find(world, open(), 6, 1, 5, 14, 1, 5, 20000) == null);
  }

  private static void pathSteps() {
    GridWorld world = new GridWorld();
    world.set(8, 1, 5, true);
    List<int[]> up = PathFinder.find(world, open(), 6, 1, 5, 8, 2, 5, 5000);
    check("Stufe hinauf: Weg gefunden", up != null);
    check("Stufe hinauf: Höhe steigt um 1", up != null && up.get(up.size() - 1)[1] == 2 && contiguous(up));

    world.set(8, 2, 5, true);
    check("Zwei Blöcke hoch: Ziel oben nicht erreichbar",
        PathFinder.find(world, open(), 6, 1, 5, 8, 3, 5, 5000) == null);

    GridWorld low = new GridWorld();
    low.set(8, 1, 5, true);
    low.set(8, 2, 5, true);
    low.set(7, 1, 5, true); // Treppe: 1, 2
    List<int[]> stairs = PathFinder.find(low, open(), 5, 1, 5, 8, 3, 5, 5000);
    check("Treppe aus zwei Stufen: Weg gefunden", stairs != null && contiguous(stairs));

    GridWorld ceiling = new GridWorld();
    ceiling.set(8, 1, 5, true);
    ceiling.set(6, 3, 5, true); // Decke über den möglichen Absprungstellen: kein Platz zum Springen
    ceiling.set(7, 3, 5, true);
    check("Stufe mit Decke darüber: kein Sprung möglich",
        PathFinder.find(ceiling, (x, z) -> z == 5 && x >= 6 && x <= 8, 6, 1, 5, 8, 2, 5, 5000) == null);
    GridWorld freeAbove = new GridWorld();
    freeAbove.set(8, 1, 5, true);
    check("Stufe ohne Decke im selben Gang: Sprung möglich",
        PathFinder.find(freeAbove, (x, z) -> z == 5 && x >= 6 && x <= 8, 6, 1, 5, 8, 2, 5, 5000) != null);
  }

  private static void pathDrops() {
    GridWorld world = new GridWorld();
    for (int x = 6; x <= 20; x++) {
      for (int z = 0; z < GridWorld.SIZE; z++) {
        world.set(x, 1, z, true);
        world.set(x, 2, z, true);
      }
    }
    // Plateau 3 hoch (y = 3 begehbar), Boden bei y = 1: Abstieg von 2 Blöcken ist erlaubt
    List<int[]> down = PathFinder.find(world, open(), 10, 3, 5, 5, 1, 5, 20000);
    check("Abstieg um 2 Blöcke: Weg gefunden", down != null && contiguous(down));

    world.set(5, 1, 5, true);
    world.set(4, 1, 5, true); // jetzt ist der Boden direkt davor 3 Blöcke tiefer
    GridWorld deep = new GridWorld();
    for (int x = 6; x <= 20; x++) {
      for (int z = 0; z < GridWorld.SIZE; z++) {
        for (int y = 1; y <= 3; y++) {
          deep.set(x, y, z, true);
        }
      }
    }
    check("Abstieg um 3 Blöcke: nicht erlaubt",
        PathFinder.find(deep, (x, z) -> x >= 5 && x <= 20, 10, 4, 5, 5, 1, 5, 20000) == null);
  }

  private static void pathZone() {
    GridWorld world = new GridWorld();
    Area zone = Area.circle(10.0D, 10.0D, 6.0D);
    check("Bereich: Ziel außerhalb ergibt keinen Weg",
        PathFinder.find(world, zone, 10, 1, 10, 20, 1, 10, 5000) == null);
    check("Bereich: Ziel innerhalb ergibt einen Weg",
        PathFinder.find(world, zone, 8, 1, 10, 13, 1, 10, 5000) != null);

    // Ein Graben mitten im Bereich zwingt zum Umweg außerhalb des Kreises: dann gibt es keinen Weg.
    for (int z = 0; z < GridWorld.SIZE; z++) {
      world.hole(10, z, 1);
    }
    check("Bereich: Umweg müsste den Bereich verlassen",
        PathFinder.find(world, zone, 8, 1, 10, 13, 1, 10, 20000) == null);

    List<int[]> inside = PathFinder.find(new GridWorld(), zone, 7, 1, 10, 13, 1, 10, 5000);
    boolean allInside = inside != null;
    if (inside != null) {
      for (int[] cell : inside) {
        allInside &= zone.contains(cell[0], cell[2]);
      }
    }
    check("Bereich: alle Zellen des Wegs liegen im Kreis", allInside);
  }

  private static void pathHazards() {
    GridWorld world = new GridWorld();
    for (int z = 0; z < GridWorld.SIZE; z++) {
      world.hazard[10][1][z] = true; // Lava-Streifen auf Fußhöhe quer durch die ganze Welt
    }
    check("Gefahr: Streifen ohne Brücke ist nicht passierbar",
        PathFinder.find(world, open(), 6, 1, 5, 14, 1, 5, 20000) == null);

    world.hazard[10][1][7] = false;
    List<int[]> path = PathFinder.find(world, open(), 6, 1, 5, 14, 1, 5, 20000);
    check("Gefahr: Weg über die gefahrlose Stelle", path != null);

    GridWorld magma = new GridWorld();
    for (int x = 8; x <= 12; x++) {
      for (int z = 0; z < GridWorld.SIZE; z++) {
        magma.hazard[x][0][z] = true; // gefährlicher Boden quer durch die ganze Welt
      }
    }
    check("Gefahr: Boden darf nicht gefährlich sein",
        PathFinder.find(magma, open(), 6, 1, 5, 14, 1, 5, 20000) == null);
  }

  private static void pathNodeLimit() {
    GridWorld world = new GridWorld();
    check("Knotenlimit: zu wenig Knoten ergibt keinen Weg",
        PathFinder.find(world, open(), 2, 1, 2, 40, 1, 40, 50) == null);
    check("Knotenlimit: genug Knoten ergibt einen Weg",
        PathFinder.find(world, open(), 2, 1, 2, 40, 1, 40, 20000) != null);
  }

  // ---- Laufsteuerung -----------------------------------------------------------------------------

  private static void walkerStraight() {
    GridWorld world = new GridWorld();
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    check("Laufen: Weg gefunden", walker.begin(15.5D, 1.0D, 5.5D));
    int[] ticks = new int[1];
    Walker.Status status = walk(player, walker, 400, ticks);
    check("Laufen geradeaus: angekommen, Status " + status, status == Walker.Status.ARRIVED);
    check("Laufen geradeaus: Dauer plausibel (10 Blöcke), Ticks " + ticks[0],
        ticks[0] > 40 && ticks[0] < 110);
    check("Laufen geradeaus: am Ziel", Math.hypot(player.x - 15.5D, player.z - 5.5D) < 0.5D);
    check("Laufen geradeaus: Tasten losgelassen", !player.forward && !player.jump);
  }

  private static void walkerAroundObstacle() {
    GridWorld world = new GridWorld();
    for (int z = 0; z < 14; z++) {
      world.wall(10, z, 3);
    }
    PhysicsPlayer player = new PhysicsPlayer(world, 6.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    check("Hindernis: Weg gefunden", walker.begin(14.5D, 1.0D, 5.5D));
    int[] ticks = new int[1];
    Walker.Status status = walk(player, walker, 800, ticks);
    check("Hindernis: angekommen, Status " + status + ", Position " + player.x + "/" + player.z,
        status == Walker.Status.ARRIVED);
    check("Hindernis: am Ziel", Math.hypot(player.x - 14.5D, player.z - 5.5D) < 0.5D);
    check("Hindernis: wurde nicht geklettert (kein Sprung)", player.jumps == 0);
  }

  private static void walkerStepsAndDrops() {
    GridWorld world = new GridWorld();
    for (int x = 10; x <= 12; x++) {
      for (int z = 0; z < GridWorld.SIZE; z++) {
        world.set(x, 1, z, true);
      }
    }
    PhysicsPlayer player = new PhysicsPlayer(world, 6.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    check("Stufe: Weg gefunden", walker.begin(16.5D, 1.0D, 5.5D));
    int[] ticks = new int[1];
    Walker.Status status = walk(player, walker, 800, ticks);
    check("Stufe und Abstieg: angekommen, Status " + status + ", Position " + player.x + "/" + player.z + "/" + player.y,
        status == Walker.Status.ARRIVED);
    check("Stufe und Abstieg: mindestens ein Sprung", player.jumps >= 1);
    check("Stufe und Abstieg: unten angekommen, y " + player.y, Math.abs(player.y - 1.0D) < 0.2D);
    check("Stufe und Abstieg: am Ziel", Math.hypot(player.x - 16.5D, player.z - 5.5D) < 0.5D);
  }

  private static void walkerKeysReleased() {
    GridWorld world = new GridWorld();
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    walker.begin(15.5D, 1.0D, 5.5D);
    for (int i = 0; i < 20; i++) {
      walker.tick();
      player.step();
    }
    check("Abbruch: unterwegs ist die Vorwärtstaste gedrückt", player.forward);
    walker.cancel();
    check("Abbruch: cancel() lässt alle Tasten los", !player.forward && !player.jump);
  }

  private static void walkerStuck() {
    GridWorld world = new GridWorld();
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    player.phantomX = 9;
    player.phantomZ = 5;
    // Die Welt meldet 9/5 als frei, die Physik blockiert es, und es gibt nur diesen engen Gang.
    Area corridor = (x, z) -> z == 5 && x >= 4 && x <= 14;
    Walker walker = new Walker(player, world, corridor);
    check("Hängenbleiben: Weg wurde gefunden", walker.begin(13.5D, 1.0D, 5.5D));
    int[] ticks = new int[1];
    Walker.Status status = walk(player, walker, 1200, ticks);
    check("Hängenbleiben: Status FAILED, war " + status, status == Walker.Status.FAILED);
    check("Hängenbleiben: Tasten losgelassen", !player.forward && !player.jump);
    check("Hängenbleiben: es wurde einmal gesprungen", player.jumps >= 1);
    check("Hängenbleiben: gab auf, bevor die Zeit abgelaufen war, Ticks " + ticks[0], ticks[0] < 1200);
  }

  private static void walkerNoPath() {
    GridWorld world = new GridWorld();
    world.wall(14, 5, 3);
    world.wall(16, 5, 3);
    world.wall(15, 4, 3);
    world.wall(15, 6, 3);
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    check("Eingemauertes Ziel: kein Weg", !walker.begin(15.5D, 1.0D, 5.5D));
    check("Eingemauertes Ziel: tick() meldet FAILED", walker.tick() == Walker.Status.FAILED);
  }

  /** Wird der Spieler vom Weg geschubst, wird neu gesucht und das Ziel trotzdem erreicht. */
  private static void walkerOffPath() {
    GridWorld world = new GridWorld();
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    walker.begin(20.5D, 1.0D, 5.5D);
    int[] ticks = new int[1];
    for (int i = 0; i < 30; i++) {
      walker.tick();
      player.step();
    }
    player.x = 12.5D;
    player.z = 12.5D; // weggeschubst (7 Blöcke abseits)
    Walker.Status status = walk(player, walker, 800, ticks);
    check("Weggeschubst: trotzdem angekommen, Status " + status, status == Walker.Status.ARRIVED);
    check("Weggeschubst: am Ziel", Math.hypot(player.x - 20.5D, player.z - 5.5D) < 0.5D);
  }

  private static void walkerAlign() {
    GridWorld world = new GridWorld();
    PhysicsPlayer player = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    Walker walker = new Walker(player, world, open());
    player.yaw = -170.0F;
    player.pitch = 0.0F;
    int ticks = 0;
    while (!walker.align(170.0F, 30.0F) && ticks < 100) {
      ticks++;
    }
    check("Ausrichten: erreicht, Ticks " + ticks, ticks < 20);
    check("Ausrichten: Blickrichtung stimmt, yaw " + player.yaw,
        Math.abs(Math.abs(player.yaw) - 170.0F) < 1.5F);
    check("Ausrichten: Neigung stimmt, pitch " + player.pitch, Math.abs(player.pitch - 30.0F) < 1.5F);

    PhysicsPlayer second = new PhysicsPlayer(world, 5.5D, 1.0D, 5.5D);
    second.yaw = -170.0F;
    Walker walker2 = new Walker(second, world, open());
    walker2.align(170.0F, 0.0F);
    check("Ausrichten: dreht den kurzen Weg (über 180), yaw " + second.yaw,
        Math.abs(second.yaw) > 170.0F);
  }

  private static void spotParsing() {
    Spot spot = new Spot(12.5D, 64.0D, -7.25D, 135.5F, 20.0F);
    Spot back = Spot.parse(spot.format());
    check("Stelle: Text lässt sich wieder lesen", back != null
        && Math.abs(back.x - 12.5D) < 0.01D && Math.abs(back.z + 7.25D) < 0.01D
        && Math.abs(back.yaw - 135.5F) < 0.1F && Math.abs(back.pitch - 20.0F) < 0.1F);
    check("Stelle: Format ohne Komma-Probleme", spot.format().indexOf(' ') < 0 && spot.format().split(",").length == 5);

    List<Spot> list = Spot.parseList("1,2,3,4,5;kaputt;6,7,8,9,10;;1,2,3");
    check("Liste: ungültige Einträge werden übersprungen, waren " + list.size(), list.size() == 2);
    check("Liste: leerer Text ergibt eine leere Liste", Spot.parseList("").isEmpty()
        && Spot.parseList(null).isEmpty() && Spot.parseList("   ").isEmpty());
    check("Liste: formatList und parseList passen zusammen",
        Spot.parseList(Spot.formatList(list)).size() == 2);
    check("Stelle: NaN wird abgelehnt", Spot.parse("NaN,1,2,3,4") == null);
    check("Stelle: Abstand", Math.abs(spot.distanceTo(15.5D, -7.25D) - 3.0D) < 0.001D);
  }
}
