package com.mekaddon.mixin.extras;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shifts the player inventory down by the extra row's height for Mekanism Extras' factory containers.
 * <p>
 * The same job {@code FactoryContainerMixin} does for Mekanism's own factories, and needed for the same reason:
 * Extras' {@code ExtraFactoryContainer} overrides {@code getInventoryYOffset} outright and never calls super, so the
 * hook on the shared base class ({@code MekanismContainerMixin}) never runs for it. Without this one the machine's
 * rows move down while the player's inventory stays put, and the inventory is then drawn on top of the output row -
 * precisely the "everything is misaligned" report that comes with Extras or Mek Energistics factory screens (Mek
 * Energistics' own factory menus extend this container, so they are covered too).
 * <p>
 * The target is a string and the config is not-required, so this compiles and loads without Extras installed.
 */
@Mixin(targets = "com.jerry.mekextras.common.inventory.container.tile.ExtraFactoryContainer")
public abstract class ExtraFactoryContainerMixin {

    @Inject(method = "getInventoryYOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$extraRowOffset(CallbackInfoReturnable<Integer> cir) {
        // getTileEntity() is declared on MekanismTileContainer, the superclass, so @Shadow cannot reach it here.
        TileEntityMekanism tile = ((MekanismTileContainer<TileEntityMekanism>) (Object) this).getTileEntity();
        if (tile instanceof CombiningRowTile && ExtraRowState.isUnlocked(tile)) {
            cir.setReturnValue(cir.getReturnValueI() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
        }
    }
}
