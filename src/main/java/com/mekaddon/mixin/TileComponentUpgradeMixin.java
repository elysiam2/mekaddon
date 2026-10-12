package com.mekaddon.mixin;

import com.mekaddon.bridge.InsertGuardView;
import com.mekaddon.registry.ModUpgrades;
import com.mekaddon.state.UpgradeConflicts;
import java.util.LinkedHashSet;
import java.util.Set;
import mekanism.api.Upgrade;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enforces that a machine carries at most one of the mod's two upgrades.
 * <p>
 * The component is the one object that knows both the machine and its upgrade slot, which makes it the natural place
 * to hang the rule on. Three separate hooks, because they answer three different questions:
 * <ul>
 *     <li><b>{@code <init>}</b> — hands the upgrade slot a guard predicate. The slot's own validator is fixed when it
 *     is built (it captures the machine's supported-upgrade set), so it cannot express a rule that depends on what is
 *     currently installed; the guard is consulted live on every prospective insertion instead. This is what makes the
 *     item genuinely impossible to put in the upgrade slot, by any path.</li>
 *     <li><b>{@code supports}</b> — stops {@code tickServer} from installing the conflicting item even if it is already
 *     sitting in the upgrade slot, which is the one gap the insertion guard cannot close: a player can park the item
 *     there <i>before</i> the other upgrade finishes installing.</li>
 *     <li><b>{@code getSupportedTypes}</b> — the set the upgrade window builds its <i>available upgrades</i> row
 *     from, so the conflicting upgrade simply is not offered while it cannot be installed.</li>
 * </ul>
 * {@code addUpgrades} gets a hook as well for the same reason {@code supports} does, covering the vanilla path where
 * an upgrade item is applied by right-clicking rather than by filling the slot.
 * <p>
 * Everything the rule needs is derived from the currently installed counts, so nothing has to be kept in step: install
 * one, and the other becomes unavailable immediately; uninstall it, and the other is available again.
 */
@Mixin(TileComponentUpgrade.class)
public abstract class TileComponentUpgradeMixin {

    @Shadow
    private TileEntityMekanism tile;

    @Shadow
    private UpgradeInventorySlot upgradeSlot;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void mekaddon$installConflictGuard(TileEntityMekanism tile, CallbackInfo ci) {
        // Captured once so the guard does not read a field of a possibly-not-yet-initialised object later on.
        TileEntityMekanism owner = this.tile;
        if (upgradeSlot instanceof InsertGuardView guard) {
            guard.mekaddon$setInsertGuard(stack -> UpgradeConflicts.blocks(owner, stack));
        }
    }

    @Inject(method = "supports", at = @At("HEAD"), cancellable = true, remap = false)
    private void mekaddon$suppressConflictingUpgrade(Upgrade upgrade, CallbackInfoReturnable<Boolean> cir) {
        if (UpgradeConflicts.suppressed((TileComponentUpgrade) (Object) this, upgrade)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getSupportedTypes", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$filterSupportedTypes(CallbackInfoReturnable<Set<Upgrade>> cir) {
        Set<Upgrade> base = cir.getReturnValue();
        if (base == null || base.isEmpty()) {
            return;
        }
        TileComponentUpgrade self = (TileComponentUpgrade) (Object) this;
        Set<Upgrade> filtered = null;
        for (Upgrade upgrade : base) {
            if (!ModUpgrades.isModUpgrade(upgrade) || !UpgradeConflicts.suppressed(self, upgrade)) {
                continue;
            }
            if (filtered == null) {
                filtered = new LinkedHashSet<>(base);
            }
            filtered.remove(upgrade);
        }
        if (filtered != null) {
            cir.setReturnValue(Set.copyOf(filtered));
        }
    }

    @Inject(method = "addUpgrades(Lmekanism/api/Upgrade;I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void mekaddon$refuseConflictingInstall(Upgrade upgrade, int amount, CallbackInfoReturnable<Integer> cir) {
        if (UpgradeConflicts.installedConflict(this.tile, upgrade) != null) {
            // Zero means "nothing was installed", and Mekanism then leaves the item where it came from.
            cir.setReturnValue(0);
        }
    }
}
