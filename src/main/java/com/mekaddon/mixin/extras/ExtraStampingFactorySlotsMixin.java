package com.mekaddon.mixin.extras;

import com.mekaddon.bridge.StackScalableTile;
import com.mekaddon.slot.StackScaledInputSlot;
import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The stamping half of the Extras pressing/stamping pair: Extras' stamping factories (every tier, one block entity
 * class) with the "Extra Stack Upgrade" multiplier on their shared {@code extraSlot}.
 * <p>
 * Unlike its pressing sibling, this one builds its extra slot through plain Mekanism's
 * {@code InputInventorySlot.at} rather than Extras' own factory, so a {@link StackScaledInputSlot} is the right stand-in
 * here.
 * <p>
 * Target and classes are reached only by name, so nothing here resolves unless both mods are present.
 */
@Mixin(targets = "com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraStampingFactory")
public abstract class ExtraStampingFactorySlotsMixin implements StackScalableTile {

    @Redirect(
          method = "addSlots(Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)V",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/inventory/slot/InputInventorySlot;at(Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lmekanism/common/inventory/slot/InputInventorySlot;"),
          remap = false
    )
    private InputInventorySlot mekaddon$scaleExtraSlot(Predicate<ItemStack> valid, IContentsListener listener, int x,
          int y) {
        return StackScaledInputSlot.create((TileEntityMekanism) (Object) this, valid, listener, x, y);
    }
}
