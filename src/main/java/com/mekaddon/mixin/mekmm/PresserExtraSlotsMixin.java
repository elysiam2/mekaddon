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
 * Lets the "Extra Stack Upgrade" enlarge the CNC presser's two auxiliary inputs.
 * <p>
 * The presser has three inputs: the item being pressed on top (y=16), and the two ingredients that pair with it below
 * it (y=35 and y=54) — the same "one main input plus extra inputs" shape as the combining factory's secondary slot and
 * the stamper's mold. Those two are the machine's extra inputs and the ones the upgrade enlarges; the main input is
 * left exactly as Mekanism's own inputs elsewhere.
 * <p>
 * Both auxiliaries are built through the same call, so — as with the stamper — the y coordinate is what tells them
 * apart, which keeps this independent of the order they are written in. The target and its class are reached only by
 * name, so nothing here resolves when Mek MM is absent.
 */
@Mixin(targets = "com.jerry.mekmm.common.tile.machine.TileEntityPresser")
public abstract class PresserExtraSlotsMixin implements StackScalableTile {

    /** GUI y of the two auxiliary inputs. The item being pressed sits above them, at y=16. */
    private static final int SECONDARY_Y = 35;
    private static final int TERTIARY_Y = 54;

    @Redirect(
          method = "getInitialInventory(Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)Lmekanism/common/capabilities/holder/slot/IInventorySlotHolder;",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/inventory/slot/InputInventorySlot;at(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lmekanism/common/inventory/slot/InputInventorySlot;"),
          remap = false
    )
    private InputInventorySlot mekaddon$scaleExtraInputs(Predicate<ItemStack> valid, Predicate<ItemStack> canExtract,
          IContentsListener listener, int x, int y) {
        if (y == SECONDARY_Y || y == TERTIARY_Y) {
            return StackScaledInputSlot.create((TileEntityMekanism) (Object) this, valid, canExtract, listener, x, y);
        }
        // Declared to return exactly what the redirected call returns, which the vanilla slot is.
        return InputInventorySlot.at(valid, canExtract, listener, x, y);
    }
}
