package com.mekaddon.slot;

/**
 * Marks one of the mod's secondary ("yellow") input slots — the machine's own shared one, or one of the per-process
 * slots the combining-slot upgrade adds.
 * <p>
 * Two things key off this rather than off the machine's side configuration:
 * <ul>
 *     <li>{@code GuiMekanism#findDataType} is forced to {@code DataType.EXTRA} for these slots, so their frame is the
 *     gold one regardless of what the side config happens to resolve to. Mekanism only draws a DataType's frame while
 *     {@code 0 < dataTypeForContainer(slot).size() < supportedDataTypes.size()}, and every {@code addSlotInfo} call
 *     <b>appends</b> to that per-slot list — so a frame that depends on the config is one re-registration away from
 *     silently turning into the plain grey "normal" frame.</li>
 *     <li>which of them the menu actually contains is decided per container build from
 *     {@code ExtraRowState.isUnlocked} (see {@link #createContainerSlot()} on the implementations), so the menu and
 *     the window geometry are driven by one and the same answer instead of by two lists that can drift apart.</li>
 * </ul>
 */
public interface ExtraSecondarySlot {
}
