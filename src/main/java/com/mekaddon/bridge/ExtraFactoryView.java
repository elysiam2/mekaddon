package com.mekaddon.bridge;

/**
 * Reaches the internals of Mekanism Extras' factory base class.
 * <p>
 * Extras reimplements Mekanism's factory framework rather than extending it ({@code TileEntityExtraFactory extends
 * TileEntityConfigurableMachine}), and its {@code processInfoSlots} / {@code tier} are declared there, so a mixin on
 * the Combining Factory subclass cannot shadow them. This bridge is implemented by a mixin on the base class and is
 * deliberately typed in terms of {@code Object} so the shared code — and this interface — compile without any
 * dependency on the optional Extras jar.
 */
public interface ExtraFactoryView {

    /** Extras' {@code ProcessInfo[]}; callers cast the elements. */
    Object[] mekaddon$processInfos();

    /** Extras' {@code ExtraFactoryTier}; callers cast it. */
    Object mekaddon$extraTier();

    /** Extras' per-process recipe monitors, one per process. */
    Object[] mekaddon$monitors();
}
