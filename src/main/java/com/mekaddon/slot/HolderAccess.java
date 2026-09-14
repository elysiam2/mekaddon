package com.mekaddon.slot;

import com.mekaddon.bridge.HolderManagerView;
import com.mekaddon.bridge.ItemHandlerManagerView;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.resolver.manager.ItemHandlerManager;
import mekanism.common.tile.base.TileEntityMekanism;
import org.jetbrains.annotations.Nullable;

/**
 * Reaches the live inventory holder of a Mekanism machine.
 * <p>
 * Both bridges are plain interfaces implemented by accessor mixins on classes the tile cannot see from its own type
 * hierarchy ({@code ItemHandlerManager} is a field of {@code TileEntityMekanism}, {@code CapabilityHandlerManager}
 * owns the holder), which keeps this working without any Mixin type dependency at compile time.
 */
public final class HolderAccess {

    private HolderAccess() {
    }

    @Nullable
    public static IInventorySlotHolder holderOf(TileEntityMekanism tile) {
        if (!(tile instanceof ItemHandlerManagerView managerView)) {
            return null;
        }
        ItemHandlerManager manager = managerView.mekaddon$getItemHandlerManager();
        if (!(manager instanceof HolderManagerView holderView)) {
            return null;
        }
        return holderView.mekaddon$getHolderTyped();
    }
}
