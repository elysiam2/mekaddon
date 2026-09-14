package com.mekaddon.bridge;

/**
 * Lets the mod reposition an already-created Mekanism inventory slot.
 * <p>
 * {@code BasicInventorySlot#x} and {@code #y} are final, and the slots are created once inside the block entity
 * constructor — so pushing the output row down to make room for the upgraded secondary row either means rebuilding the
 * whole inventory (which invalidates every reference Mekanism holds to those slots) or moving them in place. This is
 * the in-place route: implemented by {@code BasicInventorySlotPositionMixin} and consumed by
 * {@code ExtraRowSlots#positionOutputRow}.
 */
public interface SlotPositionView {

    void mekaddon$moveTo(int x, int y);

    int mekaddon$slotX();

    int mekaddon$slotY();
}
