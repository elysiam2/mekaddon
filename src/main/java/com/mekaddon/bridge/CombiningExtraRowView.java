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
     * Creates the per-process secondary slots and re-points Mekanism's single-slot recipe fields at them.
     * <p>
     * Mekanism models the secondary input as one shared slot for the whole machine; once the upgrade is installed each
     * process owns one, so the fields are re-pointed per process instead of duplicating the recipe machinery. The
     * created slots are <b>not</b> added to any holder — the caller does that.
     */
    List<IInventorySlot> mekaddon$installRow();

    /**
     * Tears the row down again and restores the vanilla fields.
     *
     * @return the machine's own shared secondary slot, which the caller must put back into the holder, or {@code null}.
     */
    IInventorySlot mekaddon$uninstallRow();

    /**
     * Points the vanilla {@code extraSlot} / {@code extraInputHandler} fields at {@code process}'s own secondary slot.
     * No-op when the row is not installed.
     */
    void mekaddon$selectProcess(int process);

    /**
     * The machine's own single shared secondary slot, whatever the current state: vanilla's slot while the row is not
     * installed, and still vanilla's slot afterwards (it is only re-pointed, never destroyed). The caller removes it
     * from — and puts it back into — the inventory holder.
     */
    IInventorySlot mekaddon$sharedSecondarySlot();

    /** The per-process secondary slots, in process order. Empty while the upgrade is not installed. */
    List<IInventorySlot> mekaddon$secondarySlots();
}
