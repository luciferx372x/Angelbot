package de.angelbot.core.bot;

import java.util.Random;

/**
 * Zustandsautomat des Angelbots. {@link #tick()} wird einmal pro Spiel-Tick (50 ms) aufgerufen.
 *
 * <p>Ablauf: auswerfen, auf den Biss warten, nach einer zufälligen Reaktionszeit einholen, nach einer
 * zufälligen Pause wieder auswerfen. Beißt nichts, wird nach der eingestellten Zeit neu ausgeworfen.
 * Landet der Schwimmer wiederholt nicht im Wasser oder reagiert der Server nicht, schaltet sich der
 * Bot ab, statt endlos weiterzumachen.
 *
 * <p>Die Klasse kennt weder Minecraft noch LabyMod (siehe {@link FishingHandle}) und lässt sich
 * deshalb einzeln testen.
 */
public final class FishingBot {

  /** Zustände des Bots. */
  public enum State {
    /** Der Bot ist aus. */
    OFF("off"),
    /** Wartet die zufällige Pause ab, danach wird ausgeworfen. */
    WAIT_CAST("waitCast"),
    /** Auswerfen wurde gesendet, der Schwimmer ist noch nicht da. */
    CAST_SENT("castSent"),
    /** Der Schwimmer schwimmt, es wird auf den Biss gewartet. */
    WAIT_BITE("waitBite"),
    /** Biss erkannt, die zufällige Reaktionszeit läuft. */
    REACT("react"),
    /** Einholen wurde gesendet, der Schwimmer ist noch nicht weg. */
    REEL_SENT("reelSent");

    private final String key;

    State(String key) {
      this.key = key;
    }

    /** Teil des Übersetzungsschlüssels ({@code angelbot.state.<key>}). */
    public String key() {
      return this.key;
    }
  }

  public static final int TICK_MS = 50;
  public static final int TICKS_PER_SECOND = 20;

  /** So lange darf es dauern, bis nach dem Auswerfen ein Schwimmer erscheint (3 s). */
  static final int CAST_CONFIRM_TICKS = 60;
  /** So lange darf es dauern, bis nach dem Einholen der Schwimmer verschwindet (3 s). */
  static final int REEL_CONFIRM_TICKS = 60;
  /** Ohne Angel in der Hand schaltet sich der Bot nach 10 s ab. */
  static final int NO_ROD_STOP_TICKS = 200;
  /** Berührt der Schwimmer 8 s nach dem Auswerfen noch nie Wasser, wird neu ausgeworfen. */
  static final int NOT_IN_WATER_TICKS = 160;
  /** Verlässt der Schwimmer das Wasser wieder und bleibt 3 s draußen, wird eingeholt. */
  static final int LEFT_WATER_TICKS = 60;
  /** Hängt der Haken schon so lange an etwas, wird eingeholt (0,5 s). */
  static final int HOOKED_REEL_TICKS = 10;
  /** So oft darf der Schwimmer nacheinander am Wasser vorbeigehen. */
  static final int MAX_MISSED_CASTS = 5;
  /** So oft darf der Server nacheinander nicht auf die Angel reagieren. */
  static final int MAX_NO_RESPONSE = 5;
  /** Pause, wenn die Angel gerade nicht benutzt werden konnte (1 s). */
  static final int RETRY_TICKS = 20;
  /** Ruht der Bot so lange wegen eines offenen Bildschirms, gibt es einen Hinweis (10 s). */
  static final int PAUSE_NOTICE_TICKS = 200;
  /** Kleinste erlaubte Bissschwelle, damit Rauschen nie als Biss zählt. */
  static final double MIN_THRESHOLD = 0.02D;

  private final FishingHandle game;
  private final BotSettings settings;
  private final BotEvents events;
  private final Random random;

  private State state = State.OFF;
  private boolean paused;
  private int delayTicks;
  private int stateTicks;
  /** Ticks seit dem ersten Wasserkontakt des Schwimmers, -1 solange er noch keins berührt hat. */
  private int waterContact = -1;
  /** Ticks am Stück, in denen der Schwimmer nach dem ersten Wasserkontakt nicht im Wasser war. */
  private int outOfWaterTicks;
  private int noRodTicks;
  private int pausedTicks;
  private int missedCasts;
  private int noResponse;
  private int debugCooldown;
  private int bites;
  private double biteSpeed;
  private long runTicks;

  public FishingBot(FishingHandle game, BotSettings settings, BotEvents events, Random random) {
    this.game = game;
    this.settings = settings;
    this.events = events;
    this.random = random;
  }

  public boolean isRunning() {
    return this.state != State.OFF;
  }

  /** Der Bot läuft, ruht aber gerade, weil ein Bildschirm offen ist. */
  public boolean isPaused() {
    return this.state != State.OFF && this.paused;
  }

  public State state() {
    return this.state;
  }

  /** Anzahl der erkannten Bisse seit dem Start. */
  public int bites() {
    return this.bites;
  }

  /** Laufzeit seit dem Start in Sekunden (ohne die Zeit mit offenem Bildschirm). */
  public int runSeconds() {
    return (int) (this.runTicks / TICKS_PER_SECOND);
  }

  /** Startet den Bot. Liegt der Schwimmer schon im Wasser, wird er übernommen. */
  public void start() {
    if (this.state != State.OFF) {
      return;
    }

    this.bites = 0;
    this.runTicks = 0L;
    this.noRodTicks = 0;
    this.pausedTicks = 0;
    this.missedCasts = 0;
    this.noResponse = 0;
    this.debugCooldown = 0;
    this.paused = false;
    this.beginWaitCast();
  }

  /** Beendet den Bot. Eine ausgeworfene Angel bleibt, wie sie ist. */
  public void stop(StopReason reason) {
    if (this.state == State.OFF) {
      return;
    }

    this.state = State.OFF;
    this.paused = false;
    this.events.stopped(reason, this.bites);
  }

  /** Muss einmal pro Spiel-Tick aufgerufen werden. */
  public void tick() {
    if (this.state == State.OFF) {
      return;
    }

    if (!this.game.isInGame()) {
      this.stop(StopReason.LEFT_GAME);
      return;
    }

    this.paused = this.settings.pauseWhileScreenOpen() && this.game.isScreenOpen();
    if (this.paused) {
      this.pausedTicks++;
      if (this.pausedTicks == PAUSE_NOTICE_TICKS) {
        this.events.paused();
      }
      return;
    }
    this.pausedTicks = 0;

    this.runTicks++;

    if (!this.game.hasRod()) {
      this.noRodTicks++;
      if (this.settings.stopWithoutRod() && this.noRodTicks >= NO_ROD_STOP_TICKS) {
        this.stop(StopReason.NO_ROD);
      }
      return;
    }
    this.noRodTicks = 0;

    switch (this.state) {
      case WAIT_CAST -> this.tickWaitCast();
      case CAST_SENT -> this.tickCastSent();
      case WAIT_BITE -> this.tickWaitBite();
      case REACT -> this.tickReact();
      case REEL_SENT -> this.tickReelSent();
      default -> {
      }
    }
  }

  private void tickWaitCast() {
    if (this.game.isCast()) {
      this.beginWaitBite();
      return;
    }

    this.delayTicks--;
    if (this.delayTicks > 0) {
      return;
    }

    if (this.game.useRod()) {
      this.state = State.CAST_SENT;
      this.stateTicks = 0;
    } else {
      this.delayTicks = RETRY_TICKS;
    }
  }

  private void tickCastSent() {
    if (this.game.isCast()) {
      this.noResponse = 0;
      this.beginWaitBite();
      return;
    }

    this.stateTicks++;
    if (this.stateTicks < CAST_CONFIRM_TICKS) {
      return;
    }

    this.noResponse++;
    if (this.noResponse >= MAX_NO_RESPONSE) {
      this.stop(StopReason.NO_RESPONSE);
    } else {
      this.beginWaitCast();
    }
  }

  private void tickWaitBite() {
    if (!this.game.isCast()) {
      this.beginWaitCast();
      return;
    }

    this.stateTicks++;
    this.trackWater();
    boolean settled = this.waterContact >= Math.max(1, this.settings.settleTicks());
    if (settled) {
      this.missedCasts = 0;
    }

    if (this.stateTicks >= HOOKED_REEL_TICKS && this.game.isHookedToEntity()) {
      this.reel(ReelReason.HOOKED);
      return;
    }

    if (settled) {
      double speed = this.game.bobberVerticalSpeed();
      double threshold = Math.max(MIN_THRESHOLD, this.settings.biteThreshold());
      if (speed <= -threshold) {
        this.onBite(speed);
        return;
      }
      if (this.settings.debug()) {
        this.debugNearMiss(speed, threshold);
      }
    }

    if (this.waterContact < 0 && this.stateTicks >= NOT_IN_WATER_TICKS) {
      this.missedCasts++;
      if (this.missedCasts >= MAX_MISSED_CASTS) {
        this.stop(StopReason.NO_WATER);
      } else {
        this.reel(ReelReason.NO_WATER);
      }
      return;
    }

    if (this.waterContact >= 0 && this.outOfWaterTicks >= LEFT_WATER_TICKS) {
      this.reel(ReelReason.LEFT_WATER);
      return;
    }

    if (this.stateTicks >= this.timeoutTicks()) {
      this.reel(ReelReason.TIMEOUT);
    }
  }

  /**
   * Hält fest, seit wann der Schwimmer im Wasser ist. Der Schwimmer schaukelt an der Oberfläche und
   * kann zwischendurch kurz als "nicht im Wasser" gemeldet werden. Darum zählt die Zeit seit dem
   * ersten Wasserkontakt und nicht die Zahl der Ticks am Stück.
   */
  private void trackWater() {
    if (this.game.isBobberInWater()) {
      this.outOfWaterTicks = 0;
      if (this.waterContact < 0) {
        this.waterContact = 0;
      }
    } else if (this.waterContact >= 0) {
      this.outOfWaterTicks++;
    }

    if (this.waterContact >= 0) {
      this.waterContact++;
    }
  }

  private void tickReact() {
    if (!this.game.isCast()) {
      this.beginWaitCast();
      return;
    }

    this.stateTicks++;
    this.delayTicks--;
    if (this.delayTicks <= 0) {
      this.reel(ReelReason.BITE);
    }
  }

  private void tickReelSent() {
    if (!this.game.isCast()) {
      this.noResponse = 0;
      this.beginWaitCast();
      return;
    }

    this.stateTicks++;
    if (this.stateTicks < REEL_CONFIRM_TICKS) {
      return;
    }

    this.noResponse++;
    if (this.noResponse >= MAX_NO_RESPONSE) {
      this.stop(StopReason.NO_RESPONSE);
      return;
    }

    this.stateTicks = 0;
    this.game.useRod();
  }

  private void onBite(double speed) {
    this.bites++;
    this.biteSpeed = speed;
    this.state = State.REACT;
    this.delayTicks = this.reactionTicks();
    if (this.settings.debug()) {
      this.events.debugBite(speed, this.delayTicks);
    }
  }

  private void reel(ReelReason reason) {
    if (this.game.useRod()) {
      this.events.reeled(reason, this.stateTicks, reason == ReelReason.BITE ? this.biteSpeed : 0.0D);
      this.state = State.REEL_SENT;
      this.stateTicks = 0;
    }
  }

  private void beginWaitCast() {
    this.state = State.WAIT_CAST;
    this.delayTicks = this.recastTicks();
    this.stateTicks = 0;
    this.waterContact = -1;
    this.outOfWaterTicks = 0;
  }

  private void beginWaitBite() {
    this.state = State.WAIT_BITE;
    this.stateTicks = 0;
    this.waterContact = -1;
    this.outOfWaterTicks = 0;
  }

  private void debugNearMiss(double speed, double threshold) {
    if (this.debugCooldown > 0) {
      this.debugCooldown--;
      return;
    }

    if (speed <= -threshold * 0.5D) {
      this.debugCooldown = 10;
      this.events.debugNearMiss(speed, threshold);
    }
  }

  private int timeoutTicks() {
    return Math.max(5, this.settings.timeoutSeconds()) * TICKS_PER_SECOND;
  }

  private int reactionTicks() {
    return this.randomTicks(this.settings.reactionMinMs(), this.settings.reactionMaxMs());
  }

  private int recastTicks() {
    return this.randomTicks(this.settings.recastMinMs(), this.settings.recastMaxMs());
  }

  /** Zufällige Zeit zwischen den beiden Grenzen (in beliebiger Reihenfolge), mindestens 1 Tick. */
  private int randomTicks(int firstMs, int secondMs) {
    int min = Math.max(0, Math.min(firstMs, secondMs));
    int max = Math.max(0, Math.max(firstMs, secondMs));
    int ms = max > min ? min + this.random.nextInt(max - min + 1) : min;
    return Math.max(1, (ms + TICK_MS - 1) / TICK_MS);
  }
}
