package com.mekaddon.bridge;

import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.factory.TileEntityFactory;

/**
 * Read-only view of {@link TileEntityFactory} fields that the mod needs but that are declared there rather than on the
 * Combining Factory. Implemented by {@link com.mekaddon.mixin.FactoryBaseMixin} and consumed by
 * {@link com.mekaddon.mixin.CombiningFactoryExtraSlotsMixin}.
 * <p>
 * Mixin field shadowing only resolves fields declared on the exact target class, so these have to be exposed from a
 * mixin on {@code TileEntityFactory} itself.
 */
public interface FactoryBaseView {

    TileEntityFactory.ProcessInfo[] mekaddon$getProcessInfoSlots();

    FactoryRecipeCacheLookupMonitor<?>[] mekaddon$getRecipeCacheLookupMonitors();
}
