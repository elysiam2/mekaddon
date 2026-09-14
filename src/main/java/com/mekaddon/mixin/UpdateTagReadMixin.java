package com.mekaddon.mixin;

import com.mekaddon.bridge.ExtraRowView;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityUpdateable;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side sync of the "secondary row installed" flag that {@code UpdateTagWriteMixin} adds to the block entity
 * update packet.
 * <p>
 * Two jobs, and both matter:
 * <ol>
 *     <li>Record the flag, because the client must know the machine's slot layout <b>before</b> it builds the GUI menu —
 *     see {@link com.mekaddon.state.ExtraRowState}. The upgrade count arrives too late for that, via the container
 *     trackers of the menu being built.</li>
 *     <li>Close the container the player currently has open against this factory. The holder <b>cannot</b> be rebuilt
 *     with a container open: the menu's slot list is fixed for its lifetime, so the next
 *     {@code container_set_content} would write past its end and the server would kick the player with
 *     {@code IndexOutOfBoundsException} / "Network Protocol Error". Rebuilding is therefore deferred to the next
 *     container open, via {@code FactoryContainerMixin}, and this close is what makes that open happen.</li>
 * </ol>
 * {@code handleUpdateTag} is declared on {@link TileEntityUpdateable}, hence this mixin's target class.
 */
@Mixin(TileEntityUpdateable.class)
public abstract class UpdateTagReadMixin implements ExtraRowView {

    @Unique
    private static final String MEKADDON_EXTRA_ROW_KEY = "mekaddon:combining_extra_row";

    /** Latest value seen on this tile; also what the GUI asks for while building itself. */
    @Unique
    private boolean mekaddon$rowInstalled;

    @Override
    public boolean mekaddon$extraRowInstalled() {
        return mekaddon$rowInstalled;
    }

    @Inject(method = "handleUpdateTag", at = @At("TAIL"), remap = false)
    private void mekaddon$readUpdateTag(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        boolean installed = tag.contains(MEKADDON_EXTRA_ROW_KEY) && tag.getBoolean(MEKADDON_EXTRA_ROW_KEY);
        if (mekaddon$rowInstalled == installed) {
            return;
        }
        mekaddon$rowInstalled = installed;
        if ((Object) this instanceof TileEntityCombiningFactory factory) {
            mekaddon$closeOpenContainer(factory);
        }
    }

    /**
     * Closes the player's open container if it belongs to this factory.
     * <p>
     * No-op when nothing is open or the open menu is unrelated.
     */
    @Unique
    private static void mekaddon$closeOpenContainer(TileEntityCombiningFactory factory) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // FactoryContainer is a subclass of MekanismTileContainer, so this catches the open factory GUI and any future
        // subclass. No separate FactoryContainer branch is needed.
        if (mc.player.containerMenu instanceof MekanismTileContainer<?> mt && mt.getTileEntity() == factory) {
            mc.player.closeContainer();
        }
    }
}
