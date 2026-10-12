package com.mekaddon.state;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.StackScalableTile;
import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.world.item.ItemStack;

/**
 * Answers "how much bigger is a secondary input stack on this machine?".
 * <p>
 * The "Extra Stack Upgrade" doubles the stack limit of the machine's per-process secondary (yellow) input slots once
 * per installed upgrade: one installed gives x2, two give x4, and so on up to eight, which give x256. The level lives
 * in Mekanism's own upgrade map ({@code TileComponentUpgrade}, persisted in the tile's upgrade NBT and synced by the
 * container trackers), so nothing extra has to be stored or synchronised here — this class only reads it.
 * <p>
 * The multiplier is applied by overriding {@code BasicInventorySlot#getLimit} on the slots the mod creates, rather than
 * by mutating anything: the answer is therefore always live, and installing or removing an upgrade takes effect
 * immediately, with no GUI rebuild.
 */
public final class ExtraStackState {

    /** Highest stack limit the upgrade can reach, as a multiple of the slot's normal limit. */
    public static final int MAX_MULTIPLIER = ModUpgrades.STACK_MAX_MULTIPLIER;

    private ExtraStackState() {
    }

    /**
     * Number of stack upgrades installed on {@code tile}, clamped to {@code 0..8}.
     * <p>
     * Only machines the mod extends qualify; everything else answers 0 and is left completely alone. Unsupported
     * machines also cannot have the upgrade installed in the first place (see {@code UpgradeSupportMixin}), so this is
     * a belt-and-braces check rather than the actual gate.
     */
    public static int level(TileEntityMekanism tile) {
        Upgrade upgrade = ModUpgrades.extraStack();
        if (upgrade == null || !isScalable(tile)) {
            return 0;
        }
        try {
            TileComponentUpgrade component = tile.getComponent();
            if (component == null) {
                // Null while the block entity is still being constructed, which simply means "none installed yet".
                return 0;
            }
            int installed = component.getUpgrades(upgrade);
            return Math.max(0, Math.min(installed, ModUpgrades.STACK_MAX));
        } catch (RuntimeException e) {
            // Sits on the block entity construction and NBT load path, where a throw is fatal to the world rather than
            // merely wrong: answer "no multiplier" instead of taking the chunk down.
            com.mekaddon.MekAddon.LOGGER.error("Could not read the extra stack upgrade level of {}",
                  tile.getClass().getName(), e);
            return 0;
        }
    }

    /**
     * {@code true} for the machines whose extra input the mod enlarges: either a Combining Factory (which also has the
     * per-process row) or a machine that only lends its secondary input slot to the multiplier — the CNC stamper and
     * the stamping factories.
     */
    public static boolean isScalable(TileEntityMekanism tile) {
        return tile instanceof CombiningRowTile || tile instanceof StackScalableTile;
    }

    /** {@code 2^level}, i.e. 1 (no upgrade) up to 256 (all eight installed). */
    public static int multiplier(TileEntityMekanism tile) {
        return 1 << level(tile);
    }

    /** {@code base} scaled by the machine's multiplier. What every affected slot's {@code getLimit} calls. */
    public static int scale(TileEntityMekanism tile, int base, ItemStack stack) {
        return scaledLimit(base, multiplier(tile), stack);
    }

    /**
     * Applies {@code multiplier} to a slot's normal limit.
     * <p>
     * Unstackable items are deliberately left alone: the base limit there is 1, and multiplying it would let the
     * machine hold several copies of something that cannot normally be stacked at all.
     */
    public static int scaledLimit(int base, int multiplier, ItemStack stack) {
        if (multiplier <= 1 || base <= 1) {
            return base;
        }
        if (!stack.isEmpty() && stack.getMaxStackSize() <= 1) {
            return base;
        }
        long scaled = (long) base * multiplier;
        return scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }
}
