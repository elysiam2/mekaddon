package com.mekaddon.slot;

import com.jerry.mekextras.common.inventory.slot.StackableInputInventorySlot;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.mekaddon.state.ExtraRowState;
import com.mekaddon.state.ExtraStackState;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;

/**
 * Mekanism Extras' own secondary input slot with the mod's "Extra Stack Upgrade" multiplier on top.
 * <p>
 * Extras implements its factory framework independently of Mekanism's, and its secondary slot is a
 * {@link StackableInputInventorySlot} — a slot that already multiplies its limit by tier ({@code limit * 8 << tier}).
 * The multiplier therefore has to be layered on this class rather than on {@link StackScaledInputSlot}, and
 * subclassing is enough: {@code getLimit} is the only thing that decides how much the slot holds.
 * <p>
 * The same class backs both Extras slots — the machine's shared one and the per-process row — distinguished by
 * {@link #shared}: {@link #create} builds the shared slot, {@link #createRow} a row slot. Which of them the menu gets
 * is decided in {@link #createContainerSlot()} from {@code ExtraRowState.isUnlocked}, so Extras' menus follow the same
 * rule as Mekanism's.
 * <p>
 * <b>This class deliberately does not live in {@code com.mekaddon.mixin.extras}.</b> A package named by a mixin
 * config is reserved for mixins — Mixin rejects any direct reference to a class in it with
 * "is in a defined mixin package ... and cannot be referenced directly", which took the mod's whole load down when
 * this class lived there. Being in an ordinary package is harmless for the Extras-absent case: it is only ever
 * reached from {@code ExtraCombiningFactoryMixin}, which itself is only applied when Extras is installed, so nothing
 * loads this class (or its Extras-typed references) otherwise.
 */
public class ExtrasStackedInputSlot extends StackableInputInventorySlot implements ExtraSecondarySlot,
      SecondaryInputSlot {

    /**
     * What the slot is for, which is what decides when it is on screen and when automation may fill it.
     * <p>
     * A boolean was not enough: "not the shared slot" covers two very different cases. The combining factory's row is
     * only live while the upgrade is installed, whereas a machine that merely <i>has</i> extra inputs — the pressing
     * factory's two, for instance — has them from the start and can never install the combining upgrade at all. Read
     * as a boolean, those two came out as "hidden unless upgraded", which is permanently hidden for exactly the
     * machines that have no upgrade to install.
     */
    private enum Role {
        /** The combining factory's own shared secondary slot: steps aside while the row is in charge. */
        SHARED,
        /** One process's secondary slot, part of the row the combining-slot upgrade adds. */
        ROW,
        /** A machine's own extra input, which simply always exists. */
        EXTRA
    }

    private final TileEntityMekanism owner;
    private final Role role;
    private final Predicate<ItemStack> valid;

    /** The machine's own shared secondary slot. */
    public static ExtrasStackedInputSlot create(TileEntityMekanism owner, ExtraFactoryTier tier,
          Predicate<ItemStack> valid, IContentsListener listener, int x, int y) {
        return new ExtrasStackedInputSlot(owner, tier, valid, listener, x, y, Role.SHARED);
    }

    /** One process's secondary slot, part of the row the combining-slot upgrade adds. */
    public static ExtrasStackedInputSlot createRow(TileEntityMekanism owner, ExtraFactoryTier tier,
          Predicate<ItemStack> valid, IContentsListener listener, int x, int y) {
        return new ExtrasStackedInputSlot(owner, tier, valid, listener, x, y, Role.ROW);
    }

    /**
     * An extra input of a machine that is not a Combining Factory — a pressing factory's second and third inputs, for
     * instance. Always visible, always usable, no upgrade involved.
     */
    public static ExtrasStackedInputSlot createExtra(TileEntityMekanism owner, ExtraFactoryTier tier,
          Predicate<ItemStack> valid, IContentsListener listener, int x, int y) {
        return new ExtrasStackedInputSlot(owner, tier, valid, listener, x, y, Role.EXTRA);
    }

    protected ExtrasStackedInputSlot(TileEntityMekanism owner, ExtraFactoryTier tier, Predicate<ItemStack> valid,
          IContentsListener listener, int x, int y, Role role) {
        super(tier, valid, valid, listener, x, y);
        this.owner = owner;
        this.role = role;
        this.valid = valid;
        setSlotType(ContainerSlotType.EXTRA);
    }

    @Override
    public int getLimit(ItemStack stack) {
        return ExtraStackState.scale(owner, super.getLimit(stack), stack);
    }

    /** See {@link SecondaryInputSlot}: the even-distribution pass asks this to know where an ingredient may go. */
    @Override
    public boolean accepts(ItemStack stack) {
        return valid.test(stack);
    }

    @Override
    public InventoryContainerSlot createContainerSlot() {
        // The same answer the window geometry uses, so the menu and the layout can never disagree.
        boolean rowInCharge = ExtraRowState.isUnlocked(owner);
        return switch (role) {
            case EXTRA -> super.createContainerSlot();
            case SHARED -> rowInCharge ? null : super.createContainerSlot();
            case ROW -> rowInCharge ? super.createContainerSlot() : null;
        };
    }

    /** Of the two that trade places, only the one currently in charge may be filled by automation. */
    @Override
    public boolean isItemValidForInsertion(ItemStack stack, AutomationType automationType) {
        if (role != Role.EXTRA && ExtraRowState.isUnlocked(owner) != (role == Role.ROW)) {
            return false;
        }
        return super.isItemValidForInsertion(stack, automationType);
    }
}
