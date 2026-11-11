package de.angelbot.core;

import de.angelbot.core.bot.FishingHandle;
import de.angelbot.core.bot.InventoryHandle;
import de.angelbot.core.bot.MovementHandle;
import net.labymod.api.reference.annotation.Referenceable;

/**
 * Zugriff auf das Spiel: Angel, Schwimmer und Rechtsklick ({@link FishingHandle}), Inventar und
 * Essen ({@link InventoryHandle}) sowie Position und Laufen ({@link MovementHandle}).
 *
 * <p>Die Umsetzung hängt von der Minecraft-Version ab und steht im Modul {@code game-runner}
 * ({@code VersionedFishingController}). Der Rest des Addons kennt nur diese Schnittstelle.
 */
@Referenceable
public interface FishingController extends FishingHandle, InventoryHandle, MovementHandle {}
