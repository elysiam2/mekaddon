package com.mekaddon.mixin;

import com.mekaddon.bridge.HolderManagerView;
import mekanism.common.capabilities.holder.IHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.resolver.manager.CapabilityHandlerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the slot holder a {@code CapabilityHandlerManager} resolves item capabilities through.
 * <p>
 * Two constraints shape this class, both learned from a production crash:
 * <ul>
 *     <li>the target is a <b>class</b>, so this must be an abstract class mixin — an interface mixin fails with
 *     {@code @Mixin target type mismatch};</li>
 *     <li>the manager's {@code holder} field is <b>generic</b> ({@code HOLDER holder}), and Mixin resolves {@code @Shadow}
 *     fields by exact erased descriptor, so shadowing it fails in production with "was not located in the target class".
 *     Shadowing the existing non-generic {@code getHolder()} method by name sidesteps that entirely.</li>
 * </ul>
 */
@Mixin(CapabilityHandlerManager.class)
public abstract class CapabilityHandlerManagerAccessor implements HolderManagerView {

    @Shadow(remap = false)
    protected abstract IHolder getHolder();

    @Override
    public IInventorySlotHolder mekaddon$getHolderTyped() {
        IHolder holder = getHolder();
        return holder instanceof IInventorySlotHolder slotHolder ? slotHolder : null;
    }
}
