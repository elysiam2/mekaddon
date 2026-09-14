package com.mekaddon.slot;

import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One process's secondary ("yellow") input of the Combining Factory — the slot the vanilla machine only has once,
 * shared by every column.
 * <p>
 * It is a plain {@link InputInventorySlot} that additionally carries its process index and the
 * {@link ContainerSlotType#EXTRA} slot type. The slot type matters twice over: it drives the gold slot frame drawn by
 * {@code SlotType#get(DataType)} and it is what the side-config registration keys off.
 */
public class AddonExtraInputSlot extends InputInventorySlot {

    private final int process;

    public static AddonExtraInputSlot create(TileEntityCombiningFactory factory, int process,
          mekanism.api.inventory.IInventorySlot mainInput, @Nullable IContentsListener listener, int x, int y) {
        // Mirror the vanilla secondary slot's validation: an item may go in when the machine has a two-input recipe
        // that accepts it — paired with the matching main input when that column already holds one.
        Predicate<ItemStack> valid = stack -> mainInput == null || mainInput.isEmpty()
              ? factory.containsRecipeB(stack)
              : factory.containsRecipeAB(mainInput.getStack(), stack);
        AddonExtraInputSlot slot = new AddonExtraInputSlot(process, valid, listener, x, y);
        slot.setSlotType(ContainerSlotType.EXTRA);
        return slot;
    }

    protected AddonExtraInputSlot(int process, Predicate<ItemStack> valid, @Nullable IContentsListener listener, int x, int y) {
        super(valid, valid, listener, x, y);
        this.process = process;
    }

    /** The factory process (column) this secondary input feeds. */
    public int getProcess() {
        return process;
    }
}
