import de.angelbot.core.bot.BotEvents;
import de.angelbot.core.bot.BotSettings;
import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.FishingHandle;
import de.angelbot.core.bot.ReelReason;
import de.angelbot.core.bot.StopReason;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Prüft die Bot-Logik gegen ein simuliertes Spiel (ohne Minecraft, ohne LabyMod).
 *
 * <p>Start:
 * <pre>
 *   javac -d tests/out core/src/main/java/de/angelbot/core/bot/*.java tests/FishingBotTest.java
 *   java -cp tests/out FishingBotTest
 * </pre>
 */
public class FishingBotTest {

  private static int checks;
  private static int failures;

  private static void check(String name, boolean ok) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FEHLER: " + name);
    }
  }

  /** Einstellungen mit den Standardwerten des Addons. */
  static class TestSettings implements BotSettings {
    int reactionMin = 200;
    int reactionMax = 450;
    int recastMin = 700;
    int recastMax = 1400;
    int timeout = 60;
    int settle = 40;
    double threshold = 0.15D;
    boolean pause = true;
    boolean stopNoRod = true;
    boolean debug = false;

    public int reactionMinMs() { return this.reactionMin; }
    public int reactionMaxMs() { return this.reactionMax; }
    public int recastMinMs() { return this.recastMin; }
    public int recastMaxMs() { return this.recastMax; }
    public int timeoutSeconds() { return this.timeout; }
    public int settleTicks() { return this.settle; }
    public double biteThreshold() { return this.threshold; }
    public boolean pauseWhileScreenOpen() { return this.pause; }
    public boolean stopWithoutRod() { return this.stopNoRod; }
    public boolean debug() { return this.debug; }
  }

  static class Recorder implements BotEvents {
    StopReason reason;
    int bites = -1;
    int stops;
    int pausedNotices;
    final List<String> debug = new ArrayList<>();
    final List<ReelReason> reelReasons = new ArrayList<>();
    final List<Double> biteSpeeds = new ArrayList<>();

    public void stopped(StopReason reason, int bites) {
      this.reason = reason;
      this.bites = bites;
      this.stops++;
    }

    public void paused() {
      this.pausedNotices++;
    }

    public void reeled(ReelReason reason, int waitTicks, double biteSpeed) {
      this.reelReasons.add(reason);
      if (reason == ReelReason.BITE) {
        this.biteSpeeds.add(biteSpeed);
      }
    }

    public void debugBite(double speed, int reactionTicks) {
      this.debug.add("bite " + speed + " " + reactionTicks);
    }

    public void debugNearMiss(double speed, double threshold) {
      this.debug.add("nearMiss " + speed + " " + threshold);
    }
  }

  /** Simuliertes Spiel mit Server-Verzögerung. */
  static class FakeGame implements FishingHandle {
    boolean inGame = true;
    boolean screenOpen;
    boolean rod = true;
    boolean hook;
    int hookAge;
    int latency = 2;
    int waterAfter = 5;      // Alter des Schwimmers, ab dem er im Wasser liegt (-1 = nie)
    int biteAtAge = -1;      // Alter, bei dem der Biss kommt (-1 = nie)
    int biteLength = 3;
    double biteSpeed = -0.3D;
    int hookedAtAge = -1;    // Alter, ab dem der Haken an etwas hängt (-1 = nie)
    boolean unresponsive;    // Server ignoriert die Angel
    double waterChance = 1.0D; // Wahrscheinlichkeit je Tick, dass der Schwimmer als "im Wasser" gilt
    int leaveWaterAtAge = -1;  // Alter, ab dem der Schwimmer das Wasser verlässt (-1 = nie)
    boolean inWaterNow;
    final Random flicker = new Random(99L);
    int tickNo;
    int pendingSpawn = -1;
    int pendingRemove = -1;
    int casts;
    int reels;
    int useCalls;
    final List<Integer> castTicks = new ArrayList<>();
    final List<Integer> reelTicks = new ArrayList<>();
    final List<Integer> removeTicks = new ArrayList<>();
    final List<Integer> biteTicks = new ArrayList<>();

    /** Einmal pro Tick vor bot.tick() aufrufen. */
    void advance() {
      this.tickNo++;
      if (this.pendingSpawn == this.tickNo) {
        this.hook = true;
        this.hookAge = 0;
        this.pendingSpawn = -1;
      } else if (this.hook) {
        this.hookAge++;
        if (this.hookAge == this.biteAtAge) {
          this.biteTicks.add(this.tickNo);
        }
      }
      if (this.pendingRemove == this.tickNo) {
        this.hook = false;
        this.pendingRemove = -1;
        this.removeTicks.add(this.tickNo);
      }
      this.inWaterNow = this.hook && this.waterAfter >= 0 && this.hookAge >= this.waterAfter
          && (this.leaveWaterAtAge < 0 || this.hookAge < this.leaveWaterAtAge)
          && (this.waterChance >= 1.0D || this.flicker.nextDouble() < this.waterChance);
    }

    public boolean isInGame() { return this.inGame; }
    public boolean isScreenOpen() { return this.screenOpen; }
    public boolean hasRod() { return this.rod; }
    public boolean isCast() { return this.hook; }

    public boolean isBobberInWater() {
      return this.inWaterNow;
    }

    public boolean isHookedToEntity() {
      return this.hook && this.hookedAtAge >= 0 && this.hookAge >= this.hookedAtAge;
    }

    public double bobberVerticalSpeed() {
      boolean biting = this.hook && this.biteAtAge >= 0 && this.hookAge >= this.biteAtAge
          && this.hookAge < this.biteAtAge + this.biteLength;
      return biting ? this.biteSpeed : 0.0D;
    }

    public boolean useRod() {
      if (!this.rod) {
        return false;
      }
      this.useCalls++;
      if (this.unresponsive) {
        return true;
      }
      if (!this.hook && this.pendingSpawn < 0) {
        this.casts++;
        this.castTicks.add(this.tickNo);
        this.pendingSpawn = this.tickNo + this.latency;
      } else if (this.hook && this.pendingRemove < 0) {
        this.reels++;
        this.reelTicks.add(this.tickNo);
        this.pendingRemove = this.tickNo + this.latency;
      }
      return true;
    }
  }

  private static void run(FakeGame game, FishingBot bot, int ticks) {
    for (int i = 0; i < ticks; i++) {
      game.advance();
      bot.tick();
    }
  }

  private static FishingBot newBot(FakeGame game, TestSettings settings, Recorder recorder) {
    return new FishingBot(game, settings, recorder, new Random(42L));
  }

  public static void main(String[] args) {
    basicCycle();
    swappedLimits();
    timeoutRecast();
    screenPause();
    noRodStops();
    noRodKeepsAlive();
    notInWater();
    flickeringWater();
    leavesWater();
    unresponsiveServer();
    leftGame();
    hookedEntity();
    adoptExistingHook();
    manualReel();
    fishEscapes();
    nearMissDebug();
    debugBiteLine();
    startStopContract();

    System.out.println(checks + " Prüfungen, " + failures + " Fehler");
    if (failures > 0) {
      System.exit(1);
    }
  }

  private static void basicCycle() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    TestSettings settings = new TestSettings();
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    check("Bot läuft nach start()", bot.isRunning());
    run(game, bot, 1500);

    check("Bot läuft weiter", bot.isRunning());
    check("mindestens 5 Bisse, waren " + bot.bites(), bot.bites() >= 5);
    check("Auswürfe und Einholen passen zusammen", Math.abs(game.casts - game.reels) <= 1);
    check("kein Einholen ohne Biss", game.reels <= bot.bites());
    check("Einholen nur wegen Bissen", allReasons(recorder.reelReasons, ReelReason.BITE));
    check("jedes Einholen wurde gemeldet", recorder.reelReasons.size() == game.reels);
    check("Biss-Geschwindigkeit wird gemeldet", !recorder.biteSpeeds.isEmpty()
        && recorder.biteSpeeds.get(0) == -0.3D);
    check("erster Auswurf nach 14 bis 28 Ticks, war " + game.castTicks.get(0),
        game.castTicks.get(0) >= 14 && game.castTicks.get(0) <= 28);

    int cycles = Math.min(game.biteTicks.size(), game.reelTicks.size());
    check("mindestens 5 Zyklen gemessen", cycles >= 5);
    for (int i = 0; i < cycles; i++) {
      int reaction = game.reelTicks.get(i) - game.biteTicks.get(i);
      check("Reaktion 4 bis 9 Ticks, war " + reaction, reaction >= 4 && reaction <= 9);
    }
    for (int i = 0; i + 1 < game.castTicks.size() && i < game.removeTicks.size(); i++) {
      int pause = game.castTicks.get(i + 1) - game.removeTicks.get(i);
      check("Pause 14 bis 28 Ticks, war " + pause, pause >= 14 && pause <= 28);
    }
  }

  private static void swappedLimits() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    TestSettings settings = new TestSettings();
    settings.reactionMin = 450;
    settings.reactionMax = 200;
    settings.recastMin = 1400;
    settings.recastMax = 700;
    FishingBot bot = newBot(game, settings, new Recorder());

    bot.start();
    run(game, bot, 800);
    check("vertauschte Grenzen: Bisse erkannt", bot.bites() >= 2);
    int cycles = Math.min(game.biteTicks.size(), game.reelTicks.size());
    for (int i = 0; i < cycles; i++) {
      int reaction = game.reelTicks.get(i) - game.biteTicks.get(i);
      check("vertauschte Grenzen: Reaktion 4 bis 9, war " + reaction,
          reaction >= 4 && reaction <= 9);
    }
  }

  private static void timeoutRecast() {
    FakeGame game = new FakeGame();
    TestSettings settings = new TestSettings();
    settings.timeout = 5;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 1000);
    check("Timeout: wurde mehrfach neu ausgeworfen, Einholen " + game.reels, game.reels >= 5);
    check("Timeout: kein Biss gezählt", bot.bites() == 0);
    check("Timeout: Grund ist TIMEOUT", allReasons(recorder.reelReasons, ReelReason.TIMEOUT));
    check("Timeout: Bot läuft weiter", bot.isRunning());
  }

  private static void screenPause() {
    FakeGame game = new FakeGame();
    TestSettings settings = new TestSettings();
    settings.timeout = 5;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 60);
    check("Pause: vorher wurde ausgeworfen", game.casts == 1);

    game.screenOpen = true;
    int before = game.useCalls;
    run(game, bot, 150);
    check("Pause: nach 7,5 s noch kein Hinweis", recorder.pausedNotices == 0);
    run(game, bot, 250);
    check("Pause: keine Aktion bei offenem Bildschirm", game.useCalls == before);
    check("Pause: Bot meldet Pause", bot.isPaused() && bot.isRunning());
    check("Pause: Hinweis nach 10 s genau einmal", recorder.pausedNotices == 1);

    game.screenOpen = false;
    run(game, bot, 600);
    check("Pause: danach geht es weiter", game.useCalls > before);
    check("Pause: keine Pause mehr gemeldet", !bot.isPaused());

    game.screenOpen = true;
    run(game, bot, 300);
    check("Pause: zweite Pause meldet erneut, Hinweise " + recorder.pausedNotices,
        recorder.pausedNotices == 2);
    game.screenOpen = false;
    run(game, bot, 50);
    game.screenOpen = true;
    run(game, bot, 100);
    game.screenOpen = false;
    run(game, bot, 50);
    check("Pause: eine 5-Sekunden-Pause meldet nichts", recorder.pausedNotices == 2);

    FakeGame game2 = new FakeGame();
    TestSettings settings2 = new TestSettings();
    settings2.pause = false;
    FishingBot bot2 = newBot(game2, settings2, new Recorder());
    bot2.start();
    game2.screenOpen = true;
    run(game2, bot2, 60);
    check("Pause aus: Bot arbeitet trotz offenem Bildschirm", game2.casts == 1);
  }

  private static void noRodStops() {
    FakeGame game = new FakeGame();
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 100);
    game.rod = false;
    run(game, bot, 150);
    check("Ohne Angel: nach 150 Ticks läuft der Bot noch", bot.isRunning());
    run(game, bot, 100);
    check("Ohne Angel: Bot hat sich abgeschaltet", !bot.isRunning());
    check("Ohne Angel: Grund NO_ROD", recorder.reason == StopReason.NO_ROD);
    check("Ohne Angel: genau eine Meldung", recorder.stops == 1);
  }

  private static void noRodKeepsAlive() {
    FakeGame game = new FakeGame();
    TestSettings settings = new TestSettings();
    settings.stopNoRod = false;
    settings.timeout = 5;
    FishingBot bot = newBot(game, settings, new Recorder());

    bot.start();
    run(game, bot, 60);
    game.rod = false;
    int calls = game.useCalls;
    run(game, bot, 600);
    check("Ohne Angel, Abschaltung aus: Bot läuft", bot.isRunning());
    check("Ohne Angel: keine Aktion", game.useCalls == calls);
    game.rod = true;
    run(game, bot, 600);
    check("Angel zurück: Bot arbeitet weiter", game.useCalls > calls);
  }

  private static void notInWater() {
    FakeGame game = new FakeGame();
    game.waterAfter = -1;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 3000);
    check("Kein Wasser: Bot hat sich abgeschaltet", !bot.isRunning());
    check("Kein Wasser: Grund NO_WATER", recorder.reason == StopReason.NO_WATER);
    check("Kein Wasser: 4 Mal eingeholt, war " + game.reels, game.reels == 4);
    check("Kein Wasser: 5 Mal ausgeworfen, war " + game.casts, game.casts == 5);
    check("Kein Wasser: Einholen wegen NO_WATER", allReasons(recorder.reelReasons, ReelReason.NO_WATER)
        && recorder.reelReasons.size() == 4);
  }

  /** Der Schwimmer schaukelt an der Oberfläche und gilt nur etwa jeden zweiten Tick als "im Wasser". */
  private static void flickeringWater() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 120;
    game.waterChance = 0.5D;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 1200);
    check("Schaukelnder Schwimmer: Bot läuft", bot.isRunning());
    check("Schaukelnder Schwimmer: Bisse erkannt, waren " + bot.bites(), bot.bites() >= 5);
    check("Schaukelnder Schwimmer: nie wegen fehlendem Wasser eingeholt",
        !recorder.reelReasons.contains(ReelReason.NO_WATER));
    check("Schaukelnder Schwimmer: nie wegen verlassenem Wasser eingeholt",
        !recorder.reelReasons.contains(ReelReason.LEFT_WATER));
    check("Schaukelnder Schwimmer: eingeholt wird nur beim Biss",
        allReasons(recorder.reelReasons, ReelReason.BITE));
  }

  /** Der Schwimmer berührt erst Wasser und verlässt es dann wieder (zum Beispiel Wasser weg). */
  private static void leavesWater() {
    FakeGame game = new FakeGame();
    game.leaveWaterAtAge = 60;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 400);
    check("Wasser verlassen: wurde eingeholt", game.reels >= 1);
    check("Wasser verlassen: Grund LEFT_WATER", !recorder.reelReasons.isEmpty()
        && recorder.reelReasons.get(0) == ReelReason.LEFT_WATER);
    if (!game.reelTicks.isEmpty() && !game.castTicks.isEmpty()) {
      int waited = game.reelTicks.get(0) - game.castTicks.get(0);
      check("Wasser verlassen: etwa 3 s nach dem Verlassen eingeholt, nach " + waited + " Ticks",
          waited >= 115 && waited <= 135);
    }
    check("Wasser verlassen: Bot läuft weiter und wirft neu aus", bot.isRunning() && game.casts >= 2);
  }

  private static boolean allReasons(List<ReelReason> reasons, ReelReason expected) {
    if (reasons.isEmpty()) {
      return false;
    }
    for (ReelReason reason : reasons) {
      if (reason != expected) {
        return false;
      }
    }
    return true;
  }

  private static void unresponsiveServer() {
    FakeGame game = new FakeGame();
    game.unresponsive = true;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 3000);
    check("Keine Antwort: Bot hat sich abgeschaltet", !bot.isRunning());
    check("Keine Antwort: Grund NO_RESPONSE", recorder.reason == StopReason.NO_RESPONSE);
    check("Keine Antwort: 5 Versuche, waren " + game.useCalls, game.useCalls == 5);
  }

  private static void leftGame() {
    FakeGame game = new FakeGame();
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 50);
    game.inGame = false;
    run(game, bot, 1);
    check("Welt verlassen: Bot aus", !bot.isRunning());
    check("Welt verlassen: Grund LEFT_GAME", recorder.reason == StopReason.LEFT_GAME);
  }

  private static void hookedEntity() {
    FakeGame game = new FakeGame();
    game.hookedAtAge = 20;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    bot.start();
    run(game, bot, 200);
    check("Haken hängt an etwas: wurde eingeholt", game.reels >= 1);
    check("Haken hängt an etwas: kein Biss gezählt", bot.bites() == 0);
    check("Haken hängt an etwas: Grund HOOKED", !recorder.reelReasons.isEmpty()
        && recorder.reelReasons.get(0) == ReelReason.HOOKED);
    check("Haken hängt an etwas: zügig eingeholt, erstes Einholen bei Tick "
        + (game.reelTicks.isEmpty() ? -1 : game.reelTicks.get(0)),
        !game.reelTicks.isEmpty() && game.reelTicks.get(0) - game.castTicks.get(0) < 40);
  }

  private static void adoptExistingHook() {
    FakeGame game = new FakeGame();
    game.hook = true;
    game.hookAge = 100;
    FishingBot bot = newBot(game, new TestSettings(), new Recorder());

    bot.start();
    run(game, bot, 30);
    check("Vorhandene Angel wird übernommen (kein neuer Auswurf)", game.casts == 0);
    check("Vorhandene Angel: Bot läuft", bot.isRunning());
    check("Vorhandene Angel: Zustand WAIT_BITE", bot.state() == FishingBot.State.WAIT_BITE);
  }

  private static void manualReel() {
    FakeGame game = new FakeGame();
    FishingBot bot = newBot(game, new TestSettings(), new Recorder());

    bot.start();
    run(game, bot, 100);
    check("Selbst eingeholt: vorher 1 Auswurf", game.casts == 1);
    game.hook = false;
    run(game, bot, 80);
    check("Selbst eingeholt: Bot wirft neu aus, Auswürfe " + game.casts, game.casts == 2);
    check("Selbst eingeholt: Bot hat nicht selbst eingeholt", game.reels == 0);
  }

  private static void fishEscapes() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    FishingBot bot = newBot(game, new TestSettings(), new Recorder());

    bot.start();
    int guard = 0;
    while (game.biteTicks.isEmpty() && guard++ < 400) {
      run(game, bot, 1);
    }
    check("Fisch entkommt: Biss kam", !game.biteTicks.isEmpty());
    game.hook = false;
    int castsBefore = game.casts;
    run(game, bot, 120);
    check("Fisch entkommt: kein Einholen ohne Schwimmer", game.reels == 0);
    check("Fisch entkommt: wirft neu aus", game.casts == castsBefore + 1);
  }

  private static void nearMissDebug() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    game.biteSpeed = -0.1D;
    TestSettings settings = new TestSettings();
    settings.debug = true;
    settings.timeout = 5;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 400);
    check("Debug: zu langsames Sinken zählt nicht als Biss", bot.bites() == 0);
    check("Debug: Hinweis wurde ausgegeben", !recorder.debug.isEmpty());

    FakeGame quiet = new FakeGame();
    quiet.biteAtAge = 100;
    quiet.biteSpeed = -0.1D;
    TestSettings quietSettings = new TestSettings();
    quietSettings.timeout = 5;
    Recorder quietRecorder = new Recorder();
    FishingBot quietBot = newBot(quiet, quietSettings, quietRecorder);
    quietBot.start();
    run(quiet, quietBot, 400);
    check("Debug aus: keine Ausgaben", quietRecorder.debug.isEmpty());
  }

  private static void debugBiteLine() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    TestSettings settings = new TestSettings();
    settings.debug = true;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 400);
    boolean hasBite = false;
    for (String line : recorder.debug) {
      hasBite = hasBite || line.startsWith("bite ");
    }
    check("Debug: Biss wird gemeldet", hasBite);
    check("Debug: Bot reagiert trotzdem normal", bot.bites() >= 1 && game.reels >= 1);
  }

  private static void startStopContract() {
    FakeGame game = new FakeGame();
    game.biteAtAge = 100;
    Recorder recorder = new Recorder();
    FishingBot bot = newBot(game, new TestSettings(), recorder);

    check("vor start(): aus", !bot.isRunning() && bot.state() == FishingBot.State.OFF);
    bot.stop(StopReason.MANUAL);
    check("stop() im Zustand aus meldet nichts", recorder.stops == 0);

    bot.start();
    run(game, bot, 200);
    int bitesBefore = bot.bites();
    FishingBot.State stateBefore = bot.state();
    bot.start();
    check("zweites start() ändert nichts", bot.state() == stateBefore && bot.bites() == bitesBefore);

    bot.stop(StopReason.MANUAL);
    bot.stop(StopReason.MANUAL);
    check("stop(): genau eine Meldung", recorder.stops == 1);
    check("stop(): Grund MANUAL", recorder.reason == StopReason.MANUAL);
    check("stop(): Bot aus", !bot.isRunning());
    int calls = game.useCalls;
    run(game, bot, 200);
    check("nach stop(): keine Aktionen mehr", game.useCalls == calls);

    bot.start();
    check("Neustart möglich", bot.isRunning() && bot.bites() == 0);
  }
}
