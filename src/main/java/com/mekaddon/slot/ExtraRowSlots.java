package com.mekaddon.slot;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.FactoryBaseView;
import com.mekaddon.bridge.SlotPositionView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import mekanism.common.tile.component.config.slot.ISlotInfo;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import mekanism.common.tile.factory.TileEntityFactory;
import mekanism.common.tile.factory.TileEntityFactory.ProcessInfo;
import mekanism.common.tile.interfaces.ISideConfiguration;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.ItemStackMap;
import org.jetbrains.annotations.Nullable;

/**
 * Geometry, slot construction and side-config registration for the Combining Factory's upgraded secondary-input row.
 * <p>
 * Without the upgrade the machine has exactly one secondary ("yellow") input slot, shared by every process — see
 * {@code TileEntityCombiningFactory#addSlots}. With the upgrade that slot steps aside and one secondary input per
 * process takes over, laid out in a row below the main inputs, so each column can combine with its own ingredient.
 * <p>
 * Both sets of slots are created once, when the machine is built, and registered with the machine's real
 * {@code InventorySlotHelper} — the same path every vanilla slot takes, which is what puts them into the holder's slot
 * list <i>and</i> its per-side lists. Which of them the menu shows is decided later, per container build, by the slots
 * themselves; see {@code ExtraRowLayout} for why that split matters.
 */
public final class ExtraRowSlots {

    /** Y of the main input row (vanilla). */
    public static final int INPUT_Y = 13;

    /**
     * How far the whole machine content moves down once the row exists.
     * <p>
     * The window grows by {@link #EXTRA_GUI_HEIGHT} to make room for the row, and Minecraft centres a screen on its
     * image height — so growing it also lifts everything on screen by half the difference. Dropping the content by a
     * few pixels when the row appears puts the title back at a comfortable distance from the first slot row instead of
     * leaving it sitting on top of it. Small on purpose: this is a nudge, not a redesign.
     */
    public static final int CONTENT_Y_SHIFT = 8;

    /**
     * Y of the secondary input row, directly below the (shifted) vanilla input row, so each secondary slot sits under
     * the main input of the same process.
     */
    public static final int SECONDARY_Y = INPUT_Y + CONTENT_Y_SHIFT + 22;

    /** Y of the output row once the row exists. Shifted down from vanilla y=57 to make room. */
    public static final int OUTPUT_Y = 57 + CONTENT_Y_SHIFT + 22;

    /** Vanilla y of the output row. */
    public static final int VANILLA_OUTPUT_Y = 57;

    /** How far the DOWN progress arrow is shifted so it sits between the new row and the shifted output row. */
    public static final int ARROW_Y_OFFSET = OUTPUT_Y - VANILLA_OUTPUT_Y;

    /**
     * Extra GUI height (and player-inventory offset) required when the row is present: the new row (22px), the 22px
     * the output row is pushed down, the {@link #CONTENT_Y_SHIFT} the whole content drops by, plus 1px slack.
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
     * The machine's inventory holder, for diagnostics: what the holder itself reports as its slot list.
     * <p>
     * Nothing in the layout depends on this any more — the row's presence is decided by the slots themselves, not by
     * list membership — but comparing the holder's view against what the mod just made live is the cheapest way to
     * notice that a machine's inventory is served by something other than the holder (a wrapper that returns a
     * filtered copy), which is exactly the failure mode that once left the row invisible while it was "installed".
     */
    @Nullable
    public static List<IInventorySlot> liveSlots(TileEntityMekanism tile) {
        IInventorySlotHolder holder = HolderAccess.holderOf(tile);
        return holder == null ? null : holder.getInventorySlots(null);
    }

    /**
     * Creates one secondary input slot per process. The slots are not part of any holder: they are offered to a menu by
     * {@code ConfigHolderMixin}, which appends them to the whole-inventory view the menu reads while the row is active.
     */
    public static List<IInventorySlot> createRow(TileEntityCombiningFactory factory, ProcessInfo[] processInfo,
          FactoryRecipeCacheLookupMonitor<?>[] monitors, IContentsListener fallbackListener) {
        FactoryTier tier = ((TileEntityFactory<?>) factory).tier;
        FactoryBaseView base = (FactoryBaseView) factory;
        List<IInventorySlot> created = new ArrayList<>(tier.processes);
        for (int i = 0; i < tier.processes; i++) {
            // The per-process recipe monitor, so the cached recipe rechecks when the secondary input changes — and
            // wrapped exactly the way the machine wraps the listeners of its own input slots, so that a change here
            // schedules the same re-sort (even distribution) a change to a main input would. Without the wrapper the
            // machine stops redistributing the moment the extra row exists.
            IContentsListener inner = i < monitors.length && monitors[i] instanceof IContentsListener cl
                  ? cl : fallbackListener;
            IContentsListener slotListener = base.mekaddon$markAllMonitors(inner);
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
     * Drops the main input row by {@link #CONTENT_Y_SHIFT} while the row exists, and puts it back when it does not.
     * <p>
     * The secondary slot of a process belongs under that process's main input, so both rows move together.
     */
    public static void positionInputRow(List<? extends IInventorySlot> inputSlots, boolean rowInstalled) {
        int y = INPUT_Y + (rowInstalled ? CONTENT_Y_SHIFT : 0);
        for (IInventorySlot slot : inputSlots) {
            move(slot, y);
        }
    }

    /**
     * Runs the machine's even-distribution ("均分") over the extra row.
     * <p>
     * Mekanism's own {@code sortInventory} only knows about {@code processInfoSlots[i].inputSlot()} — the main inputs —
     * because the vanilla machine has a single shared secondary slot and nothing to spread. Once every process owns its
     * own secondary slot, ingredients dropped into one of them have to be spread the same way, or the row simply keeps
     * whatever layout the player left behind (one slot holding everything, the rest empty).
     * <p>
     * Called right after Mekanism's pass, so the main inputs are already in their final state and each secondary slot's
     * "would you take this?" answer reflects the pairing it now has. Items are only ever written into slots that are
     * empty or already hold the same item, and the per-item total is preserved exactly, so this can neither destroy a
     * stack nor move one it does not own.
     */
    public static void distributeRow(CombiningRowTile row) {
        List<IInventorySlot> rowSlots = row.mekaddon$secondarySlots();
        if (rowSlots.size() < 2) {
            return;
        }
        // Deliberately typed on the interface, not on the Mekanism-side slot class: on an Extras machine the row is
        // made of ExtrasStackedInputSlot, which cannot share a base class with it. Getting this wrong is invisible —
        // the pass simply finds nothing to distribute and the row keeps whatever layout the player left behind.
        Map<ItemStack, List<SecondaryInputSlot>> groups = ItemStackMap.createTypeAndTagMap();
        for (IInventorySlot slot : rowSlots) {
            if (!(slot instanceof SecondaryInputSlot extra)) {
                continue;
            }
            ItemStack stack = extra.getStack();
            if (!stack.isEmpty()) {
                groups.computeIfAbsent(stack, key -> new ArrayList<>()).add(extra);
            }
        }
        for (Map.Entry<ItemStack, List<SecondaryInputSlot>> entry : groups.entrySet()) {
            ItemStack stack = entry.getKey();
            List<SecondaryInputSlot> holders = entry.getValue();
            int total = 0;
            for (SecondaryInputSlot holder : holders) {
                total += holder.getCount();
            }
            if (total <= 0) {
                continue;
            }
            List<SecondaryInputSlot> targets = new ArrayList<>();
            for (IInventorySlot slot : rowSlots) {
                if (!(slot instanceof SecondaryInputSlot extra)) {
                    continue;
                }
                ItemStack current = extra.getStack();
                if ((current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)) && extra.accepts(stack)) {
                    targets.add(extra);
                }
            }
            if (targets.size() < 2) {
                // Nowhere to spread it to (for instance no process pairs with this ingredient): leave it where it is.
                continue;
            }
            int perSlot = total / targets.size();
            int remainder = total % targets.size();
            boolean changed = false;
            for (int i = 0; i < targets.size() && !changed; i++) {
                int wanted = perSlot + (i < remainder ? 1 : 0);
                ItemStack current = targets.get(i).getStack();
                changed = wanted == 0
                      ? !current.isEmpty()
                      : current.getCount() != wanted || !ItemStack.isSameItemSameComponents(current, stack);
            }
            if (!changed) {
                // Already even. Writing anyway would only schedule another sort pass.
                continue;
            }
            for (SecondaryInputSlot holder : holders) {
                holder.setEmpty();
            }
            for (int i = 0; i < targets.size(); i++) {
                int wanted = perSlot + (i < remainder ? 1 : 0);
                if (wanted > 0) {
                    targets.get(i).setStack(stack.copyWithCount(wanted));
                }
            }
        }
    }

    /**
     * Registers the slots that are live right now as {@link DataType#EXTRA} in the machine's item side config.
     * <p>
     * Two properties matter and both are handled here:
     * <ul>
     *     <li>{@code ConfigInfo#addSlotInfo} <b>replaces</b> the entry for a {@code DataType}, so the call has to carry
     *     the complete set of extra slots every time.</li>
     *     <li>{@code addSlotInfo} also <b>appends</b> the {@code DataType} to each slot's own list, and
     *     {@code ISideConfiguration#getActiveDataType} stops resolving a slot at all once that list is as long as the
     *     config's supported set. An unconditional re-registration is therefore not merely wasteful: it walks the slots
     *     towards a frame-less "normal" slot one sync at a time. This method compares the config's current entry with
     *     what is wanted and only writes when they actually differ.</li>
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
        List<IInventorySlot> desired = new ArrayList<>(slots.size());
        for (IInventorySlot slot : slots) {
            if (!desired.contains(slot)) {
                desired.add(slot);
            }
        }
        ISlotInfo existing = itemConfig.getSlotInfo(DataType.EXTRA);
        if (existing instanceof InventorySlotInfo info && sameSlots(info.getSlots(), desired)) {
            return;
        }
        itemConfig.addSlotInfo(DataType.EXTRA, new InventorySlotInfo(true, true, desired.toArray(new IInventorySlot[0])));
    }

    private static boolean sameSlots(List<IInventorySlot> left, List<IInventorySlot> right) {
        return left.size() == right.size() && new HashSet<>(left).equals(new HashSet<>(right));
    }
}
