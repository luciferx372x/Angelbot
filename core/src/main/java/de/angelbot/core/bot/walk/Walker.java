package de.angelbot.core.bot.walk;

import de.angelbot.core.bot.MovementHandle;
import java.util.List;

/**
 * Führt den Spieler Tick für Tick auf einem Weg zu einem Ziel.
 *
 * <p>{@link #begin} sucht den Weg ({@link PathFinder}), danach ruft der Bot einmal pro Tick {@link
 * #tick()} auf. Der Walker dreht den Spieler weich in Laufrichtung, drückt "vorwärts" und springt vor
 * einer Stufe. Bleibt der Spieler hängen, springt er einmal, danach wird der Weg neu gesucht (höchstens
 * {@link #MAX_REPLANS} Mal), sonst gilt das Laufen als gescheitert. Er läuft nie in Wasser: Der
 * Wegfinder kennt nur feste, ungefährliche Zellen.
 *
 * <p>Die Klasse kennt weder Minecraft noch LabyMod und lässt sich einzeln testen.
 */
public final class Walker {

  /** Ergebnis eines Ticks. */
  public enum Status {
    /** Der Spieler ist unterwegs. */
    WALKING,
    /** Der Spieler ist am Ziel. */
    ARRIVED,
    /** Es geht nicht weiter (kein Weg, hängengeblieben). */
    FAILED
  }

  /** Größte Drehung je Tick in Grad (15 Grad je Tick sind 300 Grad pro Sekunde). */
  static final float MAX_TURN = 15.0F;
  /** Größte Drehung je Tick beim Ausrichten am Ziel. */
  static final float ALIGN_TURN = 12.0F;
  static final float ALIGN_PITCH = 6.0F;
  /** Es wird erst gelaufen, wenn der Blick höchstens so weit von der Laufrichtung abweicht. */
  static final float FORWARD_TOLERANCE = 30.0F;
  /** Ein Zwischenziel gilt als erreicht, wenn der Spieler so nah an seiner Mitte ist (in Blöcken). */
  static final double REACH = 0.35D;
  static final double ARRIVE = 0.30D;
  /** So weit darf die Höhe vom Zwischenziel abweichen (Fall bis 2 Blöcke plus Spiel). */
  static final double MAX_VERTICAL = 2.6D;
  /** Ab so viel Fortschritt (Blöcke je Tick) gilt der Spieler nicht als hängengeblieben. */
  static final double MIN_PROGRESS = 0.02D;
  static final int STUCK_JUMP_TICKS = 12;
  static final int STUCK_REPLAN_TICKS = 40;
  static final int MAX_REPLANS = 2;
  static final int MAX_NODES = 6000;
  /** Weiter als so weit vom nächsten Zwischenziel entfernt wird der Weg neu gesucht. */
  static final double OFF_PATH_DISTANCE = 3.0D;

  private final MovementHandle move;
  private final WorldView world;
  private final Area area;

  private List<int[]> path;
  private int index;
  private double goalX;
  private double goalY;
  private double goalZ;
  private int stuckTicks;
  private int replans;
  private double lastX;
  private double lastZ;
  private boolean haveLast;
  private boolean lastForward;

  public Walker(MovementHandle move, WorldView world, Area area) {
    this.move = move;
    this.world = world;
    this.area = area;
  }

  /**
   * Sucht den Weg zum Ziel (Füße des Spielers).
   *
   * @return {@code false}, wenn es keinen Weg im erlaubten Bereich gibt
   */
  public boolean begin(double goalX, double goalY, double goalZ) {
    this.goalX = goalX;
    this.goalY = goalY;
    this.goalZ = goalZ;
    this.replans = 0;
    return this.plan();
  }

  private boolean plan() {
    int startX = (int) Math.floor(this.move.x());
    int startY = cellY(this.move.y());
    int startZ = (int) Math.floor(this.move.z());
    int goalCellX = (int) Math.floor(this.goalX);
    int goalCellY = cellY(this.goalY);
    int goalCellZ = (int) Math.floor(this.goalZ);

    List<int[]> found =
        PathFinder.find(
            this.world, this.area, startX, startY, startZ, goalCellX, goalCellY, goalCellZ, MAX_NODES);
    if (found == null) {
      this.path = null;
      return false;
    }

    this.path = found;
    this.index = found.size() > 1 ? 1 : 0;
    this.stuckTicks = 0;
    this.haveLast = false;
    this.lastForward = false;
    return true;
  }

  /** Länge des aktuellen Wegs in Zellen (0, wenn keiner gesucht wurde). */
  public int pathLength() {
    return this.path == null ? 0 : this.path.size();
  }

  /** Lässt alle Tasten los. Der Bot ruft das beim Abbrechen auf. */
  public void cancel() {
    this.move.setMovement(false, false);
    this.lastForward = false;
  }

  /** Muss einmal pro Spiel-Tick aufgerufen werden, solange der Spieler laufen soll. */
  public Status tick() {
    if (this.path == null) {
      return Status.FAILED;
    }

    double px = this.move.x();
    double py = this.move.y();
    double pz = this.move.z();

    if (this.haveLast) {
      double moved = Math.hypot(px - this.lastX, pz - this.lastZ);
      if (this.lastForward && moved < MIN_PROGRESS) {
        this.stuckTicks++;
      } else {
        this.stuckTicks = 0;
      }
    }
    this.lastX = px;
    this.lastZ = pz;
    this.haveLast = true;

    int last = this.path.size() - 1;
    double dx = 0.0D;
    double dz = 0.0D;
    double distance = 0.0D;
    while (true) {
      int[] cell = this.path.get(this.index);
      double targetX = this.index == last ? this.goalX : cell[0] + 0.5D;
      double targetZ = this.index == last ? this.goalZ : cell[2] + 0.5D;
      dx = targetX - px;
      dz = targetZ - pz;
      distance = Math.hypot(dx, dz);
      boolean verticalOk = Math.abs(py - cell[1]) < MAX_VERTICAL;

      if (this.index == last) {
        if (distance < ARRIVE && verticalOk) {
          this.cancel();
          return Status.ARRIVED;
        }
        break;
      }

      if (distance < REACH && verticalOk) {
        this.index++;
        continue;
      }
      break;
    }

    if (distance > OFF_PATH_DISTANCE) {
      // Vom Weg abgekommen (zum Beispiel weggeschubst): neu suchen.
      if (!this.replan()) {
        return Status.FAILED;
      }
      return Status.WALKING;
    }

    if (this.stuckTicks >= STUCK_REPLAN_TICKS) {
      if (!this.replan()) {
        return Status.FAILED;
      }
      return Status.WALKING;
    }

    float yaw = this.move.yaw();
    float turn = 0.0F;
    float remaining = 0.0F;
    if (distance > 0.05D) {
      float desired = (float) Math.toDegrees(Math.atan2(-dx, dz));
      float difference = wrap(desired - yaw);
      turn = Math.max(-MAX_TURN, Math.min(MAX_TURN, difference));
      remaining = Math.abs(difference - turn);
      this.move.setLook(wrap(yaw + turn), this.move.pitch());
    }

    boolean forward = remaining <= FORWARD_TOLERANCE;
    int[] cell = this.path.get(this.index);
    int feetY = cellY(py);
    boolean jump = this.move.isOnGround() && cell[1] > feetY && distance < 1.4D;
    if (this.stuckTicks == STUCK_JUMP_TICKS && this.move.isOnGround()) {
      jump = true;
    }

    this.move.setMovement(forward, jump);
    this.lastForward = forward;
    return Status.WALKING;
  }

  private boolean replan() {
    this.cancel();
    if (this.replans >= MAX_REPLANS) {
      return false;
    }
    this.replans++;
    return this.plan();
  }

  /**
   * Dreht den Spieler weich in die gewünschte Blickrichtung und lässt dabei alle Tasten los.
   *
   * @return {@code true}, sobald die Blickrichtung stimmt
   */
  public boolean align(float targetYaw, float targetPitch) {
    this.move.setMovement(false, false);
    float yawDifference = wrap(targetYaw - this.move.yaw());
    float pitchDifference = targetPitch - this.move.pitch();
    float yawStep = Math.max(-ALIGN_TURN, Math.min(ALIGN_TURN, yawDifference));
    float pitchStep = Math.max(-ALIGN_PITCH, Math.min(ALIGN_PITCH, pitchDifference));
    this.move.setLook(wrap(this.move.yaw() + yawStep), this.move.pitch() + pitchStep);
    return Math.abs(yawDifference - yawStep) < 1.0F && Math.abs(pitchDifference - pitchStep) < 1.0F;
  }

  /**
   * Die Zelle (Höhe), in der die Füße stehen. Es wird aufgerundet: Wer auf einem flachen Block wie
   * einem Trampelpfad (15/16 hoch) steht, hat die Füße knapp unter der Oberkante des Blocks und steht
   * trotzdem "auf" der Zelle darüber.
   */
  static int cellY(double y) {
    return (int) Math.ceil(y - 0.1D);
  }

  /** Bringt einen Winkel in den Bereich von -180 bis 180 Grad. */
  static float wrap(float angle) {
    float result = angle % 360.0F;
    if (result > 180.0F) {
      result -= 360.0F;
    } else if (result <= -180.0F) {
      result += 360.0F;
    }
    return result;
  }
}
