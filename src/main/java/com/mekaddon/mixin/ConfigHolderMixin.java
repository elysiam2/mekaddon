package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.ConfigHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Puts the mod's per-process secondary slots into the machine's "whole inventory" view while the row is active.
 * <p>
 * This is the one place the row's presence in a menu has to be arranged, and it is arranged at the level the menu
 * actually reads: {@code MekanismTileContainer#addSlots} iterates {@code tile.getInventorySlots(null)}, which for a
 * config-based machine ends up here, in {@code getSlots} with a {@code null} side. Appending at this point — instead of
 * writing into whichever list the holder happens to keep — makes the result independent of the holder's concrete class,
 * of its list being mutable, and of the holder object the mod can reach being the same one the machine serves slots
 * from. (An earlier attempt appended to the holder's own list and silently did nothing in production: the list the
 * container read was not the one the mod was handed, so the row was installed yet invisible.)
 * <p>
 * Only the {@code null}-side view is touched, and only for a machine whose row is active, so:
 * <ul>
 *     <li>the order is deterministic — the row always lands after every vanilla slot, on both sides, so slot indices
 *     still agree between client and server;</li>
 *     <li>side-specific access is untouched: that goes through the machine's {@code ConfigInfo}, which
 *     {@code ExtraRowSlots#registerConfig} keeps up to date.</li>
 * </ul>
 */
@Mixin(ConfigHolder.class)
public abstract class ConfigHolderMixin {

    /** The machine this holder belongs to — the only way back from a holder to its tile. */
    @Shadow
    @Final
    private ISideConfiguration sideConfiguration;

    @Inject(
          method = "getSlots(Lnet/minecraft/core/Direction;Ljava/util/function/Function;)Ljava/util/List;",
          at = @At("RETURN"),
          cancellable = true,
          remap = false
    )
    private void mekaddon$appendActiveRow(Direction side, Function<?, ?> getter,
          CallbackInfoReturnable<List<IInventorySlot>> cir) {
        if (side != null) {
            return;
        }
        // ConfigHolder is the base of EVERY holder Mekanism has - items, energy, chemicals, fluids, heat. Appending
        // slots to an energy container list is not merely useless, it is fatal: Mekanism indexes that list as
        // IEnergyContainer (and a chemical list as IChemicalTank), so one item slot in it makes the machine's own GUI
        // and every Jade tooltip throw ClassCastException. Only the item holder may be touched.
        if (!((Object) this instanceof IInventorySlotHolder)) {
            return;
        }
        if (!(sideConfiguration instanceof CombiningRowTile row) || !row.mekaddon$rowActive()) {
            return;
        }
        List<IInventorySlot> withRow = row.mekaddon$secondarySlots();
        List<IInventorySlot> base = cir.getReturnValue();
        if (base == null || withRow.isEmpty() || base.containsAll(withRow)) {
            return;
        }
        List<IInventorySlot> merged = new ArrayList<>(base.size() + withRow.size());
        merged.addAll(base);
        merged.addAll(withRow);
        cir.setReturnValue(merged);
    }
}
