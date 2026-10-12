package com.mekaddon.item;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.StackScalableTile;
import com.mekaddon.registry.ModUpgrades;
import com.mekaddon.state.ExtraRowState;
import com.mekaddon.state.UpgradeConflicts;
import java.util.List;
import mekanism.api.Upgrade;
import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.component.TileComponentUpgrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

/**
 * "Extra Stack Upgrade" — multiplies the stack limit of a machine's secondary ("extra") input: one installed gives x2,
 * two give x4, and eight (the maximum) give x256.
 * <p>
 * Which slots that applies to depends on the machine. On a Combining Factory it is the machine's one shared secondary
 * input, or — with the combining-slot upgrade installed, which is the mutually exclusive alternative (see
 * {@link com.mekaddon.state.UpgradeConflicts}) — every process's own secondary slot in the row that upgrade adds. On
 * the machines that merely have extra inputs (the CNC stamper and presser, and their stamping/pressing factories,
 * including Mekanism Extras' tiers) it is those inputs; see {@link StackScalableTile}.
 * <p>
 * Installed exactly like a vanilla Mekanism upgrade: it reports its own {@link Upgrade} constant (see
 * {@link ModUpgrades}), so the upgrade window offers it, the upgrade slot accepts it, and uninstalling it returns this
 * item. It stacks up to {@link ModUpgrades#STACK_MAX}, and the Shift + right-click shortcut installs the whole held
 * stack in one go — exactly as far as the maximum allows, leaving whatever does not fit in the player's hand.
 * <p>
 * The upgrade itself only stores a count; the multiplier is read live by the affected slots (see
 * {@link com.mekaddon.state.ExtraStackState}), so installing or removing one takes effect immediately.
 */
public class ItemExtraStackUpgrade extends Item implements IUpgradeItem {

    public ItemExtraStackUpgrade(Properties properties) {
        super(properties.rarity(Rarity.RARE).stacksTo(64));
    }

    @Override
    public Upgrade getUpgradeType(@NotNull ItemStack stack) {
        Upgrade upgrade = ModUpgrades.extraStack();
        // Degenerate case only: if the enum constant could not be registered the item must still be a valid
        // IUpgradeItem so nothing NPEs; it then simply installs as a speed upgrade and does nothing special.
        return upgrade == null ? Upgrade.SPEED : upgrade;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof TileEntityMekanism tile)) {
            return InteractionResult.PASS;
        }
        // Any machine this mod extends, not just the combining factories: the machines that simply lend their extra
        // inputs to the multiplier (the CNC stamper, the presser and their factories) take the upgrade through the
        // upgrade window and its slot, so they have to take it through this shortcut too. Requiring the per-process row
        // here is what made Shift + right-click silently do nothing on exactly those machines.
        if (!(tile instanceof CombiningRowTile || tile instanceof StackScalableTile)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            // Defer the actual state change to the server; just report success for the client hand swing.
            return InteractionResult.sidedSuccess(true);
        }
        Upgrade upgrade = ModUpgrades.extraStack();
        if (upgrade == null) {
            return InteractionResult.PASS;
        }
        TileComponentUpgrade component = tile.getComponent();
        if (component == null) {
            return InteractionResult.PASS;
        }
        // Mutually exclusive with the combining-slot upgrade: whichever is installed, the other is refused. Checked
        // first so the player is told which upgrade is in the way rather than just seeing the insertion fail.
        if (UpgradeConflicts.installedConflict(tile, upgrade) != null) {
            player.displayClientMessage(UpgradeConflicts.conflictMessage(ModUpgrades.combiningSlots())
                  .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        int installed = component.getUpgrades(upgrade);
        if (installed >= ModUpgrades.STACK_MAX) {
            player.displayClientMessage(Component.translatable("message.mekaddon.extra_stack_upgrade.full",
                  ModUpgrades.STACK_MAX).withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        // Vanilla parity for the shortcut: the whole held stack goes in at once, capped by the upgrade's maximum, and
        // whatever does not fit simply stays in the player's hand. Same call Mekanism's own upgrade item makes, so the
        // installed map ends up identical to what the upgrade window would produce.
        ItemStack inHand = context.getItemInHand();
        int added = component.addUpgrades(upgrade, inHand.getCount());
        if (added <= 0) {
            player.displayClientMessage(Component.translatable("message.mekaddon.extra_stack_upgrade.failed")
                  .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            inHand.shrink(added);
        }
        int resulting = Math.min(installed + added, ModUpgrades.STACK_MAX);
        player.displayClientMessage(Component.translatable("message.mekaddon.extra_stack_upgrade.installed",
              resulting, 1 << resulting).withStyle(ChatFormatting.GREEN), true);
        if (!ExtraRowState.isUnlocked(tile)) {
            // Without the combining-slot upgrade there is no per-process row, so the multiplier lands on the machine's
            // single shared secondary slot. Say so, otherwise the player has no way to tell it worked.
            player.displayClientMessage(Component.translatable("message.mekaddon.extra_stack_upgrade.shared_slot")
                  .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.mekaddon.extra_stack_upgrade.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mekaddon.extra_stack_upgrade.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mekaddon.extra_stack_upgrade.machines").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.mekaddon.extra_stack_upgrade.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
