package com.mekaddon.slot;

import com.mekaddon.state.ExtraStackState;
import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An {@link InputInventorySlot} whose stack limit is multiplied by the machine's "Extra Stack Upgrade" level
 * ({@code x2^installed}, up to {@code x256}).
 * <p>
 * It exists because a slot's limit is decided by the slot itself: {@code getLimit} is the single place Mekanism asks
 * how much a slot will hold — {@code insertItem}, {@code setStackSize} and a container's {@code getMaxStackSize} all
 * go through it — so a slot that answers differently is the whole implementation, with no state to keep in sync and no
 * machine-side patching.
 * <p>
 * A machine that has a secondary ("extra") input gets one of these in place of the slot it would build itself, by way
 * of an <code>@Redirect</code> at that slot's creation site. Creating it there rather than replacing it later is what
 * keeps every reference Mekanism holds to the slot — side config, recipe handlers, cached recipes — valid.
 * <p>
 * Slots that need the combining-slot upgrade's behaviour on top (staying out of the menu while the per-process row is
 * in charge, for instance) subclass this; see {@link CombiningSharedSecondarySlot}.
 */
public class StackScaledInputSlot extends InputInventorySlot implements SecondaryInputSlot {

    private final TileEntityMekanism owner;
    private final Predicate<ItemStack> valid;

    /** For slots whose validation and extraction rules are the same, which is the common case. */
    public static StackScaledInputSlot create(TileEntityMekanism owner, Predicate<ItemStack> valid,
          @Nullable IContentsListener listener, int x, int y) {
        return new StackScaledInputSlot(owner, valid, valid, listener, x, y);
    }

    public static StackScaledInputSlot create(TileEntityMekanism owner, Predicate<ItemStack> valid,
          Predicate<ItemStack> canExtract, @Nullable IContentsListener listener, int x, int y) {
        return new StackScaledInputSlot(owner, valid, canExtract, listener, x, y);
    }

    protected StackScaledInputSlot(TileEntityMekanism owner, Predicate<ItemStack> valid,
          Predicate<ItemStack> canExtract, @Nullable IContentsListener listener, int x, int y) {
        super(valid, canExtract, listener, x, y);
        this.owner = owner;
        this.valid = valid;
    }

    @Override
    public int getLimit(ItemStack stack) {
        return ExtraStackState.scale(owner, super.getLimit(stack), stack);
    }

    /**
     * Whether this slot would take {@code stack}, ignoring how full it currently is — see {@link SecondaryInputSlot}.
     */
    @Override
    public final boolean accepts(ItemStack stack) {
        return valid.test(stack);
    }

    /** The machine this slot belongs to — what the stack multiplier is read from. */
    protected final TileEntityMekanism owner() {
        return owner;
    }
}
