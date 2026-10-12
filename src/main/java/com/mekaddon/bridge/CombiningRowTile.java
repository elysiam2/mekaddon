package com.mekaddon.bridge;

import java.util.List;
import mekanism.api.inventory.IInventorySlot;

/**
 * Marks a Combining Factory that the mod can extend, whichever mod the machine comes from.
 * <p>
 * Implemented by the mixins on Mekanism's {@code TileEntityCombiningFactory} and on Mekanism Extras'
 * {@code TileEntityExtraCombiningFactory}. Everything above this interface — the upgrade gate, the row lifecycle, the
 * GUI growth — is shared, so adding a supported machine means implementing this and nothing else.
 * <p>
 * A marker rather than a concrete type on purpose: the shared code must compile without any dependency on optional
 * mods.
 */
public interface CombiningRowTile extends CombiningExtraRowView {

    /** The per-process output slots, so the row can shift them out of the way. Never {@code null}. */
    List<IInventorySlot> mekaddon$outputSlots();

    /**
     * The per-process main input slots. The row shifts them down along with everything else so the machine keeps a
     * little breathing room under the window title once the extra row is there, and so the secondary slot of a process
     * stays directly beneath its own main input.
     */
    List<IInventorySlot> mekaddon$inputSlots();

    /** Log tag used for diagnostics, e.g. the machine's class name. */
    String mekaddon$describe();
}
