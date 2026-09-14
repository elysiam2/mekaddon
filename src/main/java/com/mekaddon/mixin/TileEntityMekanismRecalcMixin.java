package com.mekaddon.mixin;

import com.mekaddon.slot.ExtraRowLayout;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a Combining Factory's inventory in step with the installed upgrades.
 * <p>
 * {@code TileComponentUpgrade} calls {@code recalculateUpgrades(type)} right after it changes an upgrade's count, both
 * when installing and when uninstalling, so hooking it here means the secondary row appears and disappears at exactly
 * the right moment — including during chunk load, where the count is restored from the upgrade NBT.
 * <p>
 * {@code recalculateUpgrades} is declared on {@link TileEntityMekanism}, so this one hook covers Mekanism's factories,
 * any addon factory that does not override it, and every override that chains to super. Addons that override it without
 * chaining get their own hook from their own integration mixin — {@code ExtraRowLayout.sync} is idempotent, so the
 * extra call is harmless.
 */
@Mixin(TileEntityMekanism.class)
public abstract class TileEntityMekanismRecalcMixin {

    @Inject(method = "recalculateUpgrades", at = @At("TAIL"), remap = false)
    private void mekaddon$syncSecondaryRow(Upgrade upgrade, CallbackInfo ci) {
        ExtraRowLayout.sync((TileEntityMekanism) (Object) this);
    }
}
