package com.mekaddon.slot;

import com.mekaddon.state.ExtraRowState;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The Combining Factory's own secondary ("yellow") input slot: stack-scaled like every other
 * {@link StackScaledInputSlot}, plus the two things that follow from the machine being able to trade it away.
 * <p>
 * Installing the combining-slot upgrade replaces it with one secondary input per process, so while that upgrade is in
 * force this slot must (a) stay out of the menu — otherwise it would be drawn next to the row and do nothing — and
 * (b) refuse insertions, because nothing would consume what landed in it. Both answers come from
 * {@link ExtraRowState#isUnlocked}, the same predicate the window geometry uses, which is what keeps the menu and the
 * layout from ever disagreeing.
 */
public class CombiningSharedSecondarySlot extends StackScaledInputSlot implements ExtraSecondarySlot {

    public static CombiningSharedSecondarySlot create(TileEntityMekanism owner, Predicate<ItemStack> valid,
          @Nullable IContentsListener listener, int x, int y) {
        return new CombiningSharedSecondarySlot(owner, valid, valid, listener, x, y);
    }

    protected CombiningSharedSecondarySlot(TileEntityMekanism owner, Predicate<ItemStack> valid,
          Predicate<ItemStack> canExtract, @Nullable IContentsListener listener, int x, int y) {
        super(owner, valid, canExtract, listener, x, y);
    }

    @Override
    public InventoryContainerSlot createContainerSlot() {
        return ExtraRowState.isUnlocked(owner()) ? null : super.createContainerSlot();
    }

    @Override
    public boolean isItemValidForInsertion(ItemStack stack, AutomationType automationType) {
        return !ExtraRowState.isUnlocked(owner()) && super.isItemValidForInsertion(stack, automationType);
    }
}
