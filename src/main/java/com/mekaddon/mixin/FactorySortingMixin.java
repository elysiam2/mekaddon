package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.slot.ExtraRowSlots;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends Mekanism's even-distribution pass ("均分") to the mod's per-process secondary slots.
 * <p>
 * {@code sortInventory} spreads each distinct input item evenly across the process input slots and only ever looks at
 * {@code processInfoSlots[i].inputSlot()} — the main inputs. The vanilla Combining Factory gets away with that because
 * its secondary slot is shared by every process; once each process owns its own secondary slot, ingredients put into
 * one of them stay there (one slot full, the rest empty) unless something spreads them too.
 * <p>
 * Injecting at the tail rather than replacing anything means the main inputs are already evenly distributed by the time
 * the row is handled, which is what makes each secondary slot's "would you take this?" answer — it depends on the main
 * input of the same process — the right one to ask. Only runs for machines that have the row active; everything else
 * takes the original path untouched.
 */
@Mixin(TileEntityFactory.class)
public abstract class FactorySortingMixin {

    @Inject(method = "sortInventory", at = @At("TAIL"), remap = false)
    private void mekaddon$distributeSecondaryRow(CallbackInfo ci) {
        if ((Object) this instanceof CombiningRowTile row && row.mekaddon$rowActive()) {
            ExtraRowSlots.distributeRow(row);
        }
    }
}
