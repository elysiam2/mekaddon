package com.mekaddon.bridge;

/**
 * Duck-typed view onto {@code AbstractContainerScreen}, implemented by
 * {@code AbstractContainerScreenAccessor}.
 * <p>
 * A plain interface keeps the GuiFactory mixin free of any compile-time dependency on another Mixin class.
 */
public interface ContainerScreenLayoutView {

    int mekaddon$getImageHeight();

    void mekaddon$setImageHeight(int height);

    int mekaddon$getInventoryLabelY();

    void mekaddon$setInventoryLabelY(int y);
}
