package com.mekaddon.mixin;

import com.mekaddon.bridge.ItemHandlerManagerView;
import mekanism.common.capabilities.resolver.manager.ItemHandlerManager;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes {@code TileEntityMekanism#itemHandlerManager} so we can reach the live inventory slot holder.
 * <p>
 * {@code @Shadow} cannot be used for this from the {@code TileEntityCombiningFactory} mixin because the field is declared
 * on a superclass; shadowing it in a mixin that targets the declaring class is the supported way in. The value is handed
 * out through {@link ItemHandlerManagerView} so callers need no compile-time dependency on this Mixin class.
 */
@Mixin(TileEntityMekanism.class)
public abstract class TileEntityMekanismAccessor implements ItemHandlerManagerView {

    @Shadow(remap = false)
    protected ItemHandlerManager itemHandlerManager;

    @Override
    public ItemHandlerManager mekaddon$getItemHandlerManager() {
        return itemHandlerManager;
    }
}
