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
 * Keeps a Combining Factory's live inventory in step with whether the mod's upgrade is installed.
 * <p>
 * This is deliberately <b>not</b> a rebuild. Mekanism creates every slot once, inside the block entity constructor,
 * and then hands that same list out to the GUI container, to item automation and to the recipe handlers. Swapping the
 * list contents for freshly built slots — which an earlier iteration did — leaves all of those pointing at detached
 * objects, and the symptoms are subtle: the slot frame colours vanish (the side-config mapping is keyed by slot
 * object), the output slots report "no space" forever (the cached recipe keeps writing into the detached slot), and
 * the machine appears to stop processing.
 * <p>
 * Instead the row is appended to and removed from the live list, the output row is moved by changing the existing
 * slots' coordinates, and nothing else is touched.
 * <p>
 * The <b>holder list is treated as the source of truth</b>, not the mixin's own bookkeeping: any other patch, or a
 * save/load in the middle, can move them out of step, and trusting the bookkeeping alone means the row silently stops
 * coming back. See {@link #syncUnchecked}.
 * <p>
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
        List<IInventorySlot> live = ExtraRowSlots.liveSlots(tile);
        if (live == null) {
            MekAddon.LOGGER.warn("{}: could not resolve the inventory holder; secondary row left as it is",
                  row.mekaddon$describe());
            return;
        }
        List<IInventorySlot> tracked = row.mekaddon$secondarySlots();
        // The holder is the truth. The tracked slots and the holder can drift apart (a holder that got replaced, a
        // slot dropped by another patch, a mid-save reload), and if the bookkeeping then claims "already installed"
        // while the holder disagrees, the row silently stops appearing. Comparing against the holder makes the
        // mismatch self-healing instead.
        boolean present = !tracked.isEmpty() && live.containsAll(tracked);
        boolean changed = false;

        if (installed && !present) {
            if (!tracked.isEmpty()) {
                // Drop the stale row before building a fresh one.
                live.removeAll(tracked);
                row.mekaddon$uninstallRow();
            }
            IInventorySlot shared = row.mekaddon$sharedSecondarySlot();
            List<IInventorySlot> created = row.mekaddon$installRow();
            if (shared != null) {
                // The machine's own single shared secondary slot is replaced by the per-process row; leaving it in the
                // holder would keep the original yellow slot on screen next to the new ones.
                live.remove(shared);
            }
            live.addAll(created);
            // The shared slot is registered even while it is out of the holder: ConfigInfo#addSlotInfo *replaces* the
            // mapping for a DataType, so a registration that omitted it would leave it with no DataType at all - and a
            // slot with no DataType renders plain grey instead of gold. That is exactly the "the yellow slot lost its
            // colour" symptom.
            ExtraRowSlots.registerConfig(tile, withShared(shared, created));
            changed = true;
            MekAddon.LOGGER.info("{}: installed {} secondary input slot(s)", row.mekaddon$describe(), created.size());
        } else if (!installed && !tracked.isEmpty()) {
            List<IInventorySlot> removed = row.mekaddon$secondarySlots();
            // uninstallRow() hands back the machine's own shared slot, which is what has to go back into the holder -
            // reading sharedSecondarySlot() instead would return this row's first slot, because that is where the
            // vanilla field has been re-pointed.
            IInventorySlot shared = row.mekaddon$uninstallRow();
            live.removeAll(removed);
            if (shared != null) {
                if (!live.contains(shared)) {
                    live.add(shared);
                }
                ExtraRowSlots.registerConfig(tile, List.of(shared));
            }
            changed = true;
            MekAddon.LOGGER.info("{}: removed the secondary input row again", row.mekaddon$describe());
        }

        // Always re-asserted, and idempotent: whoever is the secondary input right now must carry the EXTRA DataType,
        // otherwise the slot renders plain grey ("the yellow slot lost its colour"). Doing it on every sync also
        // repairs a machine that got into that state earlier. The row is re-read here rather than reused from above,
        // because the install branch has just replaced it.
        IInventorySlot sharedNow = row.mekaddon$sharedSecondarySlot();
        ExtraRowSlots.registerConfig(tile,
              installed ? withShared(sharedNow, row.mekaddon$secondarySlots()) : withShared(sharedNow, List.of()));
        // Same reasoning for the geometry: the container copies each slot's coordinates when it is built, and the
        // client reaches this method from the container's constructor precisely so the copy happens after the move.
        ExtraRowSlots.positionOutputRow(row.mekaddon$outputSlots(), installed);

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
