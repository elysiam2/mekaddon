package com.mekaddon.slot;

import com.mekaddon.bridge.SlotPositionView;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tier.FactoryTier;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.factory.TileEntityFactory.ProcessInfo;
import mekanism.common.tile.interfaces.ISideConfiguration;

/**
 * Geometry, slot construction and side-config registration for the Combining Factory's upgraded secondary-input row.
 * <p>
 * Without the upgrade the machine has exactly one secondary ("yellow") input slot, shared by every process — see
 * {@code TileEntityCombiningFactory#addSlots}. With the upgrade that single slot is replaced by one per process, laid
 * out in a row below the main inputs, so each column can combine with its own ingredient.
 * <p>
 * Nothing here rebuilds the machine's inventory. The row is <b>appended to and removed from the live holder list</b>
 * (see {@link ExtraRowLayout}); Mekanism hands that same list out to the GUI container
 * ({@code MekanismTileContainer#addSlots} iterates {@code tile.getInventorySlots(null)}) and to item automation.
 * Replacing slot objects instead — as an earlier iteration did — invalidates every reference Mekanism holds
 * (the side-config mappings that give each slot its frame colour, the recipe handlers, the cached recipes), which is
 * exactly what broke the slot colours and left the output slots permanently reporting "no space".
 */
public final class ExtraRowSlots {

    /** Y of the main input row (vanilla). */
    public static final int INPUT_Y = 13;

    /**
     * Y of the secondary input row. Directly below the vanilla input row, so each secondary slot sits under the main
     * input of the same process.
     */
    public static final int SECONDARY_Y = 35;

    /** Y of the output row once the row exists. Shifted down from vanilla y=57 to make room. */
    public static final int OUTPUT_Y = 79;

    /** Vanilla y of the output row. */
    public static final int VANILLA_OUTPUT_Y = 57;

    /** How far the DOWN progress arrow is shifted so it sits between the new row and the shifted output row. */
    public static final int ARROW_Y_OFFSET = OUTPUT_Y - VANILLA_OUTPUT_Y;

    /**
     * Extra GUI height (and player-inventory offset) required when the row is present: one new row (22px) plus the
     * 22px the output row is pushed down, plus 1px slack.
     */
    public static final int EXTRA_GUI_HEIGHT = ARROW_Y_OFFSET + 23;

    private ExtraRowSlots() {
    }

    public static int columnBaseX(FactoryTier tier) {
        return tier == FactoryTier.BASIC ? 55 : tier == FactoryTier.ADVANCED ? 35 : tier == FactoryTier.ELITE ? 29 : 27;
    }

    public static int columnSpacing(FactoryTier tier) {
        return tier == FactoryTier.BASIC ? 38 : tier == FactoryTier.ADVANCED ? 26 : 19;
    }

    public static int columnX(FactoryTier tier, int process) {
        return columnBaseX(tier) + process * columnSpacing(tier);
    }

    /**
     * The machine's <b>live</b> inventory slot list.
     * <p>
     * {@code IInventorySlotHolder#getInventorySlots(null)} is {@code BasicHolder#getSlots(null)}, which returns the
     * internal list itself (side-aware filtering only kicks in for a non-null side). Appending to it is therefore
     * immediately visible to the GUI container and to automation, without touching any existing slot object.
     */
    public static List<IInventorySlot> liveSlots(TileEntityMekanism tile) {
        IInventorySlotHolder holder = HolderAccess.holderOf(tile);
        return holder == null ? null : holder.getInventorySlots(null);
    }

    /** Creates one secondary input slot per process. The slots are not yet part of any holder. */
    public static List<IInventorySlot> createRow(TileEntityCombiningFactory factory, ProcessInfo[] processInfo,
          FactoryRecipeCacheLookupMonitor<?>[] monitors, IContentsListener fallbackListener) {
        FactoryTier tier = ((TileEntityFactory<?>) factory).tier;
        List<IInventorySlot> created = new ArrayList<>(tier.processes);
        for (int i = 0; i < tier.processes; i++) {
            // Use the per-process recipe monitor as the listener so the cached recipe rechecks when the secondary
            // input changes, exactly like the vanilla slot does.
            IContentsListener slotListener = i < monitors.length && monitors[i] instanceof IContentsListener cl
                  ? cl : fallbackListener;
            IInventorySlot mainInput = i < processInfo.length ? processInfo[i].inputSlot() : null;
            // Take the column position from the main input slot that is already there rather than from a per-tier
            // lookup table: Mekanism's own FactoryTier only covers BASIC..ULTIMATE, and addons (Mekanism Extras and
            // friends) add tiers with their own spacing. Reading the existing slot keeps the row aligned for free.
            int x = mainInput instanceof SlotPositionView position
                  ? position.mekaddon$slotX() : columnX(tier, i);
            created.add(AddonExtraInputSlot.create(factory, i, mainInput, slotListener, x, SECONDARY_Y));
        }
        return created;
    }

    /**
     * Moves the output row between its vanilla position and the shifted one.
     * <p>
     * The output slots already exist (they are created once, in the block entity constructor), so their coordinates are
     * changed in place rather than by rebuilding them. The container copies each slot's x/y when it is built, so the
     * caller must run this before the GUI is constructed — which is exactly where {@link ExtraRowLayout#sync} is
     * invoked from.
     */
    public static void positionOutputRow(List<? extends IInventorySlot> outputSlots, boolean rowInstalled) {
        int y = rowInstalled ? OUTPUT_Y : VANILLA_OUTPUT_Y;
        for (IInventorySlot slot : outputSlots) {
            move(slot, y);
        }
    }

    private static void move(IInventorySlot slot, int y) {
        if (slot instanceof SlotPositionView position && position.mekaddon$slotY() != y) {
            position.mekaddon$moveTo(position.mekaddon$slotX(), y);
        }
    }

    /**
     * Registers a set of slots with the machine's item side config as {@link DataType#EXTRA}.
     * <p>
     * This is what gives the slots their gold frame: {@code GuiMekanism#addSlots} resolves each container slot's
     * {@code DataType} through {@code ISideConfiguration#getActiveDataType}, and {@code SlotType#get(DataType)} maps
     * {@code EXTRA} to the gold {@code extra.png}. A slot with no entry at all falls back to plain grey, which is why
     * losing this registration shows up as "the yellow slot lost its colour".
     * <p>
     * Two properties matter and both are handled here:
     * <ul>
     *     <li>{@code ConfigInfo#addSlotInfo} <b>replaces</b> the entry for a {@code DataType}, so a registration that
     *     omitted a slot strips that slot's mapping. The machine's own shared secondary slot is therefore always passed
     *     in by the caller, even while it is out of the holder.</li>
     *     <li>{@code addSlotInfo} also <b>appends</b> the {@code DataType} to each slot's own list, and
     *     {@code getActiveDataType} returns null once that list is as long as the config's set of DataTypes. So
     *     re-registering the same slot over and over would eventually turn it grey too. Slots that already carry
     *     {@code EXTRA} are skipped, which makes this method idempotent and safe to call on every sync.</li>
     * </ul>
     */
    public static void registerConfig(TileEntityMekanism tile, List<? extends IInventorySlot> slots) {
        if (slots.isEmpty() || !(tile instanceof ISideConfiguration side)) {
            return;
        }
        ConfigInfo itemConfig = side.getConfig().getConfig(TransmissionType.ITEM);
        if (itemConfig == null) {
            return;
        }
        List<IInventorySlot> missing = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            if (!itemConfig.getDataTypeForContainer(slot).contains(DataType.EXTRA)) {
                missing.add(slot);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, missing.toArray(new IInventorySlot[0])));
    }
}
