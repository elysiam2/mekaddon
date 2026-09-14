package com.mekaddon.mixin;

import com.mekaddon.slot.ExtraRowLayout;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tile's inventory must match the layout the menu is about to be built for.
 * <p>
 * {@code MekanismTileContainer#addSlots} iterates {@code tile.getInventorySlots(null)} and copies each slot's
 * coordinates, so this is the last moment at which the row can be appended and the output row moved. On the client the
 * answer comes from the block entity update packet, because the synced upgrade count only arrives with the menu being
 * built — see {@code ExtraRowState}.
 * <p>
 * Targetting {@code MekanismTileContainer} rather than Mekanism's {@code FactoryContainer} is what makes addon
 * factories work: they use the plain container class, and their menu is built from this same constructor. A
 * {@code FactoryContainer} reaches it through {@code super(...)}, so one hook covers both, and
 * {@link ExtraRowLayout#sync} is idempotent so the extra call costs nothing.
 */
@Mixin(MekanismTileContainer.class)
public abstract class MekanismTileContainerMixin {

    /**
     * Static is required for HEAD constructor injection: the object has not been constructed yet, so instance methods
     * are forbidden before super(). The tile parameter is all we need.
     * <p>
     * The parameter list must repeat the target constructor's exactly — Mixin compares descriptors, and widening the
     * first one to {@code Object} is a hard startup failure ("Invalid descriptor on ... but found ...").
     */
    @Inject(method = "<init>", at = @At("HEAD"), remap = false)
    private static void mekaddon$syncLayoutBeforeSlots(ContainerTypeRegistryObject<?> containerType, int windowId,
          Inventory inv, TileEntityMekanism tile, CallbackInfo ci) {
        ExtraRowLayout.sync(tile);
    }
}
