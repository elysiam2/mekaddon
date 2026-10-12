package com.mekaddon.slot;

import mekanism.api.inventory.IInventorySlot;
import net.minecraft.world.item.ItemStack;

/**
 * A secondary ("extra") input slot the mod owns, whichever machine family it belongs to.
 * <p>
 * There are two of those families and they cannot share a base class: Mekanism's slots are built on
 * {@code InputInventorySlot} ({@link StackScaledInputSlot}) while Mekanism Extras' factory framework has its own
 * {@code StackableInputInventorySlot}, which only Extras' own subclass can extend ({@link ExtrasStackedInputSlot}).
 * Code that has to treat both alike — the even-distribution pass over the per-process row, for instance — goes through
 * this interface instead of a concrete type.
 */
public interface SecondaryInputSlot extends IInventorySlot {

    /**
     * Whether this slot would take {@code stack}, ignoring how full it currently is.
     * <p>
     * Mekanism keeps that predicate private inside the slot; capturing it lets the distribution pass ask exactly the
     * question it needs ("would you accept this ingredient?") to decide which slots an item may be spread across.
     */
    boolean accepts(ItemStack stack);
}
