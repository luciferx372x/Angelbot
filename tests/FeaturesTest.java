import de.angelbot.core.bot.BotSettings;
import de.angelbot.core.bot.FishingBot;
import de.angelbot.core.bot.InventoryHandle;
import de.angelbot.core.bot.MovementHandle;
import de.angelbot.core.bot.StopReason;
import de.angelbot.core.bot.walk.Spot;
import de.angelbot.core.bot.walk.WorldView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Prüft die Erweiterungen des Bots gegen ein simuliertes Spiel: Haltbarkeitsgrenze mit Angelwechsel,
 * Essen, Bereichsgrenze, Erkennung der leeren Stelle und den Weg zur nächsten Stelle.
 *
 * <p>Start (zusammen mit den anderen Tests):
 * <pre>
 *   javac -d tests/out core/src/main/java/de/angelbot/core/bot/*.java \
 *       core/src/main/java/de/angelbot/core/bot/walk/*.java tests/*.java
 *   java -cp tests/out FeaturesTest
 * </pre>
 */
public class FeaturesTest {

  private static int checks;
  private static int failures;

  private static void check(String name, boolean ok) {
    checks++;
    if (!ok) {
      failures++;
      System.out.println("FEHLER: " + name);
    }
  }

  /** Einstellungen mit den Standardwerten des Addons plus den neuen Angaben. */
  static final class FeatSettings extends FishingBotTest.TestSettings {
    int minRod;
    boolean switchRods;
    boolean eat;
    int eatBelow = 14;
    String foods = "bread";
    int zoneRadius;
    boolean zoneCenterSet;
    double zoneX;
    double zoneZ;
    String empty = "";
    boolean walk;
    int cooldown = 10;
    String spots = "";

    public int minRodDurability() { return this.minRod; }
    public boolean switchRods() { return this.switchRods; }
    public boolean autoEat() { return this.eat; }
    public int eatBelow() { return this.eatBelow; }
    public String foods() { return this.foods; }
    public int zoneRadius() { return this.zoneRadius; }
    public boolean hasZoneCenter() { return this.zoneCenterSet; }
    public double zoneCenterX() { return this.zoneX; }
    public double zoneCenterZ() { return this.zoneZ; }
    public String emptyMessages() { return this.empty; }
    public boolean walkToNextSpot() { return this.walk; }
    public int spotCooldownMinutes() { return this.cooldown; }
    public String spots() { return this.spots; }
  }

  static final class FeatRecorder extends FishingBotTest.Recorder {
    int rodSwitches;
    int lastRodLeft = -1;
    int ate;
    int noFood;
    int spotEmpties;
    final List<Integer> walkStarts = new ArrayList<>();
    final List<Integer> walkArrivals = new ArrayList<>();

    public void rodSwitched(int durabilityLeft) {
      this.rodSwitches++;
      this.lastRodLeft = durabilityLeft;
    }

    public void ate(int foodLevel) { this.ate++; }
    public void noFood() { this.noFood++; }
    public void spotEmpty() { this.spotEmpties++; }
    public void walkStarted(int spotNumber, int spotCount) { this.walkStarts.add(spotNumber); }
    public void walkArrived(int spotNumber) { this.walkArrivals.add(spotNumber); }
  }

  /** Simuliertes Spiel mit Angel-Haltbarkeit, Hunger und einer Spielfigur, die laufen kann. */
  static final class FeatureGame extends FishingBotTest.FakeGame
      implements InventoryHandle, MovementHandle {
    int rodLeft = 64;
    int reelDamage = 1;
    final List<Integer> spares = new ArrayList<>();
    int equipCalls;
    boolean rodBroke;

    int food = 20;
    int foodItems;
    int foodGain = 6;
    int eatLeft;
    int eatStarts;
    int eatStopCalls;
    boolean castWhenEatStarted;
    boolean eatNeverStarts;

    final WalkTest.GridWorld world = new WalkTest.GridWorld();
    final WalkTest.PhysicsPlayer player = new WalkTest.PhysicsPlayer(this.world, 20.5D, 1.0D, 20.5D);

    @Override
    void advance() {
      super.advance();
      if (this.eatLeft > 0) {
        this.eatLeft--;
        if (this.eatLeft == 0) {
          this.food = Math.min(20, this.food + this.foodGain);
        }
      }
      this.player.step();
    }

    @Override
    public boolean useRod() {
      int before = this.reels;
      boolean ok = super.useRod();
      if (ok && this.reels > before) {
        this.rodLeft -= this.reelDamage;
        if (this.rodLeft <= 0) {
          this.rod = false;
          this.rodBroke = true;
        }
      }
      return ok;
    }

    public int rodDurabilityLeft() {
      return this.rod ? this.rodLeft : Integer.MAX_VALUE;
    }

    public boolean equipSpareRod(int minLeft) {
      this.equipCalls++;
      if (this.rod && this.rodLeft > minLeft) {
        return true;
      }
      for (int i = 0; i < this.spares.size(); i++) {
        if (this.spares.get(i) > minLeft) {
          this.rodLeft = this.spares.remove(i);
          this.rod = true;
          return true;
        }
      }
      return false;
    }

    public int foodLevel() { return this.food; }

    public boolean startEating(String foodIds) {
      if (this.foodItems <= 0 || foodIds == null || foodIds.isBlank()) {
        return false;
      }
      this.foodItems--;
      this.eatStarts++;
      this.castWhenEatStarted |= this.hook;
      this.eatLeft = this.eatNeverStarts ? 0 : 32;
      return true;
    }

    public boolean isEating() { return this.eatLeft > 0; }

    public void stopEating() {
      this.eatStopCalls++;
      this.eatLeft = 0;
    }

    public boolean canMove() { return true; }
    public double x() { return this.player.x(); }
    public double y() { return this.player.y(); }
    public double z() { return this.player.z(); }
    public float yaw() { return this.player.yaw(); }
    public float pitch() { return this.player.pitch(); }
    public void setLook(float yaw, float pitch) { this.player.setLook(yaw, pitch); }
    public void setMovement(boolean forward, boolean jump) { this.player.setMovement(forward, jump); }
    public boolean isOnGround() { return this.player.isOnGround(); }
    public float health() { return this.player.health(); }
    public boolean isInLiquid() { return this.player.isInLiquid(); }
    public WorldView world() { return this.world; }
  }

  private static void run(FeatureGame game, FishingBot bot, int ticks) {
    for (int i = 0; i < ticks; i++) {
      game.advance();
      bot.tick();
    }
  }

  /** Läuft, bis der Bot aus ist oder die Grenze erreicht ist. Liefert die benutzten Ticks. */
  private static int runUntilStopped(FeatureGame game, FishingBot bot, int maxTicks) {
    int ticks = 0;
    while (bot.isRunning() && ticks < maxTicks) {
      game.advance();
      bot.tick();
      ticks++;
    }
    return ticks;
  }

  private static FishingBot newBot(FeatureGame game, FeatSettings settings, FeatRecorder recorder) {
    return new FishingBot(game, settings, recorder, new Random(7L));
  }

  public static void main(String[] args) {
    rodSwitchesAtLimit();
    rodStopsWithoutSpare();
    rodSwitchingOff();
    rodBreaksWithSpare();
    rodBreaksWithoutSpare();
    rodRemovedByPlayer();
    rodUnbreakable();
    rodUsedUpOnlyAtLimit();

    eatsWhenHungry();
    eatsSeveralMeals();
    noFoodAvailable();
    autoEatOff();
    eatingInterruptedByScreen();
    eatingThatNeverStarts();
    noEatingWhileHookOut();

    zoneStopsFarAway();
    zoneExplicitCenter();
    zoneOff();

    emptyMessageStops();
    emptyMessageMatching();

    walkToNextSpot();
    walkOrderAndCooldown();
    walkCooldownExpires();
    walkOutsideZoneSpotsSkipped();
    walkNeedsZoneAndSpots();
    walkDamageStops();
    walkNoPathStops();
    walkPauseAndResume();
    walkReelsFirst();
    walkInLiquidStops();

    System.out.println(checks + " Prüfungen, " + failures + " Fehler");
    if (failures > 0) {
      System.exit(1);
    }
  }

  // ---- Angel: Haltbarkeitsgrenze und Wechsel -------------------------------------------------------

  private static FeatureGame fishingGame() {
    FeatureGame game = new FeatureGame();
    game.biteAtAge = 100;
    return game;
  }

  private static void rodSwitchesAtLimit() {
    FeatureGame game = fishingGame();
    game.rodLeft = 12;
    game.spares.add(40);
    FeatSettings settings = new FeatSettings();
    settings.minRod = 10;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 900);
    check("Angelwechsel: Bot läuft weiter", bot.isRunning());
    check("Angelwechsel: genau ein Wechsel, waren " + recorder.rodSwitches, recorder.rodSwitches == 1);
    check("Angelwechsel: neue Angel hat viel Rest, war " + recorder.lastRodLeft, recorder.lastRodLeft == 40);
    check("Angelwechsel: die erste Angel wurde bis zur Grenze benutzt, Einholen vor dem Wechsel "
        + (40 - game.rodLeft), game.rodLeft < 40 && game.rodLeft >= 30);
    check("Angelwechsel: weiter geangelt, Auswürfe " + game.casts, game.casts >= 5);
    check("Angelwechsel: die Ersatzliste ist leer", game.spares.isEmpty());
  }

  private static void rodStopsWithoutSpare() {
    FeatureGame game = fishingGame();
    game.rodLeft = 12;
    FeatSettings settings = new FeatSettings();
    settings.minRod = 10;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    runUntilStopped(game, bot, 2000);
    check("Ohne Ersatz: Bot hat gestoppt", !bot.isRunning());
    check("Ohne Ersatz: Grund NO_SPARE_ROD", recorder.reason == StopReason.NO_SPARE_ROD);
    check("Ohne Ersatz: genau eine Meldung", recorder.stops == 1);
    check("Ohne Ersatz: die Angel wurde nicht kaputt gemacht, Rest " + game.rodLeft,
        game.rodLeft == 10 && !game.rodBroke);
    check("Ohne Ersatz: zweimal benutzt, Einholen " + game.reels, game.reels == 2);
  }

  private static void rodSwitchingOff() {
    FeatureGame game = fishingGame();
    game.rodLeft = 12;
    game.spares.add(40);
    FeatSettings settings = new FeatSettings();
    settings.minRod = 10;
    settings.switchRods = false;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    runUntilStopped(game, bot, 2000);
    check("Wechsel aus: Bot stoppt trotz Ersatzangel", !bot.isRunning()
        && recorder.reason == StopReason.NO_SPARE_ROD);
    check("Wechsel aus: es wurde nicht gewechselt", game.equipCalls == 0 && recorder.rodSwitches == 0);
    check("Wechsel aus: die Ersatzangel liegt noch da", game.spares.size() == 1);
  }

  private static void rodBreaksWithSpare() {
    FeatureGame game = fishingGame();
    game.rodLeft = 2;
    game.spares.add(30);
    FeatSettings settings = new FeatSettings();
    settings.minRod = 0;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 900);
    check("Kaputt mit Ersatz: Angel ging kaputt", game.rodBroke);
    check("Kaputt mit Ersatz: Bot läuft weiter", bot.isRunning());
    check("Kaputt mit Ersatz: gewechselt, Wechsel " + recorder.rodSwitches, recorder.rodSwitches == 1);
    check("Kaputt mit Ersatz: danach weiter geangelt, Auswürfe " + game.casts, game.casts >= 4);
  }

  private static void rodBreaksWithoutSpare() {
    FeatureGame game = fishingGame();
    game.rodLeft = 2;
    FeatSettings settings = new FeatSettings();
    settings.minRod = 0;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    int ticks = 0;
    int brokeAt = -1;
    while (bot.isRunning() && ticks < 2000) {
      game.advance();
      bot.tick();
      ticks++;
      if (game.rodBroke && brokeAt < 0) {
        brokeAt = ticks;
      }
    }
    check("Kaputt ohne Ersatz: Bot stoppt", !bot.isRunning() && recorder.reason == StopReason.NO_SPARE_ROD);
    check("Kaputt ohne Ersatz: sofort, nicht erst nach 10 s, nach " + (ticks - brokeAt) + " Ticks",
        brokeAt > 0 && ticks - brokeAt <= 3);
  }

  private static void rodRemovedByPlayer() {
    FeatureGame game = fishingGame();
    game.rodLeft = 50;
    FeatSettings settings = new FeatSettings();
    settings.minRod = 5;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 100);
    game.rod = false; // der Spieler nimmt die Angel weg, sie ist nicht kaputt
    run(game, bot, 150);
    check("Angel weggenommen: nach 150 Ticks läuft der Bot noch", bot.isRunning());
    run(game, bot, 100);
    check("Angel weggenommen: alter Ablauf, Grund NO_ROD", !bot.isRunning() && recorder.reason == StopReason.NO_ROD);
    check("Angel weggenommen: es wurde nichts gewechselt", game.equipCalls == 0);
  }

  private static void rodUnbreakable() {
    FeatureGame game = fishingGame();
    game.rodLeft = Integer.MAX_VALUE;
    game.reelDamage = 0;
    FeatSettings settings = new FeatSettings();
    settings.minRod = 10;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 1200);
    check("Unzerbrechlich: Bot läuft", bot.isRunning());
    check("Unzerbrechlich: nie gewechselt", recorder.rodSwitches == 0 && game.equipCalls == 0);
    check("Unzerbrechlich: wurde geangelt, Auswürfe " + game.casts, game.casts >= 5);
  }

  /** Bei Grenze 0 wird die Angel bis zum Schluss benutzt. */
  private static void rodUsedUpOnlyAtLimit() {
    FeatureGame game = fishingGame();
    game.rodLeft = 3;
    game.spares.add(30);
    FeatSettings settings = new FeatSettings();
    settings.minRod = 0;
    settings.switchRods = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    int ticks = 0;
    while (game.reels < 2 && ticks < 2000) {
      run(game, bot, 1);
      ticks++;
    }
    check("Grenze 0: nach zwei Mal Einholen bleibt die Angel in Gebrauch, Rest " + game.rodLeft,
        recorder.rodSwitches == 0 && game.rodLeft == 1 && game.rod);
    run(game, bot, 100); // das dritte Auswerfen und Einholen folgt
    while (!game.rodBroke && ticks < 4000) {
      run(game, bot, 1);
      ticks++;
    }
    run(game, bot, 3);
    check("Grenze 0: erst nach dem Kaputtgehen wird gewechselt", game.rodBroke && recorder.rodSwitches == 1
        && game.rodLeft == 30);
  }

  // ---- Essen -----------------------------------------------------------------------------------------

  private static FeatSettings eatSettings() {
    FeatSettings settings = new FeatSettings();
    settings.eat = true;
    settings.eatBelow = 14;
    settings.foods = "bread";
    return settings;
  }

  private static void eatsWhenHungry() {
    FeatureGame game = fishingGame();
    game.food = 10;
    game.foodItems = 3;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    boolean sawEatState = false;
    for (int i = 0; i < 400; i++) {
      game.advance();
      bot.tick();
      sawEatState |= bot.state() == FishingBot.State.EAT;
    }
    check("Essen: Bot war im Zustand EAT", sawEatState);
    check("Essen: genau eine Mahlzeit, waren " + game.eatStarts, game.eatStarts == 1);
    check("Essen: Hunger gestillt, Hungerwert " + game.food, game.food == 16);
    check("Essen: Meldung kam einmal", recorder.ate == 1);
    check("Essen: es wurde nicht bei ausgeworfener Angel begonnen", !game.castWhenEatStarted);
    check("Essen: danach wird weiter geangelt, Auswürfe " + game.casts, game.casts >= 2);
    check("Essen: Bot läuft", bot.isRunning());
    check("Essen: am Ende ist nichts mehr in Arbeit", game.eatLeft == 0 && game.eatStopCalls >= 1);
  }

  private static void eatsSeveralMeals() {
    FeatureGame game = fishingGame();
    game.food = 5;
    game.foodGain = 4;
    game.foodItems = 6;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    run(game, bot, 900);
    check("Mehrere Mahlzeiten: bis zum Grenzwert gegessen, Hungerwert " + game.food, game.food > 14);
    check("Mehrere Mahlzeiten: drei Mahlzeiten (5, 9, 13 -> 17), waren " + game.eatStarts, game.eatStarts == 3);
    check("Mehrere Mahlzeiten: nicht mehr gegessen als nötig, Rest " + game.foodItems, game.foodItems == 3);
  }

  private static void noFoodAvailable() {
    FeatureGame game = fishingGame();
    game.food = 4;
    game.foodItems = 0;
    game.rodLeft = Integer.MAX_VALUE; // eine so lange Runde soll die Angel nicht aufbrauchen
    game.reelDamage = 0;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    run(game, bot, 1500);
    check("Kein Essen: Meldung genau einmal, waren " + recorder.noFood, recorder.noFood == 1);
    check("Kein Essen: Bot läuft weiter und angelt, Auswürfe " + game.casts, bot.isRunning() && game.casts >= 5);
    run(game, bot, 14000);
    check("Kein Essen: nach 10 Minuten erneut gemeldet, waren " + recorder.noFood, recorder.noFood == 2);
    check("Kein Essen: in der Zwischenzeit wurde weiter gesucht, aber nie gegessen",
        game.eatStarts == 0 && bot.isRunning());

    // Findet sich plötzlich Essen, wird beim nächsten Suchen gegessen.
    game.foodItems = 2;
    run(game, bot, 1500);
    check("Kein Essen: sobald Essen da ist, wird gegessen", game.eatStarts >= 1);
  }

  private static void autoEatOff() {
    FeatureGame game = fishingGame();
    game.food = 3;
    game.foodItems = 5;
    FeatSettings settings = eatSettings();
    settings.eat = false;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 600);
    check("Essen aus: wurde nie gegessen", game.eatStarts == 0 && recorder.ate == 0 && recorder.noFood == 0);
  }

  private static void eatingInterruptedByScreen() {
    FeatureGame game = fishingGame();
    game.food = 10;
    game.foodItems = 3;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    int ticks = 0;
    while (bot.state() != FishingBot.State.EAT && ticks < 200) {
      game.advance();
      bot.tick();
      ticks++;
    }
    check("Essen unterbrochen: Bot isst", bot.state() == FishingBot.State.EAT);
    game.screenOpen = true;
    run(game, bot, 5);
    check("Essen unterbrochen: Essen wurde beendet, Aufrufe " + game.eatStopCalls, game.eatStopCalls >= 1);
    check("Essen unterbrochen: Bot ist nicht mehr im Zustand EAT", bot.state() != FishingBot.State.EAT);
    game.screenOpen = false;
    run(game, bot, 600);
    check("Essen unterbrochen: danach geht es weiter, Auswürfe " + game.casts, game.casts >= 2 && bot.isRunning());
  }

  private static void eatingThatNeverStarts() {
    FeatureGame game = fishingGame();
    game.food = 10;
    game.foodItems = 5;
    game.eatNeverStarts = true;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    run(game, bot, 130);
    check("Essen klappt nicht: es wurde versucht, aber nicht ständig, Versuche " + game.eatStarts,
        game.eatStarts >= 1 && game.eatStarts <= 2);
    check("Essen klappt nicht: keine falsche Erfolgsmeldung", recorder.ate == 0);
    check("Essen klappt nicht: Bot läuft und angelt weiter", bot.isRunning() && game.casts >= 1);
  }

  private static void noEatingWhileHookOut() {
    FeatureGame game = fishingGame();
    game.foodItems = 3;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, eatSettings(), recorder);

    bot.start();
    run(game, bot, 60); // ausgeworfen, Schwimmer liegt im Wasser
    check("Hunger bei ausgeworfener Angel: Schwimmer ist draußen", game.hook);
    game.food = 5;
    run(game, bot, 40);
    check("Hunger bei ausgeworfener Angel: es wird nicht gegessen, solange er draußen liegt",
        game.eatStarts == 0);
    run(game, bot, 400);
    check("Hunger bei ausgeworfener Angel: nach dem Einholen wird gegessen", game.eatStarts >= 1);
    check("Hunger bei ausgeworfener Angel: nie mit ausgeworfener Angel begonnen", !game.castWhenEatStarted);
  }

  // ---- Bereichsgrenze --------------------------------------------------------------------------------

  private static void zoneStopsFarAway() {
    FeatureGame game = fishingGame();
    FeatSettings settings = new FeatSettings();
    settings.zoneRadius = 10;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    game.player.x = 20.5D + 10.9D;
    run(game, bot, 5);
    check("Grenze: knapp innerhalb (10,9 von 10 plus 1 Spiel) bleibt der Bot an", bot.isRunning());
    game.player.x = 20.5D + 11.2D;
    run(game, bot, 2);
    check("Grenze: außerhalb stoppt der Bot", !bot.isRunning());
    check("Grenze: Grund OUT_OF_ZONE", recorder.reason == StopReason.OUT_OF_ZONE);
    check("Grenze: genau eine Meldung", recorder.stops == 1);
  }

  private static void zoneExplicitCenter() {
    FeatureGame game = fishingGame();
    FeatSettings settings = new FeatSettings();
    settings.zoneRadius = 5;
    settings.zoneCenterSet = true;
    settings.zoneX = 40.0D;
    settings.zoneZ = 40.0D;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 2);
    check("Fester Mittelpunkt: Start außerhalb ergibt sofort einen Stopp", !bot.isRunning()
        && recorder.reason == StopReason.OUT_OF_ZONE);

    FeatureGame inside = fishingGame();
    inside.player.x = 40.5D;
    inside.player.z = 40.5D;
    FishingBot bot2 = newBot(inside, settings, new FeatRecorder());
    bot2.start();
    run(inside, bot2, 300);
    check("Fester Mittelpunkt: innerhalb bleibt der Bot an", bot2.isRunning());
  }

  private static void zoneOff() {
    FeatureGame game = fishingGame();
    FeatSettings settings = new FeatSettings();
    settings.zoneRadius = 0;
    FishingBot bot = newBot(game, settings, new FeatRecorder());
    bot.start();
    game.player.x = 500.0D;
    run(game, bot, 100);
    check("Grenze 0: keine Begrenzung", bot.isRunning());
  }

  // ---- Leere Stelle ----------------------------------------------------------------------------------

  private static void emptyMessageStops() {
    FeatureGame game = fishingGame();
    FeatSettings settings = new FeatSettings();
    settings.empty = "keine fische, no fish";
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("Hier gibt es KEINE Fische mehr!");
    check("Leere Stelle: Reaktion erst im nächsten Tick", bot.isRunning());
    run(game, bot, 2);
    check("Leere Stelle: Bot stoppt ohne Laufen", !bot.isRunning() && recorder.reason == StopReason.SPOT_EMPTY);
    check("Leere Stelle: Meldung kam einmal", recorder.spotEmpties == 1);
  }

  private static void emptyMessageMatching() {
    FeatureGame game = fishingGame();
    FeatSettings settings = new FeatSettings();
    settings.empty = " keine fische ,no fish,";
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.onChatMessage("keine fische");
    bot.start();
    run(game, bot, 5);
    check("Meldung vor dem Start wird ignoriert", bot.isRunning());

    bot.onChatMessage("Willkommen auf dem Server");
    bot.onChatMessage(null);
    run(game, bot, 5);
    check("Andere Meldungen werden ignoriert", bot.isRunning() && recorder.spotEmpties == 0);

    bot.onChatMessage("Oh no, NO FISH here");
    run(game, bot, 3);
    check("Zweiter Textteil (Groß/Klein egal) wird erkannt", !bot.isRunning() && recorder.spotEmpties == 1);

    FeatureGame game2 = fishingGame();
    FeatSettings settings2 = new FeatSettings();
    settings2.empty = "";
    FishingBot bot2 = newBot(game2, settings2, new FeatRecorder());
    bot2.start();
    bot2.onChatMessage("keine Fische mehr");
    run(game2, bot2, 5);
    check("Ohne eingestellten Text passiert nichts", bot2.isRunning());
  }

  // ---- Weg zur nächsten Stelle -------------------------------------------------------------------------

  private static final Spot S1 = new Spot(20.5D, 1.0D, 20.5D, 0.0F, 30.0F);
  private static final Spot S2 = new Spot(30.5D, 1.0D, 20.5D, 90.0F, 20.0F);
  private static final Spot S3 = new Spot(20.5D, 1.0D, 30.5D, 180.0F, 10.0F);

  private static FeatSettings walkSettings() {
    FeatSettings settings = new FeatSettings();
    settings.empty = "keine fische";
    settings.walk = true;
    settings.zoneRadius = 15;
    settings.zoneCenterSet = true;
    settings.zoneX = 25.0D;
    settings.zoneZ = 25.0D;
    List<Spot> spots = new ArrayList<>();
    spots.add(S1);
    spots.add(S2);
    spots.add(S3);
    settings.spots = Spot.formatList(spots);
    return settings;
  }

  private static boolean at(FeatureGame game, Spot spot) {
    return Math.hypot(game.player.x - spot.x, game.player.z - spot.z) < 0.6D;
  }

  private static void walkToNextSpot() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 80);
    int castsBefore = game.casts;
    bot.onChatMessage("Hier gibt es keine Fische mehr");
    run(game, bot, 2);
    check("Laufen: Bot ist im Zustand WALK", bot.state() == FishingBot.State.WALK);
    check("Laufen: Ziel ist Stelle 2", recorder.walkStarts.size() == 1 && recorder.walkStarts.get(0) == 2);

    int ticks = 0;
    while (bot.state() == FishingBot.State.WALK && ticks < 600) {
      game.advance();
      bot.tick();
      ticks++;
    }
    check("Laufen: angekommen, nach " + ticks + " Ticks", bot.state() != FishingBot.State.WALK && bot.isRunning());
    check("Laufen: Dauer plausibel (10 Blöcke), Ticks " + ticks, ticks > 50 && ticks < 300);
    check("Laufen: an Stelle 2", at(game, S2));
    check("Laufen: Blickrichtung wie gespeichert, yaw " + game.player.yaw + " pitch " + game.player.pitch,
        Math.abs(game.player.yaw - 90.0F) < 2.0F && Math.abs(game.player.pitch - 20.0F) < 2.0F);
    check("Laufen: Ankunft gemeldet", recorder.walkArrivals.size() == 1 && recorder.walkArrivals.get(0) == 2);
    check("Laufen: Tasten losgelassen", !game.player.forward && !game.player.jump);
    check("Laufen: vor dem Loslaufen wurde eingeholt", !game.hook);

    run(game, bot, 400);
    check("Laufen: an der neuen Stelle wird weiter geangelt, Auswürfe " + game.casts, game.casts > castsBefore);
    check("Laufen: Bot läuft", bot.isRunning());
  }

  private static void walkOrderAndCooldown() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    runUntilWalkDone(game, bot);
    check("Reihenfolge: erst Stelle 2", at(game, S2));

    run(game, bot, 100);
    bot.onChatMessage("keine fische"); // noch in der Schutzfrist nach der Ankunft: wird ignoriert
    run(game, bot, 5);
    check("Schutzfrist: eine Meldung kurz nach der Ankunft löst nichts aus",
        bot.isRunning() && bot.state() != FishingBot.State.WALK && recorder.walkStarts.size() == 1
            && recorder.spotEmpties == 1);

    run(game, bot, 250);
    bot.onChatMessage("keine fische");
    runUntilWalkDone(game, bot);
    check("Reihenfolge: dann Stelle 3", at(game, S3) && recorder.walkStarts.size() == 2
        && recorder.walkStarts.get(1) == 3);

    run(game, bot, 250);
    bot.onChatMessage("keine fische");
    run(game, bot, 3);
    check("Alle leer: Bot stoppt", !bot.isRunning() && recorder.reason == StopReason.ALL_SPOTS_EMPTY);
    check("Alle leer: die Tasten sind los", !game.player.forward && !game.player.jump);
  }

  private static void runUntilWalkDone(FeatureGame game, FishingBot bot) {
    int ticks = 0;
    run(game, bot, 2);
    while (bot.state() == FishingBot.State.WALK && ticks < 700) {
      game.advance();
      bot.tick();
      ticks++;
    }
  }

  private static void walkCooldownExpires() {
    FeatureGame game = fishingGame();
    FeatSettings settings = walkSettings();
    settings.cooldown = 1; // 1 Minute = 1200 Ticks
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische"); // Stelle 1 leer -> Stelle 2
    runUntilWalkDone(game, bot);
    run(game, bot, 1300); // Stelle 1 ist wieder bereit
    bot.onChatMessage("keine fische"); // Stelle 2 leer -> Stelle 3
    runUntilWalkDone(game, bot);
    check("Wartezeit: zweiter Wechsel zu Stelle 3", at(game, S3));
    run(game, bot, 250);
    bot.onChatMessage("keine fische"); // Stelle 3 leer -> Stelle 1 (Wartezeit abgelaufen), 2 noch nicht
    runUntilWalkDone(game, bot);
    check("Wartezeit: zurück an Stelle 1, " + recorder.walkStarts, at(game, S1) && bot.isRunning());
  }

  private static void walkOutsideZoneSpotsSkipped() {
    FeatureGame game = fishingGame();
    FeatSettings settings = walkSettings();
    List<Spot> spots = new ArrayList<>();
    spots.add(S1);
    spots.add(new Spot(60.5D, 1.0D, 20.5D, 90.0F, 20.0F)); // 35 Blöcke vom Mittelpunkt: außerhalb
    settings.spots = Spot.formatList(spots);
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    run(game, bot, 3);
    check("Stelle außerhalb des Bereichs: wird nicht angelaufen", !bot.isRunning()
        && recorder.reason == StopReason.ALL_SPOTS_EMPTY && recorder.walkStarts.isEmpty());
  }

  private static void walkNeedsZoneAndSpots() {
    // Laufen an, aber kein Mittelpunkt gespeichert
    FeatureGame game = fishingGame();
    FeatSettings noCenter = walkSettings();
    noCenter.zoneCenterSet = false;
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, noCenter, recorder);
    bot.start();
    bot.onChatMessage("keine fische");
    run(game, bot, 3);
    check("Ohne Mittelpunkt: kein Laufen, Bot stoppt", !bot.isRunning() && recorder.reason == StopReason.SPOT_EMPTY);

    // Nur eine Stelle gespeichert
    FeatureGame game2 = fishingGame();
    FeatSettings oneSpot = walkSettings();
    oneSpot.spots = S1.format();
    FeatRecorder recorder2 = new FeatRecorder();
    FishingBot bot2 = newBot(game2, oneSpot, recorder2);
    bot2.start();
    bot2.onChatMessage("keine fische");
    run(game2, bot2, 3);
    check("Nur eine Stelle: kein Laufen, Bot stoppt", !bot2.isRunning() && recorder2.reason == StopReason.SPOT_EMPTY);

    // Radius 0
    FeatureGame game3 = fishingGame();
    FeatSettings noRadius = walkSettings();
    noRadius.zoneRadius = 0;
    FeatRecorder recorder3 = new FeatRecorder();
    FishingBot bot3 = newBot(game3, noRadius, recorder3);
    bot3.start();
    bot3.onChatMessage("keine fische");
    run(game3, bot3, 3);
    check("Radius 0: kein Laufen, Bot stoppt", !bot3.isRunning() && recorder3.reason == StopReason.SPOT_EMPTY);

    // Laufen aus
    FeatureGame game4 = fishingGame();
    FeatSettings off = walkSettings();
    off.walk = false;
    FeatRecorder recorder4 = new FeatRecorder();
    FishingBot bot4 = newBot(game4, off, recorder4);
    bot4.start();
    bot4.onChatMessage("keine fische");
    run(game4, bot4, 3);
    check("Laufen aus: Bot stoppt", !bot4.isRunning() && recorder4.reason == StopReason.SPOT_EMPTY
        && recorder4.walkStarts.isEmpty());
  }

  private static void walkDamageStops() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    run(game, bot, 60);
    check("Schaden: Bot läuft gerade", bot.state() == FishingBot.State.WALK);
    game.player.health = 17.0F;
    run(game, bot, 2);
    check("Schaden: Bot stoppt", !bot.isRunning() && recorder.reason == StopReason.WALK_DANGER);
    check("Schaden: Tasten losgelassen", !game.player.forward && !game.player.jump);

    FeatureGame low = fishingGame();
    low.player.health = 5.0F;
    FeatRecorder recorder2 = new FeatRecorder();
    FishingBot bot2 = newBot(low, walkSettings(), recorder2);
    bot2.start();
    bot2.onChatMessage("keine fische");
    run(low, bot2, 10);
    check("Wenig Leben: es wird gar nicht erst gelaufen", !bot2.isRunning()
        && recorder2.reason == StopReason.WALK_DANGER);
  }

  private static void walkInLiquidStops() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    run(game, bot, 60);
    game.player.inLiquid = true;
    run(game, bot, 2);
    check("Im Wasser: Bot stoppt", !bot.isRunning() && recorder.reason == StopReason.WALK_DANGER);
    check("Im Wasser: Tasten losgelassen", !game.player.forward && !game.player.jump);
  }

  private static void walkNoPathStops() {
    FeatureGame game = fishingGame();
    // Stelle 2 ist von Mauern umgeben
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        if (dx != 0 || dz != 0) {
          game.world.wall(30 + dx, 20 + dz, 3);
        }
      }
    }
    FeatSettings settings = walkSettings();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, settings, recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    runUntilStopped(game, bot, 300);
    check("Kein Weg: Bot stoppt mit WALK_FAILED", !bot.isRunning() && recorder.reason == StopReason.WALK_FAILED);
    check("Kein Weg: Tasten losgelassen", !game.player.forward && !game.player.jump);
  }

  private static void walkPauseAndResume() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 80);
    bot.onChatMessage("keine fische");
    run(game, bot, 40);
    check("Pause beim Laufen: Bot läuft gerade", bot.state() == FishingBot.State.WALK && game.player.forward);
    game.screenOpen = true;
    run(game, bot, 3);
    check("Pause beim Laufen: Tasten losgelassen", !game.player.forward && !game.player.jump);
    check("Pause beim Laufen: Bot ist pausiert, aber noch im Zustand WALK",
        bot.isPaused() && bot.state() == FishingBot.State.WALK);
    double frozenX = game.player.x;
    run(game, bot, 40);
    check("Pause beim Laufen: die Figur steht still", Math.abs(game.player.x - frozenX) < 0.01D);
    game.screenOpen = false;
    runUntilWalkDone(game, bot);
    check("Pause beim Laufen: danach kommt der Bot an", at(game, S2) && bot.isRunning());
  }

  /** Liegt der Schwimmer draußen, wenn die Meldung kommt, wird zuerst eingeholt. */
  private static void walkReelsFirst() {
    FeatureGame game = fishingGame();
    FeatRecorder recorder = new FeatRecorder();
    FishingBot bot = newBot(game, walkSettings(), recorder);

    bot.start();
    run(game, bot, 70);
    check("Erst einholen: Schwimmer liegt draußen", game.hook);
    int reelsBefore = game.reels;
    bot.onChatMessage("keine fische");
    run(game, bot, 3);
    check("Erst einholen: Angel wurde eingeholt", game.reels == reelsBefore + 1);
    run(game, bot, 10);
    check("Erst einholen: Schwimmer ist weg, bevor gelaufen wird", !game.hook);
    runUntilWalkDone(game, bot);
    check("Erst einholen: danach ist der Bot an Stelle 2", at(game, S2) && bot.isRunning());
  }
}
