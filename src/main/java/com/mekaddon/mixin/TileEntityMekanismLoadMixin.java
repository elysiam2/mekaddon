package com.mekaddon.mixin;

import com.mekaddon.slot.ExtraRowLayout;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes sure a Combining Factory's inventory matches its saved upgrade count as soon as the tile has been deserialised.
 * <p>
 * The count is persisted by Mekanism ({@code TileComponentUpgrade} NBT), so nothing is written here. All this does is
 * restore the row — including for machines that were upgraded in a previous session, and for machines saved before the
 * upgrade existed, where the count is zero and the layout must stay vanilla.
 * <p>
 * It runs after the tile's components (and therefore its upgrades) have been read, but before slot contents are
 * restored, so the layout is settled before anything is placed into it. On the client this is also the chunk-load path;
 * the row itself is added later, when the block entity update packet supplies the flag.
 */
@Mixin(TileEntityMekanism.class)
public abstract class TileEntityMekanismLoadMixin {

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = false)
    private void mekaddon$syncRowOnLoad(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        ExtraRowLayout.sync((TileEntityMekanism) (Object) this);
    }
}
