package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shifts the player inventory down by the extra row's height so the existing slots still line up with the GUI.
 * <p>
 * {@code MekanismContainer#getInventoryYOffset} is the base for everyone (84), and {@code addInventorySlots} uses it.
 * Mekanism's own {@code FactoryContainer} overrides it without calling super (85/95/105), so patching the base here
 * covers containers that do <b>not</b> override it — notably the plain {@code MekanismTileContainer} an addon's
 * factory uses — while never double-counting for {@code FactoryContainer}, whose override replaces the base entirely.
 */
@Mixin(MekanismContainer.class)
public abstract class MekanismContainerMixin {

    @Inject(method = "getInventoryYOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$extraRowOffset(CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof MekanismTileContainer<?> container)) {
            return;
        }
        TileEntityMekanism tile = container.getTileEntity();
        if (tile instanceof CombiningRowTile && ExtraRowState.isUnlocked(tile)) {
            cir.setReturnValue(cir.getReturnValueI() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
        }
    }
}
