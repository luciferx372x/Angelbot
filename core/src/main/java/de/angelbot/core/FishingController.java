package de.angelbot.core;

import de.angelbot.core.bot.FishingHandle;
import net.labymod.api.reference.annotation.Referenceable;

/**
 * Zugriff auf das Spiel (Angel, Schwimmer, Rechtsklick).
 *
 * <p>Die Umsetzung hängt von der Minecraft-Version ab und steht im Modul {@code game-runner}
 * ({@code VersionedFishingController}). Der Rest des Addons kennt nur diese Schnittstelle.
 */
@Referenceable
public interface FishingController extends FishingHandle {}
