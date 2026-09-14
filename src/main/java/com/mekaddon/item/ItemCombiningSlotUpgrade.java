package com.mekaddon.item;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.registry.ModUpgrades;
import com.mekaddon.state.ExtraRowState;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.Upgrade;
import mekanism.common.inventory.slot.UpgradeInventorySlot;
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
 * "Combining Slot Upgrade" — installs into a Combining Factory's upgrade slot like any vanilla Mekanism upgrade.
 * <p>
 * The item reports its own {@link Upgrade} constant (see {@link ModUpgrades}), so it behaves exactly like a Mekanism
 * upgrade everywhere: it is offered in the upgrade window, it is accepted by the upgrade slot, it shows up in the
 * installed list and it can be uninstalled again — returning this item, because
 * {@code UpgradeUtilsMixin} maps the constant back to this item.
 * <p>
 * Installing it replaces the machine's single shared secondary ("yellow") input slot with one per process; see
 * {@code CombiningFactoryExtraSlotsMixin}. Only ever one is consumed: the upgrade's own max stack is 1, so the machine
 * stops taking them from the slot once one is installed. The other supported route is Shift + right-click, which simply
 * puts one into the machine's upgrade slot and lets Mekanism's own tick install it — the same final state either way.
 */
public class ItemCombiningSlotUpgrade extends Item implements IUpgradeItem {

    public ItemCombiningSlotUpgrade(Properties properties) {
        // Carries like any ordinary item, but the upgrade itself is capped at one per machine by Upgrade#getMax.
        super(properties.rarity(Rarity.RARE).stacksTo(64));
    }

    @Override
    public Upgrade getUpgradeType(@NotNull ItemStack stack) {
        Upgrade upgrade = ModUpgrades.combiningSlots();
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
        if (!(blockEntity instanceof CombiningRowTile) || !(blockEntity instanceof TileEntityMekanism tile)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            // Defer the actual state change to the server; just report success for the client hand swing.
            return InteractionResult.sidedSuccess(true);
        }
        if (ExtraRowState.isUnlocked(tile)) {
            player.displayClientMessage(Component.translatable("message.mekaddon.combining_slot_upgrade.already")
                  .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.FAIL;
        }
        TileComponentUpgrade component = tile.getComponent();
        UpgradeInventorySlot upgradeSlot = component == null ? null : component.getUpgradeSlot();
        if (upgradeSlot == null) {
            return InteractionResult.PASS;
        }
        // Hand one over to the machine rather than flipping any state ourselves: Mekanism's tickServer moves it from
        // the upgrade slot into the installed map, so this path and the upgrade window end up identical.
        ItemStack remainder = upgradeSlot.insertItem(new ItemStack(this, 1), Action.EXECUTE, AutomationType.INTERNAL);
        if (!remainder.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.mekaddon.combining_slot_upgrade.failed")
                  .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.mekaddon.combining_slot_upgrade.unlocked")
              .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.mekaddon.combining_slot_upgrade.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mekaddon.combining_slot_upgrade.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mekaddon.combining_slot_upgrade.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
