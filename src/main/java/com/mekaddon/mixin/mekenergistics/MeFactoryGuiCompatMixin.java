package com.mekaddon.mixin.mekenergistics;

import com.mekaddon.bridge.ContainerScreenLayoutView;
import com.mekaddon.bridge.GuiFactoryTileView;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.state.ExtraRowState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.IProgressInfoHandler;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compatibility shim for Mek Energistics (mekenergistics).
 * <p>
 * That mod ships its own "ME factory" machine variants (converted in place via its "ME 工厂安装器" item): the block
 * entity stays a plain Mekanism / Mekanism Extras factory tile, but the menu type and the <b>screen</b> are its own
 * re-implementations ({@code MeGuiFactory}, {@code MeGuiExtraFactory}, ...), which extend {@code GuiMekanismTile}
 * directly rather than Mekanism's {@code GuiFactory} / Extras' {@code GuiExtraFactory}. The container- and tile-side
 * of this mod therefore keeps working untouched (our mixins land on shared superclasses), but the two screen-side
 * adjustments - growing the window and shifting the DOWN progress arrows - never run, so an upgraded Combining
 * Factory renders with the extra row overlapping the arrows and the player inventory too high.
 * <p>
 * The screens are near-copies of the vanilla layout (same column geometry, same {@code y=13} inputs, same output
 * positions), so exactly the same treatment as {@code GuiFactoryMixin} / {@code ExtraFactoryGuiMixin} applies.
 * The {@code tile} field and the {@code imageHeight} / {@code inventoryLabelY} layout fields are inherited from
 * {@code GuiMekanismTile} / {@code AbstractContainerScreen}, where this mod already bridges them.
 * <p>
 * Targets are given as strings and the config is marked not-required (plus {@code MekAddonMixinPlugin} re-checks
 * class presence per target), so none of this loads when Mek Energistics is not installed. Nothing here references
 * that mod's classes at compile time.
 */
@Mixin(targets = {
      "com.beipuo.mekenergistics.client.screen.machine.MeGuiFactory",
      "com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraFactory",
      "com.beipuo.mekenergistics.client.screen.machine.MeGuiExtraAdvancedFactory"
})
public abstract class MeFactoryGuiCompatMixin {

    @Unique
    private boolean mekaddon$isUpgradedCombining;

    /**
     * Same growth as {@code GuiFactoryMixin}: make room for the extra secondary-input row and push the player
     * inventory label down. The constructor descriptor is pinned explicitly - every Mek Energistics factory screen
     * uses the standard {@code (MekanismTileContainer, Inventory, Component)} signature, and pinning keeps Mixin from
     * trying to match any auxiliary constructors.
     */
    @Inject(
          method = "<init>(Lmekanism/common/inventory/container/tile/MekanismTileContainer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;)V",
          at = @At("TAIL"),
          remap = false
    )
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
     * Arrow shift: DOWN progress arrows move to a y value that sits <em>strictly below the secondary input row</em>
     * (which is at y=35..53 once the upgrade is installed) and <em>above the shifted output row</em> (y=79..97).
     * <p>
     * Why {@code y = OUTPUT_Y - 22} rather than {@code y = original_y + ARROW_Y_OFFSET}: Mek Energistics draws its
     * arrows at y=33 (above the vanilla output row at y=57), and a delta-from-original shift lands at y=55 which is
     * 2px below the secondary row - close enough that visual glitches and overlap became a recurring support issue.
     * "Land just above the shifted output row" is the position-independent rule that always clears the 18px-tall
     * secondary row (y=35..53) cleanly.
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
            // Just above the shifted output row, below the secondary row.
            y = ExtraRowSlots.OUTPUT_Y - 22;
        }
        return new GuiProgress(handler, type, gui, x, y);
    }

    /**
     * Safety net for the secondary input row's gold frames.
     * <p>
     * Mek Energistics' {@code addGuiElements} calls {@code super.addGuiElements} (which on this branch ultimately
     * invokes {@link GuiMekanism#addSlots()}) and then adds its own widgets on top. {@code GuiMekanism.addSlots()}
     * iterates the container's slots and creates one {@code GuiSlot} widget per {@link InventoryContainerSlot} at the
     * slot's {@code (x-1, y-1)}, using the {@code DataType} lookup for the frame colour. When the mod's
     * {@code registerConfig} side-config registration is in effect, every secondary slot gets {@code EXTRA} and
     * therefore the gold frame - so normally nothing extra is needed here.
     * <p>
     * This injection is the belt-and-braces fix for the failure mode where that wiring is somehow skipped (for
     * example, a third-party mod that wraps {@code addSlots} differently, or a future Mek Energistics refactor):
     * after {@code addGuiElements} returns we still walk the container's slots, and for any EXTRA slot whose widget
     * is missing from the renderable list we add one explicitly. The protected {@code findDataType} /
     * {@code addRenderableWidget} methods are reached via reflection so the source stays independent of any
     * internal renames in Mekanism and degrades gracefully (no-op) if the method is ever removed.
     */
    @Inject(method = "addGuiElements", at = @At("TAIL"), remap = false)
    private void mekaddon$ensureExtraSlotFrames(CallbackInfo ci) {
        if (!mekaddon$isUpgradedCombining) {
            return;
        }
        Object self = this;
        List<?> renderables = findRenderables(self);
        Method findDataType = findMethod(self.getClass(), "findDataType", mekanism.common.tile.component.config.DataType.class,
              InventoryContainerSlot.class);
        Method addRenderableWidget = findMethod(self.getClass(), "addRenderableWidget", GuiElement.class);
        if (findDataType == null || addRenderableWidget == null) {
            return;
        }
        GuiMekanism<?> screen = (GuiMekanism<?>) self;
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot instanceof InventoryContainerSlot ics)) {
                continue;
            }
            Object dt;
            try {
                dt = findDataType.invoke(screen, ics);
            } catch (ReflectiveOperationException e) {
                continue;
            }
            if (dt != mekanism.common.tile.component.config.DataType.EXTRA) {
                continue;
            }
            SlotType slotType = SlotType.get((mekanism.common.tile.component.config.DataType) dt);
            int sx = slot.x - 1;
            int sy = slot.y - 1;
            boolean already = false;
            for (Object r : renderables) {
                if (r instanceof GuiSlot gs && gs.getX() == sx && gs.getY() == sy) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            GuiSlot widget = new GuiSlot(slotType, (IGuiWrapper) self, sx, sy);
            ics.addWarnings(widget);
            try {
                addRenderableWidget.invoke(screen, widget);
            } catch (ReflectiveOperationException e) {
                // Give up silently - addSlots has likely drawn something we cannot override.
            }
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static List<?> findRenderables(Object screen) {
        for (Field f : screen.getClass().getDeclaredFields()) {
            if ("renderables".equals(f.getName())) {
                f.setAccessible(true);
                try {
                    return (List<?>) f.get(screen);
                } catch (ReflectiveOperationException e) {
                    return java.util.Collections.emptyList();
                }
            }
        }
        // Fall back to walking the superclass chain (the field is declared on GuiMekanism).
        for (Class<?> c = screen.getClass().getSuperclass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if ("renderables".equals(f.getName())) {
                    f.setAccessible(true);
                    try {
                        return (List<?>) f.get(screen);
                    } catch (ReflectiveOperationException e) {
                        return java.util.Collections.emptyList();
                    }
                }
            }
        }
        return java.util.Collections.emptyList();
    }

    @Unique
    private static Method findMethod(Class<?> startClass, String name, Class<?>... paramTypes) {
        Class<?> c = startClass;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}