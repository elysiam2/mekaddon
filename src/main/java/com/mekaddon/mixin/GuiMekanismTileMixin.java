package com.mekaddon.mixin;

import com.mekaddon.bridge.GuiFactoryTileView;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.tile.base.TileEntityMekanism;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Shadows the {@code tile} field on {@link GuiMekanismTile} and exposes it through {@link GuiFactoryTileView}.
 * <p>
 * The {@code tile} field lives on {@link GuiMekanismTile}, not on {@code GuiFactory} itself, so {@code @Shadow}
 * from a {@code GuiFactory} mixin fails. Targeting the declaring class here and exposing the value via a bridge
 * interface lets {@code GuiFactoryMixin} read it cleanly.
 */
@Mixin(GuiMekanismTile.class)
public abstract class GuiMekanismTileMixin implements GuiFactoryTileView {

    @Shadow
    protected TileEntityMekanism tile;

    @Override
    public TileEntityMekanism mekaddon$getTile() {
        return tile;
    }
}