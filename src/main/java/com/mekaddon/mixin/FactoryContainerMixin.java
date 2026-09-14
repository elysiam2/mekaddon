package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import mekanism.common.inventory.container.tile.FactoryContainer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shifts the player inventory down by the extra row's height so the existing slots still line up with the GUI.
 * <p>
 * This has to be injected here <b>as well as</b> on {@code MekanismContainer}, because {@code FactoryContainer}
 * overrides {@code getInventoryYOffset} outright (85/95/105) and never calls super — so a hook on the base class alone
 * simply never runs for a factory. Without it the GUI grows and the machine's rows move down while the player's
 * inventory stays put, which is exactly what pushed the inventory label into the middle of the player's own slots.
 * The two hooks cannot both fire for the same container, because the override replaces the base method entirely.
 */
@Mixin(FactoryContainer.class)
public abstract class FactoryContainerMixin {

    @Inject(method = "getInventoryYOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$extraRowOffset(CallbackInfoReturnable<Integer> cir) {
        // getTileEntity() is declared on the superclass MekanismTileContainer, so @Shadow (which only resolves members
        // declared on the target class) cannot be used here. Cast instead.
        TileEntityMekanism tile = ((MekanismTileContainer<TileEntityMekanism>) (Object) this).getTileEntity();
        if (tile instanceof CombiningRowTile && ExtraRowState.isUnlocked(tile)) {
            cir.setReturnValue(cir.getReturnValueI() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
        }
    }
}
