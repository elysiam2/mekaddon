package com.mekaddon.mixin;

import com.mekaddon.bridge.SlotPositionView;
import mekanism.common.inventory.slot.BasicInventorySlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Makes {@code BasicInventorySlot}'s slot coordinates writable.
 * <p>
 * They are final because Mekanism creates each slot once, in the block entity constructor. The upgraded secondary row
 * has to push the output row down by one row height, and rebuilding the inventory to do that invalidates every
 * reference Mekanism holds to the old slot objects (side-config mappings, recipe handlers, cached recipes). Moving the
 * existing slots is both simpler and safer: the container copies each slot's coordinates when it is built, and the mod
 * closes and reopens the GUI when the layout changes.
 */
@Mixin(BasicInventorySlot.class)
public abstract class BasicInventorySlotPositionMixin implements SlotPositionView {

    @Shadow
    @Mutable
    private int x;

    @Shadow
    @Mutable
    private int y;

    @Override
    public void mekaddon$moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int mekaddon$slotX() {
        return x;
    }

    @Override
    public int mekaddon$slotY() {
        return y;
    }
}
