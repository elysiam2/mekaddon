package com.mekaddon.mixin.mekajadeupgrade;

import com.mekaddon.registry.ModItems;
import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/**
 * Compatibility with MekaJadeUpgrades ({@code mekajadeupgrade}), the addon that lists a machine's installed upgrades on
 * its Jade tooltip.
 * <p>
 * That mod renders each installed upgrade as its item icon plus a count, and resolves the icon through its own
 * {@code UpgradeProvider#getUpgrade} — a lookup that only knows Mekanism's seven built-in upgrades. For the mod's own
 * constants the lookup fails, and the provider's {@code catch} draws a vanilla barrier instead: the red "no entry" icon
 * that shows up next to a machine that has one of this mod's upgrades installed.
 * <p>
 * This mod knows the answer, so it supplies it: the icon is built from the real item, with the same size and placement
 * the provider uses for its own icons, and everything else (the count text beside it, the ordering) is left to the
 * provider. Only the mod's two upgrades are touched; vanilla upgrades take the original path untouched.
 * <p>
 * The target is a string and the config is not-required, so neither this class nor its Jade imports exist when either
 * mod is absent.
 */
@Mixin(targets = "com.devdyna.mekajadeupgrade.client.provider.UpgradeProvider")
public abstract class ModUpgradeIconMixin {

    @Inject(
          method = "item(Lmekanism/api/Upgrade;I)Lsnownee/jade/api/ui/IElement;",
          at = @At("HEAD"),
          cancellable = true,
          remap = false
    )
    private void mekaddon$modUpgradeIcon(Upgrade upgrade, int count, CallbackInfoReturnable<IElement> cir) {
        Item item;
        if (ModUpgrades.isCombining(upgrade)) {
            item = ModItems.COMBINING_SLOT_UPGRADE.get();
        } else if (ModUpgrades.isExtraStack(upgrade)) {
            item = ModItems.EXTRA_STACK_UPGRADE.get();
        } else {
            return;
        }
        // Mirrors the provider's own element: 0.55 scale, a 10x10 box shifted by the running x offset, and no hover
        // message (its own icons pass null as well).
        cir.setReturnValue(IElementHelper.get().item(new ItemStack(item), 0.55F)
              .size(new Vec2(10.0F, 10.0F))
              .translate(new Vec2(count, -2.0F))
              .message(null));
    }
}
