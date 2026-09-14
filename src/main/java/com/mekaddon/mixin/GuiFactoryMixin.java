package com.mekaddon.mixin;

import com.mekaddon.bridge.ContainerScreenLayoutView;
import com.mekaddon.bridge.GuiFactoryTileView;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.machine.GuiFactory;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import mekanism.common.tile.factory.TileEntityFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grows the Combining Factory GUI by exactly the space the extra slot row needs, and moves the progress arrows down so
 * they don't overlap the extra input row.
 * <p>
 * An un-upgraded factory therefore renders identically to vanilla.
 * <p>
 * Note: the {@code tile} field lives on {@code GuiMekanismTile} (the parent of {@link GuiFactory}), so Mixin field
 * shadowing can't resolve it from this target. {@link GuiMekanismTileMixin} shadows it on the declaring class and
 * exposes it through {@link GuiFactoryTileView}; we read it via that bridge.
 */
@Mixin(GuiFactory.class)
public abstract class GuiFactoryMixin {

    @Unique
    private boolean mekaddon$isUpgradedCombining;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void mekaddon$growForExtraRow(MekanismTileContainer<TileEntityFactory<?>> container, Inventory inv,
          Component title, CallbackInfo ci) {
        TileEntityFactory<?> tile = (TileEntityFactory<?>) tileView().mekaddon$getTile();
        mekaddon$isUpgradedCombining = tile instanceof TileEntityCombiningFactory factory && ExtraRowState.isUnlocked(factory);
        if (mekaddon$isUpgradedCombining) {
            accessor().mekaddon$setImageHeight(accessor().mekaddon$getImageHeight() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
            accessor().mekaddon$setInventoryLabelY(accessor().mekaddon$getInventoryLabelY() + ExtraRowSlots.EXTRA_GUI_HEIGHT);
        }
    }

    /**
     * Shifts the DOWN progress arrows down so they sit between the new extra input row and the shifted output row
     * instead of overlapping the extra input slots.
     * <p>
     * IMPORTANT: for {@code @At("NEW")} the target must be written as {@code (args)Lowner/Class;} — i.e. the
     * constructor descriptor with the <b>owner class as the return type</b>. The {@code Lowner;<init>(args)V}
     * form used for {@code @At("INVOKE")} does <b>not</b> work here: {@code MemberInfo#toCtorType()} derives the
     * owner from the descriptor's return type, so a {@code V} return yields {@code "V"} and the NEW node is never
     * matched ("Scanned 0 target(s)").
     */
    @Redirect(
          method = "addGuiElements",
          at = @At(value = "NEW",
                target = "(Lmekanism/client/gui/element/progress/IProgressInfoHandler;Lmekanism/client/gui/element/progress/ProgressType;Lmekanism/client/gui/IGuiWrapper;II)Lmekanism/client/gui/element/progress/GuiProgress;"),
          remap = false
    )
    private GuiProgress mekaddon$shiftProgressArrow(IProgressInfoHandler handler, ProgressType type, IGuiWrapper gui, int x, int y) {
        if (mekaddon$isUpgradedCombining && type == ProgressType.DOWN) {
            // From between input/output to between the new secondary row and the shifted output row.
            y += ExtraRowSlots.ARROW_Y_OFFSET;
        }
        return new GuiProgress(handler, type, gui, x, y);
    }

    @Unique
    private ContainerScreenLayoutView accessor() {
        return (ContainerScreenLayoutView) (Object) this;
    }

    @Unique
    private GuiFactoryTileView tileView() {
        return (GuiFactoryTileView) (Object) this;
    }
}