package com.mekaddon.mixin.extras;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.slot.ExtraRowSlots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Extras half of {@code FactorySortingMixin}: extends Extras' even-distribution pass ("均分") to the mod's
 * per-process secondary slots.
 * <p>
 * Mekanism Extras reimplements the factory framework rather than extending it — {@code TileEntityExtraFactory} is not a
 * {@code TileEntityFactory} — so it has its own {@code sortInventory}, and a hook on Mekanism's class never runs for
 * these machines. That is exactly why an upgraded Extras factory kept its ingredients heaped in a single secondary
 * slot while its main inputs were being spread correctly.
 * <p>
 * Same placement and same reasoning as the Mekanism-side mixin: injected at the tail, so the main inputs are already
 * evenly distributed and each secondary slot's "would you take this?" answer reflects the pairing it now has.
 */
@Mixin(targets = "com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory")
public abstract class ExtraFactorySortingMixin {

    @Inject(method = "sortInventory", at = @At("TAIL"), remap = false)
    private void mekaddon$distributeSecondaryRow(CallbackInfo ci) {
        if ((Object) this instanceof CombiningRowTile row && row.mekaddon$rowActive()) {
            ExtraRowSlots.distributeRow(row);
        }
    }
}
