package com.mekaddon.bridge;

import java.util.List;
import mekanism.api.inventory.IInventorySlot;

/**
 * Duck-typed view onto the Combining Factory's per-process secondary (yellow) input slots.
 * <p>
 * Implemented by {@code CombiningFactoryExtraSlotsMixin} and consumed by {@code ExtraRowLayout}, which owns the
 * inventory-list bookkeeping while this side owns the slot objects and the recipe plumbing. A plain interface keeps
 * the two independent of each other at compile time.
 */
public interface CombiningExtraRowView {

    /**
     * Whether the machine is currently running its per-process row — i.e. whether its {@code extraSlot} /
     * {@code extraInputHandler} fields point at the row rather than at its own shared secondary slot.
     * <p>
     * This is bookkeeping the mod owns, and it is deliberately <b>not</b> derived from inventory-list membership: that
     * is what the row's presence in a menu no longer depends on either.
     */
    boolean mekaddon$rowActive();

    /**
     * Creates the per-process secondary slots if they do not exist yet and points the vanilla recipe fields at them.
     * <p>
     * The slots themselves are created once for the machine's lifetime and registered with the machine's real
     * {@code InventorySlotHelper}; activating again is a no-op, and deactivating never destroys them — which of them a
     * menu contains is decided per container build by the slots' own {@code createContainerSlot()}.
     *
     * @return the row, empty if it could not be created.
     */
    List<IInventorySlot> mekaddon$installRow();

    /**
     * Points the vanilla fields back at the machine's own shared secondary slot and marks the row inactive. The row
     * slots stay where they are; they simply stop being offered to menus and to item automation.
     */
    void mekaddon$uninstallRow();

    /**
     * Points the vanilla {@code extraSlot} / {@code extraInputHandler} fields at {@code process}'s own secondary slot.
     * No-op when the row is not active.
     */
    void mekaddon$selectProcess(int process);

    /**
     * The machine's own single shared secondary slot, whatever the current state — vanilla's slot, which is only ever
     * re-pointed, never destroyed.
     */
    IInventorySlot mekaddon$sharedSecondarySlot();

    /** The per-process secondary slots, in process order. Empty only before the row has ever been created. */
    List<IInventorySlot> mekaddon$secondarySlots();
}
