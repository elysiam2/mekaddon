package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.StackScalableTile;
import com.mekaddon.registry.ModUpgrades;
import java.util.LinkedHashSet;
import java.util.Set;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes every supported Combining Factory advertise the mod's upgrades.
 * <p>
 * {@code TileEntityMekanism#getSupportedUpgrade} reads the set straight off the block's {@code AttributeUpgradeSupport}
 * attribute, and that set drives three things that all have to agree: which items
 * {@code UpgradeInventorySlot#input(listener, supported)} accepts, which entries the upgrade window lists, and which
 * counts {@code TileComponentUpgrade} keeps in its installed map. Adding the constants at RETURN keeps the attribute
 * untouched.
 * <p>
 * Gating on {@link CombiningRowTile} rather than on a concrete tile type is what makes optional mod support work
 * without any compile-time dependency: whichever mod's Combining Factory implements that marker gets the upgrades.
 * <p>
 * Both of the mod's upgrades are gated the same way on purpose. The stack upgrade only ever multiplies the limit of the
 * per-process secondary slots the other upgrade creates, so offering it on any other machine would just waste an
 * upgrade slot.
 */
@Mixin(TileEntityMekanism.class)
public abstract class UpgradeSupportMixin {

    @Unique
    private Set<Upgrade> mekaddon$supportedWithModUpgrades;

    @Inject(method = "getSupportedUpgrade", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$supportModUpgrades(CallbackInfoReturnable<Set<Upgrade>> cir) {
        Upgrade combining = ModUpgrades.combiningSlots();
        Upgrade stack = ModUpgrades.extraStack();
        // Which upgrades a machine gets depends on what it can actually do with them: a Combining Factory can trade its
        // shared secondary slot for a per-process row (so it gets both, and the mutual-exclusion rule then applies),
        // while a machine that only has an extra input to enlarge - the CNC stamper, the stamping factory - gets the
        // stack upgrade alone.
        boolean hasRow = (Object) this instanceof CombiningRowTile;
        boolean scalable = (Object) this instanceof StackScalableTile;
        if ((combining == null && stack == null) || (!hasRow && !scalable)) {
            return;
        }
        Set<Upgrade> base = cir.getReturnValue();
        // Empty means this block does not support upgrades at all - leave that decision alone.
        if (base == null || base.isEmpty()) {
            return;
        }
        if (mekaddon$supportedWithModUpgrades == null) {
            Set<Upgrade> extended = new LinkedHashSet<>(base);
            if (combining != null && hasRow) {
                extended.add(combining);
            }
            if (stack != null) {
                extended.add(stack);
            }
            mekaddon$supportedWithModUpgrades = Set.copyOf(extended);
            // One line per machine, logged once: this is the decision that makes the upgrade slot accept the items, so
            // it is the first thing to look for when an install does nothing and consumes nothing.
            com.mekaddon.MekAddon.LOGGER.info("{} now advertises the mod's upgrades (supported: {})",
                  ((Object) this).getClass().getSimpleName(), mekaddon$supportedWithModUpgrades.size());
        }
        cir.setReturnValue(mekaddon$supportedWithModUpgrades);
    }
}
