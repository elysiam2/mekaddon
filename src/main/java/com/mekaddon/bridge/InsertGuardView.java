package com.mekaddon.bridge;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;

/**
 * Duck-typed view onto a Mekanism inventory slot that the mod wants to add an extra acceptance rule to.
 * <p>
 * Implemented by {@code BasicInventorySlotGuardMixin}, which applies the guard on top of whatever the slot already
 * decided — the guard can only ever make a slot <i>more</i> restrictive. Mekanism's own validation of a slot is
 * captured in its (private, final) predicates, so layering a further check on the slot is the only way to add a
 * runtime rule to a slot the mod does not own.
 */
public interface InsertGuardView {

    /**
     * Installs the guard. It is consulted with the incoming stack after the slot's own validation has accepted it; a
     * {@code true} result makes the insertion fail.
     */
    void mekaddon$setInsertGuard(Predicate<ItemStack> guard);
}
