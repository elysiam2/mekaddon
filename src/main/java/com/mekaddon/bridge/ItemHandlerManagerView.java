package com.mekaddon.bridge;

import mekanism.common.capabilities.resolver.manager.ItemHandlerManager;

/**
 * Duck-typed view onto {@code TileEntityMekanism}, implemented by {@code TileEntityMekanismAccessor}.
 */
public interface ItemHandlerManagerView {

    ItemHandlerManager mekaddon$getItemHandlerManager();
}
