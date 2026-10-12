package com.mekaddon.slot;

import com.mekaddon.MekAddon;
import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.state.ExtraRowState;
import com.mekaddon.state.ModSide;
import java.util.List;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Keeps a Combining Factory's secondary input in step with whether the mod's upgrade is installed.
 * <p>
 * "In step" means three separate things, and it is worth being explicit about which list owns what:
 * <ul>
 *     <li><b>Which slots exist.</b> Every slot the mod adds is created once, when the machine is built, and stays in
 *     the inventory holder for the machine's whole life. Which of them the <i>menu</i> contains is decided per container
 *     build by the slots' own {@code createContainerSlot()} from {@link ExtraRowState#isUnlocked} — the same answer the
 *     window geometry uses, so the two can never disagree. Earlier revisions instead added and removed the row from the
 *     holder at runtime; that is only correct if the holder hands out its internal list, and where it does not (a holder
 *     that returns a copy), the row ends up installed but invisible while the old shared slot stays on screen.</li>
 *     <li><b>The output row's position.</b> The output slots are moved down by one row, in place, on the existing slot
 *     objects — the container copies their coordinates when it is built, so this runs before the menu is constructed.</li>
 *     <li><b>The side config.</b> The slots that are live right now are registered as {@code DataType.EXTRA}, which is
 *     what item automation and the side-config GUI read.</li>
 * </ul>
 * Called from four places, all outside the constructor: upgrade changes ({@code TileEntityMekanismRecalcMixin}), NBT
 * load ({@code TileEntityMekanismLoadMixin}) and container construction ({@code MekanismTileContainerMixin} — the
 * client learns about the layout from the block entity update packet, see {@link ExtraRowState}).
 */
public final class ExtraRowLayout {

    private ExtraRowLayout() {
    }

    /** Idempotent: safe to call as often as the caller likes, and a no-op for unsupported machines. */
    public static void sync(TileEntityMekanism tile) {
        if (!(tile instanceof CombiningRowTile row)) {
            return;
        }
        try {
            syncUnchecked(row);
        } catch (RuntimeException e) {
            // Runs during NBT load as well, where a throw would take the chunk down with it. The row is simply left in
            // whatever state it was, and the next sync (GUI open) corrects it.
            MekAddon.LOGGER.error("Failed to sync the secondary input row on {}", row.mekaddon$describe(), e);
        }
    }

    private static void syncUnchecked(CombiningRowTile row) {
        TileEntityMekanism tile = (TileEntityMekanism) row;
        boolean installed = ExtraRowState.isUnlocked(tile);
        boolean changed = false;

        // The block entity's own fields (extraSlot / extraInputHandler) are re-pointed at the row — or back at the
        // shared slot — here. No slot is created or destroyed either way; see the class comment.
        if (installed != row.mekaddon$rowActive()) {
            if (installed) {
                List<IInventorySlot> rowSlots = row.mekaddon$installRow();
                changed = true;
                MekAddon.LOGGER.info("{}: activated {} secondary input slot(s)", row.mekaddon$describe(), rowSlots.size());
            } else {
                row.mekaddon$uninstallRow();
                changed = true;
                MekAddon.LOGGER.info("{}: deactivated the secondary input row", row.mekaddon$describe());
            }
        }

        // Exactly the slots that are live right now. Rewriting this unconditionally would be a bug rather than a
        // harmless refresh: every registration appends the DataType to each slot's own list, and Mekanism stops
        // resolving a DataType once that list has grown to the size of the config's supported set — the frame would
        // silently go grey. registerConfig therefore compares first and only writes on a real change.
        IInventorySlot sharedNow = row.mekaddon$sharedSecondarySlot();
        ExtraRowSlots.registerConfig(tile,
              installed ? row.mekaddon$secondarySlots() : withShared(sharedNow, List.of()));

        // Same reasoning for the geometry: the container copies each slot's coordinates when it is built, and the
        // client reaches this method from the container's constructor precisely so the copy happens after the move.
        // The main inputs drop by a few pixels with it, so the secondary slot of a process stays under its main input
        // and the title keeps some room above the first row.
        ExtraRowSlots.positionInputRow(row.mekaddon$inputSlots(), installed);
        ExtraRowSlots.positionOutputRow(row.mekaddon$outputSlots(), installed);

        // Sanity check, logged only when the layout just changed: the machine's own inventory view has to contain what
        // was made live, or the row is installed and yet will never reach a menu. Cheap, bounded, and it turns "the
        // slots are missing" into a one-line diagnosis.
        if (changed) {
            List<IInventorySlot> view = ExtraRowSlots.liveSlots(tile);
            List<IInventorySlot> active = installed ? row.mekaddon$secondarySlots() : List.of();
            if (view != null && !view.containsAll(active)) {
                MekAddon.LOGGER.warn("{}: the machine's inventory view does not contain the {} active secondary "
                      + "slot(s); the row will not reach the menu", row.mekaddon$describe(), active.size());
            }
        }

        if (changed) {
            notifyChanged(tile);
        }
    }

    /** {@code shared} first (may be {@code null}), then the row. */
    private static List<IInventorySlot> withShared(IInventorySlot shared, List<IInventorySlot> row) {
        if (shared == null) {
            return row;
        }
        List<IInventorySlot> all = new java.util.ArrayList<>(row.size() + 1);
        all.add(shared);
        all.addAll(row);
        return all;
    }

    /**
     * Tells clients about the new layout and gets any stale open container out of the way.
     * <p>
     * Closing the container server-side is what makes the whole thing deterministic: a menu's slot list is fixed for
     * its lifetime, and because the client builds its menu from its own copy of the tile, the two sides must let go of
     * the old GUI together. The update packet is sent first so the client already knows the new layout by the time it
     * reopens.
     */
    private static void notifyChanged(TileEntityMekanism tile) {
        if (!tile.hasLevel()) {
            return;
        }
        tile.markForSave();
        if (ModSide.isClient(tile)) {
            return;
        }
        tile.sendUpdatePacket();
        for (Player player : List.copyOf(tile.playersUsing)) {
            if (player instanceof ServerPlayer serverPlayer
                  && serverPlayer.containerMenu instanceof MekanismTileContainer<?> container
                  && container.getTileEntity() == tile) {
                serverPlayer.closeContainer();
            }
        }
    }
}
