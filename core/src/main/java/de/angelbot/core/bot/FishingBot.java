package de.angelbot.core.bot;

import de.angelbot.core.bot.walk.Area;
import de.angelbot.core.bot.walk.Spot;
import de.angelbot.core.bot.walk.Walker;
import de.angelbot.core.bot.walk.WorldView;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Zustandsautomat des Angelbots. {@link #tick()} wird einmal pro Spiel-Tick (50 ms) aufgerufen.
 *
 * <p>Ablauf: auswerfen, auf den Biss warten, nach einer zufälligen Reaktionszeit einholen, nach einer
 * zufälligen Pause wieder auswerfen. Beißt nichts, wird nach der eingestellten Zeit neu ausgeworfen.
 * Landet der Schwimmer wiederholt nicht im Wasser oder reagiert der Server nicht, schaltet sich der
 * Bot ab, statt endlos weiterzumachen.
 *
 * <p>Dazu kommen, jeweils einzeln einstellbar: eine Haltbarkeitsgrenze mit Wechsel auf eine
 * Ersatzangel, automatisches Essen, eine Bereichsgrenze und der Weg zur nächsten gespeicherten Stelle,
 * wenn der Server meldet, dass an der aktuellen nichts mehr zu fangen ist.
 *
 * <p>Die Klasse kennt weder Minecraft noch LabyMod (siehe {@link FishingHandle}, {@link
 * InventoryHandle} und {@link MovementHandle}) und lässt sich deshalb einzeln testen.
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
    REEL_SENT("reelSent"),
    /** Der Bot isst gerade. */
    EAT("eat"),
    /** Der Bot läuft zur nächsten Angelstelle. */
    WALK("walk");

    private final String key;

    State(String key) {
      this.key = key;
    }

    /** Teil des Übersetzungsschlüssels ({@code angelbot.state.<key>}). */
    public String key() {
      return this.key;
    }
  }

  private enum WalkPhase {
    /** Die Angel wird erst eingeholt. */
    REEL,
    /** Der Weg wird abgelaufen. */
    PATH,
    /** Der Blick wird in die gespeicherte Richtung gedreht. */
    ALIGN,
    /** Kurze Ruhe nach der Ankunft. */
    SETTLE
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

  /** Eine Angel, die zuletzt höchstens so viel Haltbarkeit hatte, ist vermutlich kaputtgegangen. */
  static final int MAX_BREAK_DAMAGE = 5;
  /** Essen dauert 1,6 s; länger als 4 s wird nicht gewartet. */
  static final int EAT_MAX_TICKS = 80;
  /** So lange darf das Essen nach dem Start noch nicht begonnen haben. */
  static final int EAT_START_GRACE_TICKS = 6;
  /** Nach dem Essen wird einen Moment nicht erneut gegessen. */
  static final int EAT_AFTER_TICKS = 40;
  /** Klappt das Essen nicht, wird erst nach 10 s wieder versucht. */
  static final int EAT_RETRY_TICKS = 200;
  /** Ist nichts zu essen da, wird erst nach einer Minute wieder gesucht. */
  static final int NO_FOOD_RETRY_TICKS = 1200;
  /** Dass nichts zu essen da ist, wird höchstens alle 10 Minuten gemeldet. */
  static final int NO_FOOD_NOTICE_TICKS = 12000;

  /** Länger als 90 s darf der Weg zur nächsten Stelle nicht dauern. */
  static final int MAX_WALK_TICKS = 1800;
  /** So lange wird versucht, die Angel vor dem Loslaufen einzuholen (3 s). */
  static final int WALK_REEL_MAX_TICKS = 60;
  static final int WALK_REEL_RETRY_TICKS = 20;
  /** Ruhe nach der Ankunft, bevor ausgeworfen wird (1 s). */
  static final int ARRIVE_SETTLE_TICKS = 20;
  /** So nah (in Blöcken) muss der Spieler an einer gespeicherten Stelle stehen, um dort zu sein. */
  static final double SPOT_MATCH_DISTANCE = 3.0D;
  /** Bei so wenig Leben (3 Herzen) wird nicht gelaufen. */
  static final float MIN_WALK_HEALTH = 6.0F;
  /** Nach der Ankunft werden Meldungen "Stelle leer" 10 s lang ignoriert (alte Aktionsleistentexte). */
  static final long EMPTY_IGNORE_TICKS = 200L;
  /** Eine Minute hat 1200 Ticks. */
  static final long TICKS_PER_MINUTE = 1200L;

  private static final InventoryHandle NO_INVENTORY = new InventoryHandle() {};
  private static final MovementHandle NO_MOVEMENT = new MovementHandle() {};

  private final FishingHandle game;
  private final InventoryHandle inventory;
  private final MovementHandle movement;
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

  private boolean hadRod;
  private int lastRodLeft = Integer.MAX_VALUE;

  private int eatTicks;
  private int eatCooldown;
  private int noFoodNotice;
  private boolean ateSomething;

  private boolean hasStart;
  private double startX;
  private double startZ;

  /** Wird vom Chat-Ereignis gesetzt, das auf einem anderen Thread laufen kann. */
  private volatile boolean pendingSpotEmpty;
  /** Bis zu diesem Tick werden Meldungen "Stelle leer" ignoriert (Meldungen halten sich lange). */
  private long ignoreEmptyUntil;
  private String spotsText;
  private List<Spot> spotList = new java.util.ArrayList<>();
  private long[] emptyUntil = new long[0];
  private Walker walker;
  private WalkPhase walkPhase = WalkPhase.REEL;
  private Spot targetSpot;
  private int targetIndex = -1;
  private int walkTicks;
  private int settleTicks;
  private float lastHealth;

  /** Für Spiele, die {@link InventoryHandle} und {@link MovementHandle} gleich mit umsetzen. */
  public FishingBot(FishingHandle game, BotSettings settings, BotEvents events, Random random) {
    this(
        game,
        game instanceof InventoryHandle inventoryHandle ? inventoryHandle : NO_INVENTORY,
        game instanceof MovementHandle movementHandle ? movementHandle : NO_MOVEMENT,
        settings,
        events,
        random);
  }

  public FishingBot(
      FishingHandle game,
      InventoryHandle inventory,
      MovementHandle movement,
      BotSettings settings,
      BotEvents events,
      Random random) {
    this.game = game;
    this.inventory = inventory;
    this.movement = movement;
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
    this.pendingSpotEmpty = false;
    this.ignoreEmptyUntil = 0L;
    this.eatTicks = 0;
    this.eatCooldown = 0;
    this.noFoodNotice = 0;
    this.ateSomething = false;
    this.walker = null;
    this.targetSpot = null;
    this.targetIndex = -1;
    this.spotsText = null;
    this.hadRod = this.game.hasRod();
    this.lastRodLeft = this.hadRod ? this.inventory.rodDurabilityLeft() : Integer.MAX_VALUE;
    this.hasStart = this.movement.canMove();
    if (this.hasStart) {
      this.startX = this.movement.x();
      this.startZ = this.movement.z();
    }
    this.beginWaitCast();
  }

  /** Beendet den Bot. Eine ausgeworfene Angel bleibt, wie sie ist. */
  public void stop(StopReason reason) {
    if (this.state == State.OFF) {
      return;
    }

    // Tasten und Essen immer freigeben, damit nichts hängen bleibt.
    if (this.state == State.WALK) {
      this.releaseMovement();
    } else if (this.state == State.EAT) {
      this.inventory.stopEating();
    }

    this.state = State.OFF;
    this.paused = false;
    this.events.stopped(reason, this.bites);
  }

  /**
   * Eine Chatmeldung ist eingetroffen. Enthält sie einen der eingestellten Textteile für "Stelle
   * leer", reagiert der Bot im nächsten Tick darauf.
   */
  public void onChatMessage(String message) {
    if (this.state == State.OFF || this.state == State.WALK || this.state == State.EAT) {
      return;
    }

    String patterns = this.settings.emptyMessages();
    if (message == null || patterns == null || patterns.isBlank()) {
      return;
    }

    String text = message.toLowerCase(Locale.ROOT);
    for (String pattern : patterns.split(",")) {
      String part = pattern.trim().toLowerCase(Locale.ROOT);
      if (!part.isEmpty() && text.contains(part)) {
        this.pendingSpotEmpty = true;
        return;
      }
    }
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
      this.onPaused();
      this.pausedTicks++;
      if (this.pausedTicks == PAUSE_NOTICE_TICKS) {
        this.events.paused();
      }
      return;
    }
    this.pausedTicks = 0;

    this.runTicks++;
    if (this.eatCooldown > 0) {
      this.eatCooldown--;
    }
    if (this.noFoodNotice > 0) {
      this.noFoodNotice--;
    }

    if (this.outOfZone()) {
      this.stop(StopReason.OUT_OF_ZONE);
      return;
    }

    if (this.pendingSpotEmpty) {
      this.pendingSpotEmpty = false;
      if (this.runTicks >= this.ignoreEmptyUntil) {
        this.handleSpotEmpty();
        if (this.state == State.OFF) {
          return;
        }
      }
    }

    if (this.state == State.WALK) {
      this.tickWalk();
      return;
    }

    if (this.state == State.EAT) {
      this.tickEat();
      return;
    }

    if (!this.checkRod()) {
      return;
    }

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

  /** Ein offener Bildschirm unterbricht Laufen und Essen, damit nichts hängen bleibt. */
  private void onPaused() {
    if (this.state == State.WALK) {
      this.releaseMovement();
    } else if (this.state == State.EAT) {
      this.inventory.stopEating();
      this.beginWaitCast();
    }
  }

  private void releaseMovement() {
    if (this.walker != null) {
      this.walker.cancel();
    } else {
      this.movement.setMovement(false, false);
    }
  }

  /**
   * Prüft, ob eine Angel in der Hand liegt.
   *
   * @return {@code false}, wenn in diesem Tick nichts weiter passieren darf
   */
  private boolean checkRod() {
    if (this.game.hasRod()) {
      this.hadRod = true;
      this.lastRodLeft = this.inventory.rodDurabilityLeft();
      this.noRodTicks = 0;
      return true;
    }

    // Die Angel war eben noch da, hatte kaum noch Haltbarkeit und ist jetzt weg: kaputtgegangen.
    boolean looksBroken = this.hadRod && this.lastRodLeft <= MAX_BREAK_DAMAGE;
    this.hadRod = false;
    if (looksBroken) {
      int min = Math.max(0, this.settings.minRodDurability());
      if (this.settings.switchRods() && this.inventory.equipSpareRod(min)) {
        this.hadRod = true;
        this.lastRodLeft = this.inventory.rodDurabilityLeft();
        this.noRodTicks = 0;
        this.events.rodSwitched(this.lastRodLeft);
        return true;
      }
      this.stop(StopReason.NO_SPARE_ROD);
      return false;
    }

    this.noRodTicks++;
    if (this.settings.stopWithoutRod() && this.noRodTicks >= NO_ROD_STOP_TICKS) {
      this.stop(StopReason.NO_ROD);
    }
    return false;
  }

  /**
   * Sorgt vor dem Auswerfen dafür, dass die Angel in der Hand noch benutzt werden darf: Hat sie nur
   * noch so viel Haltbarkeit wie die Grenze, wird gewechselt oder, ohne Ersatz, gestoppt.
   *
   * @return {@code false}, wenn der Bot gestoppt wurde
   */
  private boolean ensureUsableRod() {
    int min = Math.max(0, this.settings.minRodDurability());
    if (min <= 0 || this.inventory.rodDurabilityLeft() > min) {
      return true;
    }

    if (this.settings.switchRods() && this.inventory.equipSpareRod(min)) {
      this.lastRodLeft = this.inventory.rodDurabilityLeft();
      this.events.rodSwitched(this.lastRodLeft);
      return true;
    }

    this.stop(StopReason.NO_SPARE_ROD);
    return false;
  }

  /**
   * Beginnt zu essen, wenn der Bot Hunger hat. Nur zwischen zwei Würfen, wenn kein Schwimmer im
   * Wasser liegt.
   *
   * @return {@code true}, wenn der Bot jetzt isst
   */
  private boolean maybeStartEating() {
    if (!this.settings.autoEat() || this.eatCooldown > 0 || this.game.isCast()) {
      return false;
    }

    if (this.inventory.foodLevel() > this.settings.eatBelow()) {
      return false;
    }

    if (this.inventory.startEating(this.settings.foods())) {
      this.state = State.EAT;
      this.eatTicks = 0;
      this.ateSomething = false;
      return true;
    }

    this.eatCooldown = NO_FOOD_RETRY_TICKS;
    if (this.noFoodNotice <= 0) {
      this.noFoodNotice = NO_FOOD_NOTICE_TICKS;
      this.events.noFood();
    }
    return false;
  }

  private void tickEat() {
    this.eatTicks++;
    boolean eating = this.inventory.isEating();
    if (eating) {
      this.ateSomething = true;
    }

    boolean done =
        (!eating && this.eatTicks > EAT_START_GRACE_TICKS) || this.eatTicks > EAT_MAX_TICKS;
    if (!done) {
      return;
    }

    this.inventory.stopEating();
    this.eatCooldown = this.ateSomething ? EAT_AFTER_TICKS : EAT_RETRY_TICKS;
    if (this.ateSomething) {
      this.events.ate(this.inventory.foodLevel());
    }
    this.beginWaitCast();
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

    if (this.maybeStartEating()) {
      return;
    }

    if (!this.ensureUsableRod()) {
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

  // ---- Bereichsgrenze --------------------------------------------------------------------------

  /** Mittelpunkt des Bereichs: der gespeicherte oder, ohne den, die Stelle, an der der Bot startete. */
  private boolean zoneCenterKnown() {
    return this.settings.hasZoneCenter() || this.hasStart;
  }

  private double zoneCenterX() {
    return this.settings.hasZoneCenter() ? this.settings.zoneCenterX() : this.startX;
  }

  private double zoneCenterZ() {
    return this.settings.hasZoneCenter() ? this.settings.zoneCenterZ() : this.startZ;
  }

  /** Der Spieler ist weiter als der Radius (plus 1 Block Spiel) vom Mittelpunkt entfernt. */
  private boolean outOfZone() {
    int radius = this.settings.zoneRadius();
    if (radius <= 0 || !this.movement.canMove() || !this.zoneCenterKnown()) {
      return false;
    }

    double dx = this.movement.x() - this.zoneCenterX();
    double dz = this.movement.z() - this.zoneCenterZ();
    double limit = radius + 1.0D;
    return dx * dx + dz * dz > limit * limit;
  }

  // ---- Leere Stelle und Weg zur nächsten ---------------------------------------------------------

  private List<Spot> spots() {
    String text = this.settings.spots();
    if (text == null) {
      text = "";
    }

    if (!text.equals(this.spotsText)) {
      this.spotsText = text;
      this.spotList = Spot.parseList(text);
      this.emptyUntil = new long[this.spotList.size()];
    }
    return this.spotList;
  }

  private void handleSpotEmpty() {
    this.events.spotEmpty();

    int radius = this.settings.zoneRadius();
    List<Spot> spots = this.spots();
    boolean canWalk =
        this.settings.walkToNextSpot()
            && radius > 0
            && this.settings.hasZoneCenter()
            && this.movement.canMove()
            && this.movement.world() != null
            && spots.size() >= 2;
    if (!canWalk) {
      this.stop(StopReason.SPOT_EMPTY);
      return;
    }

    Area zone = Area.circle(this.settings.zoneCenterX(), this.settings.zoneCenterZ(), radius);
    int current = this.nearestSpot(spots, SPOT_MATCH_DISTANCE);
    if (current >= 0) {
      this.emptyUntil[current] =
          this.runTicks + Math.max(1, this.settings.spotCooldownMinutes()) * TICKS_PER_MINUTE;
    }

    int next = this.pickNextSpot(spots, current, zone);
    if (next < 0) {
      this.stop(StopReason.ALL_SPOTS_EMPTY);
      return;
    }

    WorldView world = this.movement.world();
    this.walker = new Walker(this.movement, world, zone);
    this.targetSpot = spots.get(next);
    this.targetIndex = next;
    this.state = State.WALK;
    this.walkPhase = WalkPhase.REEL;
    this.walkTicks = 0;
    this.lastHealth = this.movement.health();
    this.events.walkStarted(next + 1, spots.size());
  }

  /** Index der gespeicherten Stelle, an der der Spieler steht (höchstens {@code maxDistance}), sonst -1. */
  private int nearestSpot(List<Spot> spots, double maxDistance) {
    int best = -1;
    double bestDistance = maxDistance;
    for (int index = 0; index < spots.size(); index++) {
      double distance = spots.get(index).distanceTo(this.movement.x(), this.movement.z());
      if (distance <= bestDistance) {
        best = index;
        bestDistance = distance;
      }
    }
    return best;
  }

  /**
   * Wählt die nächste Stelle: der Reihe nach nach der aktuellen, sonst die nächstgelegene. Stellen,
   * die noch als leer gelten oder außerhalb des Bereichs liegen, werden übersprungen.
   */
  private int pickNextSpot(List<Spot> spots, int current, Area zone) {
    int count = spots.size();
    if (current >= 0) {
      for (int step = 1; step < count; step++) {
        int index = (current + step) % count;
        if (this.spotAvailable(spots.get(index), index, zone)) {
          return index;
        }
      }
      return -1;
    }

    int best = -1;
    double bestDistance = Double.MAX_VALUE;
    for (int index = 0; index < count; index++) {
      if (!this.spotAvailable(spots.get(index), index, zone)) {
        continue;
      }
      double distance = spots.get(index).distanceTo(this.movement.x(), this.movement.z());
      if (distance < bestDistance) {
        best = index;
        bestDistance = distance;
      }
    }
    return best;
  }

  private boolean spotAvailable(Spot spot, int index, Area zone) {
    return this.emptyUntil[index] <= this.runTicks
        && zone.contains((int) Math.floor(spot.x), (int) Math.floor(spot.z));
  }

  private void tickWalk() {
    this.walkTicks++;
    if (this.walkTicks > MAX_WALK_TICKS) {
      this.stop(StopReason.WALK_FAILED);
      return;
    }

    float health = this.movement.health();
    if (health < this.lastHealth - 0.01F
        || health <= MIN_WALK_HEALTH
        || this.movement.isInLiquid()) {
      this.stop(StopReason.WALK_DANGER);
      return;
    }
    this.lastHealth = health;

    switch (this.walkPhase) {
      case REEL -> {
        if (this.game.isCast() && this.walkTicks <= WALK_REEL_MAX_TICKS) {
          if (this.walkTicks % WALK_REEL_RETRY_TICKS == 1) {
            this.game.useRod();
          }
          return;
        }
        if (this.walker.begin(this.targetSpot.x, this.targetSpot.y, this.targetSpot.z)) {
          this.walkPhase = WalkPhase.PATH;
        } else {
          this.stop(StopReason.WALK_FAILED);
        }
      }
      case PATH -> {
        Walker.Status status = this.walker.tick();
        if (status == Walker.Status.ARRIVED) {
          this.walkPhase = WalkPhase.ALIGN;
        } else if (status == Walker.Status.FAILED) {
          this.stop(StopReason.WALK_FAILED);
        }
      }
      case ALIGN -> {
        if (this.walker.align(this.targetSpot.yaw, this.targetSpot.pitch)) {
          this.walkPhase = WalkPhase.SETTLE;
          this.settleTicks = 0;
        }
      }
      case SETTLE -> {
        this.settleTicks++;
        if (this.settleTicks >= ARRIVE_SETTLE_TICKS) {
          this.finishWalk();
        }
      }
    }
  }

  private void finishWalk() {
    this.walker = null;
    this.noResponse = 0;
    this.missedCasts = 0;
    this.ignoreEmptyUntil = this.runTicks + EMPTY_IGNORE_TICKS;
    this.beginWaitCast();
    this.events.walkArrived(this.targetIndex + 1);
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
