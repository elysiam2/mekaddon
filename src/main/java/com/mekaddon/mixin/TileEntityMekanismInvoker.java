package com.mekaddon.mixin;

import mekanism.api.IContentsListener;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Lets the Combining Factory mixin re-run Mekanism's own inventory-holder construction.
 * <p>
 * Both methods are {@code protected} on {@link TileEntityMekanism}. {@code @Shadow} cannot reach them from a
 * {@code TileEntityCombiningFactory} mixin because {@code @Shadow} only resolves members declared on the target class
 * itself, so an {@code @Invoker} mixin on the declaring class is the supported alternative.
 */
@Mixin(TileEntityMekanism.class)
public interface TileEntityMekanismInvoker {

    @Invoker("getInitialInventory")
    IInventorySlotHolder mekaddon$invokeGetInitialInventory(IContentsListener listener);

    @Invoker("getListener")
    IContentsListener mekaddon$invokeGetListener(ContainerType<?, ?, ?> type, IContentsListener saveOnlyListener);
}
