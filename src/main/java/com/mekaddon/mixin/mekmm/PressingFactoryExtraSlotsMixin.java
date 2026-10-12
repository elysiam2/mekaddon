package com.mekaddon.mixin.mekmm;

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
 * The factory version of {@link PresserExtraSlotsMixin}: Mek MM's pressing factories (every tier — they share this one
 * block entity class) keep two shared extra inputs, {@code secondarySlot} and {@code tertiarySlot}, alongside the
 * per-process main inputs their parent builds. Both are created through the same call in {@code addSlots}, so the
 * redirect covers them together.
 * <p>
 * The target and its class are reached only by name, so nothing here resolves when Mek MM is absent.
 */
@Mixin(targets = "com.jerry.mekmm.common.tile.factory.TileEntityPressingFactory")
public abstract class PressingFactoryExtraSlotsMixin implements StackScalableTile {

    @Redirect(
          method = "addSlots(Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)V",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/inventory/slot/InputInventorySlot;at(Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lmekanism/common/inventory/slot/InputInventorySlot;"),
          remap = false
    )
    private InputInventorySlot mekaddon$scaleExtraSlots(Predicate<ItemStack> valid, IContentsListener listener, int x,
          int y) {
        return StackScaledInputSlot.create((TileEntityMekanism) (Object) this, valid, listener, x, y);
    }
}
