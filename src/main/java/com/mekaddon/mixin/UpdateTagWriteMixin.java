package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.state.ExtraRowState;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ships the "secondary row installed" flag to the client with the block entity update packet, so the client GUI knows
 * to grow and the container lays the extra slots out in the same place the server did.
 * <p>
 * The flag has to travel this way rather than through the synced upgrade count: the count only arrives once a container
 * is open, which is after the client has already had to decide how many slots its menu has.
 * <p>
 * {@code getReducedUpdateTag} is declared on {@link TileEntityMekanism}, which is why this mixin targets that class and
 * guards with a marker check. {@code getUpdateTag} delegates to {@code getReducedUpdateTag} in Mekanism, so the packet
 * sent when a player starts tracking the chunk carries the flag too.
 */
@Mixin(TileEntityMekanism.class)
public abstract class UpdateTagWriteMixin {

    @Unique
    private static final String MEKADDON_EXTRA_ROW_KEY = "mekaddon:combining_extra_row";

    @Inject(method = "getReducedUpdateTag", at = @At("TAIL"), remap = false)
    private void mekaddon$writeUpdateTag(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        if ((Object) this instanceof CombiningRowTile && ExtraRowState.isUnlocked((TileEntityMekanism) (Object) this)) {
            CompoundTag tag = cir.getReturnValue();
            if (tag != null) {
                tag.putBoolean(MEKADDON_EXTRA_ROW_KEY, true);
            }
        }
    }
}
