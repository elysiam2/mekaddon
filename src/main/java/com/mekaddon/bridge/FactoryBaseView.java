package com.mekaddon.bridge;

import mekanism.api.IContentsListener;
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

    /**
     * Wraps a listener the way the machine wraps the ones it hands its own input slots: the inner listener still runs,
     * every process monitor is unpaused, and — the part that matters here — the machine marks its inventory as needing
     * to be sorted again.
     * <p>
     * The mod's secondary slots have to go through this too. They are ordinary inputs, so putting an ingredient into
     * one has to schedule the same even-distribution pass a vanilla input slot would; otherwise the machine silently
     * stops redistributing as soon as the extra row exists.
     */
    IContentsListener mekaddon$markAllMonitors(IContentsListener listener);
}
