package com.mekaddon.bridge;

/**
 * Marks a machine whose secondary ("extra") input slot the "Extra Stack Upgrade" enlarges, <b>without</b> the machine
 * having a per-process row to offer — the CNC stamper and the stamping factories, for instance, where the extra input is
 * the mold and there is one of it.
 * <p>
 * A marker rather than a concrete type on purpose, exactly like {@link CombiningRowTile}: the machines come from other
 * mods, so nothing here may reference them at compile time. A machine that implements both this and
 * {@link CombiningRowTile} is simply offered both upgrades (and then the mutual-exclusion rule applies).
 */
public interface StackScalableTile {
}
