package com.mekaddon.state;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.ExtraRowView;
import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;

/**
 * Answers "does this Combining Factory have the extra secondary-input row?".
 * <p>
 * On the <b>server</b> the answer is simply whether the mod's upgrade is installed. Mekanism owns everything that
 * entails: the upgrade slot accepts the item, {@code TileComponentUpgrade} installs it into its map, the count is
 * persisted in the tile's own upgrade NBT, and the uninstall button gives the item back.
 * <p>
 * On the <b>client</b> the count cannot be used, which is subtle and was the cause of a "Network Protocol Error": the
 * count is synced by Mekanism's container trackers, so it only arrives <i>after</i> a container has been opened — but
 * the client needs the answer while it is constructing that very menu, because the answer decides how many slots the
 * menu has. The block entity update packet carries the flag instead; it can arrive at any time and is what tells the
 * client to close and reopen the GUI.
 * <p>
 * This runs during {@code TileEntityMekanism}'s constructor (the inventory holder is built from there), so it must not
 * touch anything that requires a level — {@link ModSide} exists for that reason, and a null
 * {@code getComponent()} is the expected state at that point rather than an error.
 */
public final class ExtraRowState {

    private ExtraRowState() {
    }

    public static boolean isUnlocked(TileEntityMekanism tile) {
        Upgrade upgrade = ModUpgrades.combiningSlots();
        // Only machines the mod can actually extend qualify; everything else answers no and is left completely alone.
        if (upgrade == null || !(tile instanceof CombiningRowTile)) {
            return false;
        }
        try {
            TileComponentUpgrade component = tile.getComponent();
            if (ModSide.isClient(tile)) {
                // The flag is what the client can rely on *before* a menu is built. The synced count is a second,
                // independent signal: it only arrives once a container has been opened, but from then on it is
                // authoritative, so treating either signal as "installed" makes the client self-correcting if the
                // update packet is ever missed. Erring towards "installed" is safe - the client then has *more* menu
                // slots than the server, which vanilla tolerates, whereas the reverse is what causes the kick.
                if (tile instanceof ExtraRowView view && view.mekaddon$extraRowInstalled()) {
                    return true;
                }
                return component != null && component.getUpgrades(upgrade) > 0;
            }
            // Null while the block entity is still being constructed, which simply means "no upgrade installed yet".
            return component != null && component.getUpgrades(upgrade) > 0;
        } catch (RuntimeException e) {
            // This sits on the block entity construction and NBT load path, where a throw is fatal to the world rather
            // than merely wrong: report the wrong answer (the row is treated as not installed, and gets restored by the
            // next sync) instead of taking the chunk down. Logged loudly so a real defect is still visible.
            com.mekaddon.MekAddon.LOGGER.error("Could not determine whether {} has the secondary row installed",
                  tile.getClass().getName(), e);
            return false;
        }
    }
}
