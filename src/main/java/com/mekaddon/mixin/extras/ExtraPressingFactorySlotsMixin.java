package com.mekaddon.mixin.extras;

import com.jerry.mekextras.common.inventory.slot.StackableInputInventorySlot;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.mekaddon.bridge.StackScalableTile;
import com.mekaddon.slot.ExtrasStackedInputSlot;
import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mekanism Extras' pressing factories (Absolute, Cosmic, … — they all share this one block entity class, so every tier
 * is covered at once) with the "Extra Stack Upgrade" multiplier on their two shared auxiliary inputs.
 * <p>
 * Extras' version of {@code TileEntityPressingFactory}: the per-process main inputs come from its parent, and
 * {@code secondarySlot} (y=57) and {@code tertiarySlot} (y=37) are the extras — the same two the Mek MM original gets.
 * They are built through Extras' own slot factory, so they are substituted with {@link ExtrasStackedInputSlot}, which
 * layers the multiplier on top of Extras' tier scaling.
 * <p>
 * Target and classes are reached only by name, so nothing here resolves unless both mods are present.
 */
@Mixin(targets = "com.jerry.mekextras.common.integration.mekmm.tile.factory.TileEntityExtraPressingFactory")
public abstract class ExtraPressingFactorySlotsMixin implements StackScalableTile {

    @Redirect(
          method = "addSlots(Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)V",
          at = @At(value = "INVOKE",
                target = "Lcom/jerry/mekextras/common/inventory/slot/StackableInputInventorySlot;at(Lcom/jerry/mekextras/common/tier/ExtraFactoryTier;Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lcom/jerry/mekextras/common/inventory/slot/StackableInputInventorySlot;"),
          remap = false
    )
    private StackableInputInventorySlot mekaddon$scaleExtraSlots(ExtraFactoryTier tier, Predicate<ItemStack> valid,
          IContentsListener listener, int x, int y) {
        // Declared to return exactly what the redirected call returns (the instance is a subclass).
        // createExtra, not createRow: these two are the machine's own extra inputs and always exist — they are not the
        // combining factory's per-process row, which only appears with an upgrade these machines cannot install.
        return ExtrasStackedInputSlot.createExtra((TileEntityMekanism) (Object) this, tier, valid, listener, x, y);
    }
}
