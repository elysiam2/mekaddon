package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.FactoryBaseView;
import com.mekaddon.slot.ExtraRowSlots;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.factory.TileEntityCombiningFactory;
import mekanism.common.tile.factory.TileEntityFactory.ProcessInfo;
import mekanism.common.upgrade.CombinerUpgradeData;
import mekanism.common.upgrade.IUpgradeData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns the Combining Factory's single shared secondary ("yellow") input slot into one secondary input per process,
 * laid out in a row directly below the main inputs.
 * <p>
 * Vanilla models the secondary input as <b>one</b> slot for the whole machine: {@code extraSlot} plus the single
 * {@code extraInputHandler} derived from it, consumed by every process's cached recipe. The upgrade replaces it with a
 * per-process row, but the recipe machinery still reads those two fields — so instead of duplicating that machinery,
 * every code path that already knows the process index re-points the fields at that process's own slot before letting
 * the vanilla logic run ({@link #mekaddon$selectProcess(int)}).
 * <p>
 * The fields really are re-pointable: {@code extraSlot} is not final, and {@code extraInputHandler} only is.
 * <p>
 * This mixin deliberately does <b>not</b> create or remove anything inside {@code addSlots}: the row is installed and
 * removed later, by {@link com.mekaddon.slot.ExtraRowLayout}, which appends to (or removes from) the live inventory
 * list without replacing any existing slot. See that class for why replacing slots breaks the slot colours and the
 * output-space tracking.
 */
@Mixin(TileEntityCombiningFactory.class)
public abstract class CombiningFactoryExtraSlotsMixin implements CombiningRowTile {

    @Shadow
    private InputInventorySlot extraSlot;

    @Shadow
    @Final
    @Mutable
    private IInputHandler<ItemStack> extraInputHandler;

    /** The per-process secondary slots currently installed, in process order. Empty while not upgraded. */
    @Unique
    private List<IInventorySlot> mekaddon$secondary = List.of();

    /** Input handlers for {@link #mekaddon$secondary}, used to re-point {@link #extraInputHandler}. */
    @Unique
    private IInputHandler<ItemStack>[] mekaddon$secondaryHandlers;

    /** The machine's own shared secondary slot, captured before the row takes those fields over. */
    @Unique
    private InputInventorySlot mekaddon$vanillaExtraSlot;

    /** The handler vanilla built for {@link #mekaddon$vanillaExtraSlot}, restored verbatim on uninstall. */
    @Unique
    private IInputHandler<ItemStack> mekaddon$vanillaExtraHandler;

    // ------------------------------------------------------------------ lifecycle

    @Override
    public List<IInventorySlot> mekaddon$installRow() {
        if (!mekaddon$secondary.isEmpty()) {
            return List.of();
        }
        // Capture vanilla's state first: both fields are about to be re-pointed at the row.
        mekaddon$vanillaExtraSlot = extraSlot;
        mekaddon$vanillaExtraHandler = extraInputHandler;

        FactoryBaseView base = (FactoryBaseView) (Object) this;
        ProcessInfo[] processInfo = base.mekaddon$getProcessInfoSlots();
        FactoryRecipeCacheLookupMonitor<?>[] monitors = base.mekaddon$getRecipeCacheLookupMonitors();
        // markForSave() satisfies IContentsListener (onContentsChanged), and is only a fallback for the (unlikely) case
        // of a monitor not being a listener - the per-process monitor is what actually triggers a recipe recheck.
        IContentsListener fallback = ((mekanism.common.tile.base.TileEntityUpdateable) (Object) this)::markForSave;

        List<IInventorySlot> created = ExtraRowSlots.createRow((TileEntityCombiningFactory) (Object) this, processInfo,
              monitors, fallback);
        @SuppressWarnings("unchecked")
        IInputHandler<ItemStack>[] handlers = new IInputHandler[created.size()];
        for (int i = 0; i < created.size(); i++) {
            handlers[i] = (IInputHandler<ItemStack>) InputHelper.getInputHandler(created.get(i),
                  RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
        }
        mekaddon$secondary = created;
        mekaddon$secondaryHandlers = handlers;
        // Process 0 is the one asked for most often, and the fallback for the paths that carry no process index.
        mekaddon$selectProcess(0);
        return created;
    }

    @Override
    public IInventorySlot mekaddon$uninstallRow() {
        if (mekaddon$secondary.isEmpty()) {
            return null;
        }
        mekaddon$secondary = List.of();
        mekaddon$secondaryHandlers = null;
        // Restore vanilla's own slot and handler, so the machine behaves exactly as it did before the upgrade.
        InputInventorySlot vanilla = mekaddon$vanillaExtraSlot;
        if (vanilla != null) {
            extraSlot = vanilla;
            if (mekaddon$vanillaExtraHandler != null) {
                extraInputHandler = mekaddon$vanillaExtraHandler;
            }
        }
        mekaddon$vanillaExtraSlot = null;
        mekaddon$vanillaExtraHandler = null;
        return vanilla;
    }

    @Override
    public List<IInventorySlot> mekaddon$secondarySlots() {
        return List.copyOf(mekaddon$secondary);
    }

    @Override
    public IInventorySlot mekaddon$sharedSecondarySlot() {
        return extraSlot;
    }

    @Override
    public List<IInventorySlot> mekaddon$outputSlots() {
        List<IInventorySlot> outputs = new ArrayList<>();
        for (ProcessInfo info : ((FactoryBaseView) (Object) this).mekaddon$getProcessInfoSlots()) {
            if (info.outputSlot() != null) {
                outputs.add(info.outputSlot());
            }
            if (info.secondaryOutputSlot() != null) {
                outputs.add(info.secondaryOutputSlot());
            }
        }
        return outputs;
    }

    @Override
    public String mekaddon$describe() {
        return "TileEntityCombiningFactory";
    }

    // ------------------------------------------------------------------ recipe plumbing

    @Override
    public void mekaddon$selectProcess(int process) {
        if (mekaddon$secondaryHandlers == null || process < 0 || process >= mekaddon$secondary.size()) {
            return;
        }
        IInventorySlot slot = mekaddon$secondary.get(process);
        if (slot instanceof InputInventorySlot input) {
            extraSlot = input;
        }
        extraInputHandler = mekaddon$secondaryHandlers[process];
    }

    @Inject(method = "getRecipe(I)Lmekanism/api/recipes/CombinerRecipe;", at = @At("HEAD"), remap = false)
    private void mekaddon$selectForGetRecipe(int process, CallbackInfoReturnable<CombinerRecipe> cir) {
        mekaddon$selectProcess(process);
    }

    @Inject(method = "createNewCachedRecipe(Lmekanism/api/recipes/CombinerRecipe;I)Lmekanism/api/recipes/cache/CachedRecipe;",
          at = @At("HEAD"), remap = false)
    private void mekaddon$selectForCachedRecipe(CombinerRecipe recipe, int process,
          CallbackInfoReturnable<CachedRecipe<CombinerRecipe>> cir) {
        mekaddon$selectProcess(process);
    }

    @Inject(method = "findRecipe(ILnet/minecraft/world/item/ItemStack;Lmekanism/api/inventory/IInventorySlot;Lmekanism/api/inventory/IInventorySlot;)Lmekanism/api/recipes/CombinerRecipe;",
          at = @At("HEAD"), remap = false)
    private void mekaddon$selectForFindRecipe(int process, ItemStack mainInput, IInventorySlot mainSlot,
          IInventorySlot outputSlot, CallbackInfoReturnable<CombinerRecipe> cir) {
        mekaddon$selectProcess(process);
    }

    /**
     * While upgraded, a cached recipe stays valid when the process's own secondary slot is empty (the machine then
     * reports {@code NOT_ENOUGH_SECONDARY_INPUT} itself) or holds an item the recipe accepts. Mirrors the vanilla body
     * of {@code TileEntityCombiningFactory#isCachedRecipeValid}, which is hard-wired to the single shared slot.
     */
    @Inject(method = "isCachedRecipeValid(Lmekanism/api/recipes/cache/CachedRecipe;Lnet/minecraft/world/item/ItemStack;)Z",
          at = @At("HEAD"), cancellable = true, remap = false)
    private void mekaddon$cachedRecipeValid(CachedRecipe<CombinerRecipe> cachedRecipe, ItemStack mainInput,
          CallbackInfoReturnable<Boolean> cir) {
        if (mekaddon$secondary.isEmpty()) {
            return;
        }
        if (cachedRecipe == null) {
            cir.setReturnValue(false);
            return;
        }
        CombinerRecipe recipe = cachedRecipe.getRecipe();
        if (!recipe.getMainInput().testType(mainInput)) {
            cir.setReturnValue(false);
            return;
        }
        InputInventorySlot secondary = extraSlot;
        cir.setReturnValue(secondary == null || secondary.isEmpty() || recipe.getExtraInput().testType(secondary.getStack()));
    }

    /**
     * The saved/restored "extra" slot must always be the same column, otherwise breaking the machine would shuffle one
     * column's secondary ingredient into another (duplicating or destroying items), because every secondary slot is
     * also part of the normal inventory persistence.
     */
    @Inject(method = "getUpgradeData(Lnet/minecraft/core/HolderLookup$Provider;)Lmekanism/common/upgrade/CombinerUpgradeData;",
          at = @At("HEAD"), remap = false)
    private void mekaddon$selectForUpgradeData(HolderLookup.Provider provider,
          CallbackInfoReturnable<CombinerUpgradeData> cir) {
        mekaddon$selectProcess(0);
    }

    @Inject(method = "parseUpgradeData(Lnet/minecraft/core/HolderLookup$Provider;Lmekanism/common/upgrade/IUpgradeData;)V",
          at = @At("HEAD"), remap = false)
    private void mekaddon$selectForParseUpgradeData(HolderLookup.Provider provider, IUpgradeData data, CallbackInfo ci) {
        mekaddon$selectProcess(0);
    }
}
