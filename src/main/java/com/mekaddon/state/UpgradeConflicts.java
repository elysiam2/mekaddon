package com.mekaddon.state;

import com.mekaddon.registry.ModUpgrades;
import mekanism.api.Upgrade;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's two upgrades are mutually exclusive: a machine may carry the combining-slot upgrade <b>or</b> the extra
 * stack upgrade, never both.
 * <p>
 * They are two answers to the same question — "how do I get more secondary input capacity out of this machine?" — so
 * allowing both would double up on the same slots. The rule is enforced in three places, all of them live lookups
 * rather than a stored flag:
 * <ul>
 *     <li>the machine's upgrade <b>slot</b> refuses the conflicting item ({@code BasicInventorySlotGuardMixin}), which
 *     covers dragging, shift-clicking, automation and the mod's own Shift + right-click shortcut;</li>
 *     <li>the machine stops <b>supporting</b> the conflicting upgrade ({@code TileComponentUpgradeMixin}), which both
 *     hides it from the upgrade window and stops {@code tickServer} from installing it if it is already sitting in the
 *     slot;</li>
 *     <li>the mod's items report the conflict explicitly when installed by hand, so the player is told why instead of
 *     just seeing nothing happen.</li>
 * </ul>
 * Nothing is ever removed automatically: a machine saved before this rule existed can still hold both, and it keeps
 * working exactly as it did (the per-process row keeps its multiplier). Only <i>new</i> combinations are refused.
 */
public final class UpgradeConflicts {

    private UpgradeConflicts() {
    }

    /**
     * The machine's installed upgrade that conflicts with {@code type}, or {@code null} when there is none.
     * <p>
     * Only the mod's own two upgrades participate: every vanilla Mekanism upgrade is untouched.
     */
    @Nullable
    public static Upgrade installedConflict(TileEntityMekanism tile, Upgrade type) {
        if (tile == null || type == null) {
            return null;
        }
        boolean combining = ModUpgrades.isCombining(type);
        boolean stack = ModUpgrades.isExtraStack(type);
        if (combining == stack) {
            // Neither, or (impossible) both: nothing to do.
            return null;
        }
        Upgrade other = combining ? ModUpgrades.extraStack() : ModUpgrades.combiningSlots();
        if (other == null) {
            return null;
        }
        return count(tile, other) > 0 ? other : null;
    }

    /** {@code true} when {@code stack} is one of the mod's upgrades and the machine already has the other one. */
    public static boolean blocks(TileEntityMekanism tile, ItemStack stack) {
        if (tile == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (!(item instanceof IUpgradeItem upgradeItem)) {
            return false;
        }
        return installedConflict(tile, upgradeItem.getUpgradeType(stack)) != null;
    }

    /** The message shown when an installation is refused, naming the upgrade that is in the way. */
    public static MutableComponent conflictMessage(@Nullable Upgrade installed) {
        if (installed == null) {
            return Component.translatable("message.mekaddon.conflict.generic");
        }
        return Component.translatable("message.mekaddon.conflict",
              Component.translatable(installed.getTranslationKey()));
    }

    /**
     * {@code true} when {@code type} should disappear from this machine's supported set.
     * <p>
     * Deliberately asymmetric: the upgrade is hidden only while it is <i>not</i> installed and the other one is. A
     * machine that already holds both (an older save) therefore keeps both advertised — which is what stops the
     * persisted counts from being dropped on the next load — and an installed upgrade is never hidden from the window
     * that is supposed to list it.
     */
    public static boolean suppressed(TileComponentUpgrade component, Upgrade type) {
        if (component == null || type == null) {
            return false;
        }
        boolean combining = ModUpgrades.isCombining(type);
        boolean stack = ModUpgrades.isExtraStack(type);
        if (combining == stack) {
            return false;
        }
        Upgrade other = combining ? ModUpgrades.extraStack() : ModUpgrades.combiningSlots();
        if (other == null) {
            return false;
        }
        try {
            return component.getUpgrades(other) > 0 && component.getUpgrades(type) == 0;
        } catch (RuntimeException e) {
            com.mekaddon.MekAddon.LOGGER.error("Could not evaluate the mod's upgrade conflict", e);
            return false;
        }
    }

    private static int count(TileEntityMekanism tile, Upgrade type) {
        try {
            TileComponentUpgrade component = tile.getComponent();
            // Null while the block entity is still being constructed, which simply means "nothing installed yet".
            return component == null ? 0 : component.getUpgrades(type);
        } catch (RuntimeException e) {
            // Sits on the slot-insertion and block entity load paths, where a throw would be far worse than a wrong
            // answer: report "no conflict" and let the machine carry on.
            com.mekaddon.MekAddon.LOGGER.error("Could not read the '{}' upgrade count of {}",
                  type.getSerializedName(), tile.getClass().getName(), e);
            return 0;
        }
    }
}
