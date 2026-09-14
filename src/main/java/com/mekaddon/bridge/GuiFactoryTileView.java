package com.mekaddon.bridge;

import mekanism.common.tile.base.TileEntityMekanism;

/**
 * Read-only view of the {@code tile} field declared on {@code GuiMekanismTile} (the parent of
 * {@code mekanism.client.gui.machine.GuiFactory}). Mixin field shadowing only resolves fields declared on the
 * exact target class, so the field is shadowed by {@code GuiMekanismTileMixin} and exposed through this
 * interface so {@code GuiFactoryMixin} can read it without depending on a sibling mixin class.
 */
public interface GuiFactoryTileView {

    TileEntityMekanism mekaddon$getTile();
}