package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.registry.ModUpgrades;
import java.util.HashSet;
import java.util.Set;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes every supported Combining Factory advertise the mod's upgrade.
 * <p>
 * {@code TileEntityMekanism#getSupportedUpgrade} reads the set straight off the block's {@code AttributeUpgradeSupport}
 * attribute, and that set drives three things that all have to agree: which items
 * {@code UpgradeInventorySlot#input(listener, supported)} accepts, which entries the upgrade window lists, and which
 * counts {@code TileComponentUpgrade} keeps in its installed map. Adding the constant at RETURN keeps the attribute
 * untouched.
 * <p>
 * Gating on {@link CombiningRowTile} rather than on a concrete tile type is what makes optional mod support work
 * without any compile-time dependency: whichever mod's Combining Factory implements that marker gets the upgrade.
 */
@Mixin(TileEntityMekanism.class)
public abstract class UpgradeSupportMixin {

    @Unique
    private Set<Upgrade> mekaddon$supportedWithCombining;

    @Inject(method = "getSupportedUpgrade", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$supportCombiningSlotUpgrade(CallbackInfoReturnable<Set<Upgrade>> cir) {
        if (ModUpgrades.combiningSlots() == null || !((Object) this instanceof CombiningRowTile)) {
            return;
        }
        Set<Upgrade> base = cir.getReturnValue();
        // Empty means this block does not support upgrades at all - leave that decision alone.
        if (base == null || base.isEmpty()) {
            return;
        }
        if (mekaddon$supportedWithCombining == null) {
            Set<Upgrade> extended = new HashSet<>(base);
            extended.add(ModUpgrades.combiningSlots());
            mekaddon$supportedWithCombining = Set.copyOf(extended);
            // One line per machine, logged once: this is the decision that makes the upgrade slot accept the item, so
            // it is the first thing to look for when an install does nothing and consumes nothing.
            com.mekaddon.MekAddon.LOGGER.info("{} now advertises the '{}' upgrade (supported: {})",
                  ((Object) this).getClass().getSimpleName(), ModUpgrades.NAME, mekaddon$supportedWithCombining.size());
        }
        cir.setReturnValue(mekaddon$supportedWithCombining);
    }
}
