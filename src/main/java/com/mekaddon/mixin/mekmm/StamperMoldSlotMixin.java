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
 * Lets the "Extra Stack Upgrade" enlarge Mek MM's CNC stamper's <b>mold</b> slot.
 * <p>
 * The stamper has the same shape as the Combining Factory's secondary input: one item input and one extra input that
 * pairs with it (there, the secondary ingredient; here, the mold). Its mold slot is therefore the machine's "extra
 * slot", and it is built through the very same Mekanism call every other input slot is — so swapping in a stack-scaled
 * slot at that point is all it takes. The slot object is created before anything registers it, which is why
 * substituting it here is safe; replacing it afterwards would invalidate the side config and the recipe handlers that
 * hold it.
 * <p>
 * The target and its class are deliberately reached only by name: this config is not-required, so the mod loads (and
 * this class is never resolved) when Mek MM is absent.
 */
@Mixin(targets = "com.jerry.mekmm.common.tile.machine.TileEntityStamper")
public abstract class StamperMoldSlotMixin implements StackScalableTile {

    /** GUI y of the mold slot. The item input sits at y=17, directly above it. */
    private static final int MOLD_Y = 53;

    /**
     * The stamper creates <b>two</b> input slots through this one call — the item input and the mold — so the y
     * coordinate is what tells them apart. Matching on position rather than on an occurrence index keeps this working
     * regardless of the order the two are written in.
     */
    @Redirect(
          method = "getInitialInventory(Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)Lmekanism/common/capabilities/holder/slot/IInventorySlotHolder;",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/inventory/slot/InputInventorySlot;at(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lmekanism/common/inventory/slot/InputInventorySlot;"),
          remap = false
    )
    private InputInventorySlot mekaddon$scaleMoldSlot(Predicate<ItemStack> valid, Predicate<ItemStack> canExtract,
          IContentsListener listener, int x, int y) {
        if (y == MOLD_Y) {
            return StackScaledInputSlot.create((TileEntityMekanism) (Object) this, valid, canExtract, listener, x, y);
        }
        // Declared to return exactly what the redirected call returns, which the vanilla slot is.
        return InputInventorySlot.at(valid, canExtract, listener, x, y);
    }
}
