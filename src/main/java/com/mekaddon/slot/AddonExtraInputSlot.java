package com.mekaddon.slot;

import com.mekaddon.state.ExtraRowState;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One process's secondary ("yellow") input of the Combining Factory — the slot the vanilla machine only has once,
 * shared by every column.
 * <p>
 * It carries its process index and the {@link ContainerSlotType#EXTRA} slot type. The slot type matters twice over: it
 * drives the gold slot frame drawn by {@code SlotType#get(DataType)} and it is what the side-config registration keys
 * off.
 * <p>
 * The stack limit comes from {@link StackScaledInputSlot}: the "Extra Stack Upgrade" doubles it per upgrade installed.
 * <p>
 * The slot is <b>created once</b>, when the machine is built, and stays in the inventory holder for the machine's whole
 * life. {@link #createContainerSlot()} hands it to the menu only while the upgrade is installed, which is the one place
 * its presence is decided — previously the row was added to and removed from the holder at runtime, and anything that
 * made those list edits ineffective (a holder that returns a copy, for instance) left the row installed but invisible.
 */
public class AddonExtraInputSlot extends StackScaledInputSlot implements ExtraSecondarySlot {

    private final int process;

    public static AddonExtraInputSlot create(TileEntityCombiningFactory factory, int process,
          mekanism.api.inventory.IInventorySlot mainInput, @Nullable IContentsListener listener, int x, int y) {
        // Mirror the vanilla secondary slot's validation: an item may go in when the machine has a two-input recipe
        // that accepts it — paired with the matching main input when that column already holds one.
        Predicate<ItemStack> valid = stack -> mainInput == null || mainInput.isEmpty()
              ? factory.containsRecipeB(stack)
              : factory.containsRecipeAB(mainInput.getStack(), stack);
        AddonExtraInputSlot slot = new AddonExtraInputSlot(factory, process, valid, listener, x, y);
        slot.setSlotType(ContainerSlotType.EXTRA);
        return slot;
    }

    protected AddonExtraInputSlot(TileEntityMekanism owner, int process, Predicate<ItemStack> valid,
          @Nullable IContentsListener listener, int x, int y) {
        super(owner, valid, valid, listener, x, y);
        this.process = process;
    }

    /** The factory process (column) this secondary input feeds. */
    public int getProcess() {
        return process;
    }

    @Override
    public InventoryContainerSlot createContainerSlot() {
        return ExtraRowState.isUnlocked(owner()) ? super.createContainerSlot() : null;
    }

    /**
     * While the upgrade is not installed the row is dead weight: it is not in the menu, the block entity's
     * {@code extraSlot} points at the shared slot, and nothing would consume what lands in it.
     */
    @Override
    public boolean isItemValidForInsertion(ItemStack stack, AutomationType automationType) {
        return ExtraRowState.isUnlocked(owner()) && super.isItemValidForInsertion(stack, automationType);
    }
}
