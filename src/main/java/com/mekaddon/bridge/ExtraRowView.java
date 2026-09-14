package com.mekaddon.bridge;

/**
 * Client-side knowledge of whether the factory currently has its upgraded secondary-input row.
 * <p>
 * The upgrade count itself is authoritative on the server, but on the client it only arrives once a container is
 * open (Mekanism syncs it through the container trackers) — too late: the client has to know the layout <i>before</i>
 * it builds its menu, otherwise the client and server menus end up with different slot counts and the next
 * {@code container_set_content} packet kicks the player with {@code IndexOutOfBoundsException}.
 * <p>
 * The block entity update packet can be sent at any time, so the same flag is shipped there and implemented by
 * {@code UpdateTagReadMixin} (which targets {@code TileEntityUpdateable}, hence "every tile").
 */
public interface ExtraRowView {

    boolean mekaddon$extraRowInstalled();
}
