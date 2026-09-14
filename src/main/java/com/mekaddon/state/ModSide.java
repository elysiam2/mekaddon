package com.mekaddon.state;

import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.Level;

/**
 * Side detection that is safe to call at any point in a block entity's lifetime.
 * <p>
 * Mekanism's own {@code TileEntityUpdateable#isRemote()} is implemented as {@code getWorldNN().isClientSide()}, and
 * {@code getWorldNN()} deliberately throws {@code NullPointerException("getWorldNN called before world set")} while the
 * level has not been assigned yet. That happens for the whole of {@code TileEntityMekanism}'s constructor — which is
 * exactly when the inventory holder is built, and therefore exactly when the mod needs to know whether the upgraded row
 * is installed. Calling {@code isRemote()} there takes the whole chunk load down with it, so use this instead.
 */
public final class ModSide {

    private ModSide() {
    }

    /** {@code true} only when the tile is on a client level; {@code false} before the level exists. */
    public static boolean isClient(TileEntityMekanism tile) {
        if (tile == null || !tile.hasLevel()) {
            return false;
        }
        Level level = tile.getLevel();
        return level != null && level.isClientSide();
    }
}
