package com.mekaddon.mixin;

import com.mekaddon.bridge.ContainerScreenLayoutView;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes the protected layout fields declared on {@link AbstractContainerScreen}.
 * <p>
 * They are not declared on {@code GuiFactory} itself, so {@code @Shadow} from the GuiFactory mixin would fail; shadowing
 * them here, targeting the declaring class, works. Values are exposed through {@link ContainerScreenLayoutView} to keep
 * the GuiFactory mixin independent of this class.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenAccessor implements ContainerScreenLayoutView {

    @Shadow
    protected int imageHeight;

    @Shadow
    protected int inventoryLabelY;

    @Override
    public int mekaddon$getImageHeight() {
        return imageHeight;
    }

    @Override
    public void mekaddon$setImageHeight(int height) {
        this.imageHeight = height;
    }

    @Override
    public int mekaddon$getInventoryLabelY() {
        return inventoryLabelY;
    }

    @Override
    public void mekaddon$setInventoryLabelY(int y) {
        this.inventoryLabelY = y;
    }
}
