package com.mekaddon.mixin;

import com.mekaddon.registry.ModItems;
import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Teaches {@link UpgradeUtils} about the mod's upgrade constants.
 * <p>
 * {@code UpgradeUtils#getItem} is an exhaustive {@code tableswitch} over {@link Upgrade#ordinal()} that throws
 * {@code MatchException} for anything past {@code STONE_GENERATOR}, and it is what turns an upgrade type back into an
 * item stack — used both to draw the icons in the upgrade window and, crucially, by
 * {@code TileComponentUpgrade#removeUpgrade} when the player uninstalls an upgrade. Intercepting it is what makes the
 * uninstall give the mod's items back instead of throwing.
 */
@Mixin(UpgradeUtils.class)
public abstract class UpgradeUtilsMixin {

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mekaddon$modUpgradeItem(Upgrade upgrade, CallbackInfoReturnable<Holder<Item>> cir) {
        if (ModUpgrades.isCombining(upgrade)) {
            cir.setReturnValue(ModItems.COMBINING_SLOT_UPGRADE);
        } else if (ModUpgrades.isExtraStack(upgrade)) {
            cir.setReturnValue(ModItems.EXTRA_STACK_UPGRADE);
        }
    }
}
