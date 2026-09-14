package com.mekaddon.bridge;

import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;

/**
 * Duck-typed view onto Mekanism's inventory holder manager, implemented by
 * {@code CapabilityHandlerManagerAccessor} on {@code CapabilityHandlerManager}.
 * <p>
 * A plain interface is used (instead of casting to the Mixin class) so that normal code can read the holder without
 * referencing a Mixin type at compile time.
 */
public interface HolderManagerView {

    /** The inventory slot holder this manager resolves item capabilities through. */
    IInventorySlotHolder mekaddon$getHolderTyped();
}
