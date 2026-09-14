package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningExtraRowView;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.common.tile.factory.TileEntityFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Selects the right process's secondary input before anything validates or re-uses a cached recipe.
 * <p>
 * {@code getCachedRecipe(int)} is the one private helper both call sites of {@code isCachedRecipeValid} go through —
 * {@code getRecipeForInput} (the normal recipe lookup) and {@code sortInventory} (the factory's "distribute evenly /
 * sorting" pass). Hooking it covers both with a single injection, which matters because those are exactly the paths
 * that have no other way of telling the mod which column is being asked about: the second parameter of
 * {@code isCachedRecipeValid} is only the main input stack.
 * <p>
 * Without this, sorting would validate each column against whichever secondary slot happened to be selected last, and
 * the even-distribution logic would refuse to spread items across columns that actually do have a matching recipe.
 */
@Mixin(TileEntityFactory.class)
public abstract class FactoryCachedRecipeMixin {

    @Inject(method = "getCachedRecipe", at = @At("HEAD"), remap = false)
    private void mekaddon$selectProcessForCachedRecipe(int process, CallbackInfoReturnable<CachedRecipe<?>> cir) {
        if ((Object) this instanceof CombiningExtraRowView view) {
            view.mekaddon$selectProcess(process);
        }
    }
}
