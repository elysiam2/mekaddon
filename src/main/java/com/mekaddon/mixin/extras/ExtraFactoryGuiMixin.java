package com.mekaddon.mixin.extras;

import com.mekaddon.bridge.ContainerScreenLayoutView;
import com.mekaddon.bridge.GuiFactoryTileView;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client half of the Mekanism Extras integration: grows Extras' factory GUI by the height the extra row needs and
 * moves its DOWN progress arrows out of the way.
 * <p>
 * Extras' {@code GuiExtraFactory} does not extend Mekanism's {@code GuiFactory}, but it is a near-copy of it: same
 * 27px column base, same 19px spacing, same arrow at {@code y = 33} built the same way, and it inherits the {@code tile}
 * field and the {@code imageHeight} / {@code inventoryLabelY} layout fields this mod already bridges on
 * {@code GuiMekanismTile} and {@code AbstractContainerScreen}. So the same treatment applies verbatim.
 * <p>
 * The target is given as a string so the mixin is skipped when Extras is not installed.
 */
@Mixin(targets = "com.jerry.mekextras.client.gui.machine.GuiExtraFactory")
public abstract class ExtraFactoryGuiMixin {

    @Unique
    private boolean mekaddon$isUpgradedCombining;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void mekaddon$growForExtraRow(MekanismTileContainer<?> container, Inventory inv, Component title,
          CallbackInfo ci) {
        TileEntityMekanism tile = ((GuiFactoryTileView) (Object) this).mekaddon$getTile();
        mekaddon$isUpgradedCombining = ExtraRowState.isUnlocked(tile);
        if (mekaddon$isUpgradedCombining) {
            ContainerScreenLayoutView layout = (ContainerScreenLayoutView) (Object) this;
            layout.mekaddon$setImageHeight(layout.mekaddon$getImageHeight() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
            layout.mekaddon$setInventoryLabelY(layout.mekaddon$getInventoryLabelY() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
        }
    }

    /**
     * Shifts the DOWN progress arrows down so they sit between the new secondary row and the shifted output row
     * instead of overlapping the extra slots.
     */
    @Redirect(
          method = "addGuiElements",
          at = @At(value = "NEW",
                target = "(Lmekanism/client/gui/element/progress/IProgressInfoHandler;Lmekanism/client/gui/element/progress/ProgressType;Lmekanism/client/gui/IGuiWrapper;II)Lmekanism/client/gui/element/progress/GuiProgress;"),
          remap = false
    )
    private GuiProgress mekaddon$shiftProgressArrow(IProgressInfoHandler handler, ProgressType type, IGuiWrapper gui,
          int x, int y) {
        if (mekaddon$isUpgradedCombining && type == ProgressType.DOWN) {
            y += ExtraRowSlots.ARROW_Y_OFFSET;
        }
        return new GuiProgress(handler, type, gui, x, y);
    }
}
