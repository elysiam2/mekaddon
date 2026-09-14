package com.mekaddon.mixin.extras;

import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import com.mekaddon.bridge.ExtraFactoryView;
import com.mekaddon.slot.ExtraRowLayout;
import mekanism.api.Upgrade;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Exposes Mekanism Extras' factory internals to the sibling mixin on its Combining Factory.
 * <p>
 * Extras reimplements Mekanism's factory framework instead of extending it — {@code TileEntityExtraFactory} derives
 * from {@code TileEntityConfigurableMachine} — and {@code processInfoSlots} / {@code tier} are declared here, so a
 * mixin on the Combining Factory subclass cannot shadow them. Same reason and same shape as
 * {@code FactoryBaseMixin} on Mekanism's side.
 * <p>
 * The target is given as a string so this mixin is simply skipped when Mekanism Extras is not installed (the config it
 * lives in is marked not-required); the compiled references to Extras types are only ever resolved once the target has
 * been found.
 */
@Mixin(targets = "com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory")
public abstract class ExtraTileEntityFactoryMixin implements ExtraFactoryView {

    @Shadow
    protected TileEntityExtraFactory.ProcessInfo[] processInfoSlots;

    @Shadow
    public ExtraFactoryTier tier;

    @Shadow
    protected FactoryRecipeCacheLookupMonitor<?>[] recipeCacheLookupMonitors;

    @Override
    public Object[] mekaddon$processInfos() {
        return processInfoSlots;
    }

    @Override
    public Object mekaddon$extraTier() {
        return tier;
    }

    @Override
    public Object[] mekaddon$monitors() {
        return recipeCacheLookupMonitors;
    }

    /**
     * Extras' factories declare their own {@code recalculateUpgrades}, so the generic hook on
     * {@code TileEntityMekanism} may not see the call. This one always does; {@code ExtraRowLayout.sync} is
     * idempotent, so firing twice is harmless.
     */
    @Inject(method = "recalculateUpgrades", at = @At("TAIL"), remap = false)
    private void mekaddon$syncSecondaryRow(Upgrade upgrade, CallbackInfo ci) {
        ExtraRowLayout.sync((TileEntityMekanism) (Object) this);
    }
}
