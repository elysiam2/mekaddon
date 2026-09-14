package com.mekaddon.mixin;

import com.mekaddon.bridge.FactoryBaseView;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes a couple of {@link TileEntityFactory} fields to sibling mixins targeting subclasses. Mixin field shadowing
 * only resolves fields declared on the exact target class, so fields like {@code processInfoSlots} must be shadowed here
 * and exposed through {@link FactoryBaseView}.
 */
@Mixin(TileEntityFactory.class)
public abstract class FactoryBaseMixin implements FactoryBaseView {

    @Shadow
    protected TileEntityFactory.ProcessInfo[] processInfoSlots;

    @SuppressWarnings("unchecked")
    @Shadow
    protected FactoryRecipeCacheLookupMonitor<?>[] recipeCacheLookupMonitors;

    @Override
    public TileEntityFactory.ProcessInfo[] mekaddon$getProcessInfoSlots() {
        return processInfoSlots;
    }

    @Override
    public FactoryRecipeCacheLookupMonitor<?>[] mekaddon$getRecipeCacheLookupMonitors() {
        return recipeCacheLookupMonitors;
    }
}
