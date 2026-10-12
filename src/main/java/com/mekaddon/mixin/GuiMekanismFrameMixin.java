package com.mekaddon.mixin;

import com.mekaddon.slot.ExtraSecondarySlot;
import mekanism.client.gui.GuiMekanism;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.component.config.DataType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the mod's secondary slots with the gold "extra" frame, whatever the side configuration says.
 * <p>
 * {@code GuiMekanism#addSlots} picks a slot's frame from {@code findDataType(slot)}, which resolves through the
 * machine's {@code ConfigInfo}, and Mekanism only answers with a {@code DataType} while that slot's own DataType list
 * is <b>shorter</b> than the config's set of supported types. Every {@code ConfigInfo#addSlotInfo} call appends to the
 * slot's list, so a slot whose frame depends on the config can silently fall back to the plain grey
 * {@code SlotType.NORMAL} — which is what a "the yellow slot lost its colour" report means.
 * <p>
 * Since the mod knows perfectly well which slots are its own secondary inputs, it answers the question itself instead
 * of depending on that bookkeeping. The fallback is harmless for everything else: {@code super} is untouched.
 */
@Mixin(GuiMekanism.class)
public abstract class GuiMekanismFrameMixin {

    @Inject(
          method = "findDataType(Lmekanism/common/inventory/container/slot/InventoryContainerSlot;)Lmekanism/common/tile/component/config/DataType;",
          at = @At("RETURN"),
          cancellable = true,
          remap = false
    )
    private void mekaddon$forceExtraFrame(InventoryContainerSlot slot, CallbackInfoReturnable<DataType> cir) {
        if (slot.getInventorySlot() instanceof ExtraSecondarySlot) {
            cir.setReturnValue(DataType.EXTRA);
        }
    }
}
