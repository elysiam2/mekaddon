package com.mekaddon.mixin;

import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.FactoryBaseView;
import com.mekaddon.slot.CombiningSharedSecondarySlot;
import com.mekaddon.slot.ExtraRowSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
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
import org.spongepowered.asm.mixin.injection.Redirect;
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

    /** The per-process secondary slots, in process order. Created once and kept; only their activation changes. */
    @Unique
    private List<IInventorySlot> mekaddon$secondary = List.of();

    /** Input handlers for {@link #mekaddon$secondary}, used to re-point {@link #extraInputHandler}. */
    @Unique
    private IInputHandler<ItemStack>[] mekaddon$secondaryHandlers;

    /** The machine's own shared secondary slot, remembered while the row is the one in use. */
    @Unique
    private InputInventorySlot mekaddon$vanillaExtraSlot;

    /** The handler vanilla built for {@link #mekaddon$vanillaExtraSlot}, restored verbatim when the row steps aside. */
    @Unique
    private IInputHandler<ItemStack> mekaddon$vanillaExtraHandler;

    /** Whether the vanilla recipe fields currently point at the row rather than at the shared slot. */
    @Unique
    private boolean mekaddon$rowActive;

    // ------------------------------------------------------------------ lifecycle

    @Override
    public boolean mekaddon$rowActive() {
        return mekaddon$rowActive;
    }

    @Override
    public List<IInventorySlot> mekaddon$installRow() {
        mekaddon$createRow();
        if (mekaddon$secondary.isEmpty() || mekaddon$rowActive) {
            return mekaddon$secondary;
        }
        if (mekaddon$vanillaExtraSlot == null) {
            // Remember vanilla's own slot and handler before the fields move: the shared slot is what the machine
            // falls back to, and it is never destroyed, only re-pointed away from.
            mekaddon$vanillaExtraSlot = extraSlot;
            mekaddon$vanillaExtraHandler = extraInputHandler;
        }
        mekaddon$rowActive = true;
        // Process 0 is the one asked for most often, and the fallback for the paths that carry no process index.
        mekaddon$selectProcess(0);
        return mekaddon$secondary;
    }

    @Override
    public void mekaddon$uninstallRow() {
        if (!mekaddon$rowActive) {
            return;
        }
        mekaddon$rowActive = false;
        // Restore vanilla's own slot and handler, so the machine behaves exactly as it did before the upgrade. The row
        // slots stay in the holder - they are simply not offered to a menu, or to automation, while inactive
        // (see AddonExtraInputSlot#createContainerSlot).
        if (mekaddon$vanillaExtraSlot != null) {
            extraSlot = mekaddon$vanillaExtraSlot;
            if (mekaddon$vanillaExtraHandler != null) {
                extraInputHandler = mekaddon$vanillaExtraHandler;
            }
        }
    }

    /**
     * Builds the per-process row once and makes it part of the machine.
     * <p>
     * The slots are created and registered here rather than in {@code addSlots} for one practical reason: the
     * container's slot list is fixed when the menu is built, so as long as the slots are in the holder before that,
     * they are in the menu. Doing it lazily keeps an un-upgraded machine's inventory exactly as vanilla made it.
     */
    @Unique
    private void mekaddon$createRow() {
        if (!mekaddon$secondary.isEmpty()) {
            return;
        }
        TileEntityCombiningFactory self = (TileEntityCombiningFactory) (Object) this;
        FactoryBaseView base = (FactoryBaseView) (Object) this;
        ProcessInfo[] processInfo = base.mekaddon$getProcessInfoSlots();
        FactoryRecipeCacheLookupMonitor<?>[] monitors = base.mekaddon$getRecipeCacheLookupMonitors();
        // markForSave() satisfies IContentsListener (onContentsChanged), and is only a fallback for the (unlikely) case
        // of a monitor not being a listener - the per-process monitor is what actually triggers a recipe recheck.
        IContentsListener fallback = ((TileEntityUpdateable) (Object) this)::markForSave;

        List<IInventorySlot> created = ExtraRowSlots.createRow(self, processInfo, monitors, fallback);
        @SuppressWarnings("unchecked")
        IInputHandler<ItemStack>[] handlers = new IInputHandler[created.size()];
        for (int i = 0; i < created.size(); i++) {
            handlers[i] = (IInputHandler<ItemStack>) InputHelper.getInputHandler(created.get(i),
                  RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
        }
        mekaddon$secondary = created;
        mekaddon$secondaryHandlers = handlers;
    }

    @Override
    public List<IInventorySlot> mekaddon$secondarySlots() {
        return List.copyOf(mekaddon$secondary);
    }

    @Override
    public IInventorySlot mekaddon$sharedSecondarySlot() {
        // The machine's own slot, whatever the current state: while the row is active the vanilla field points at the
        // row's first process instead, so remember which object it was.
        return mekaddon$vanillaExtraSlot != null ? mekaddon$vanillaExtraSlot : extraSlot;
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
    public List<IInventorySlot> mekaddon$inputSlots() {
        List<IInventorySlot> inputs = new ArrayList<>();
        for (ProcessInfo info : ((FactoryBaseView) (Object) this).mekaddon$getProcessInfoSlots()) {
            if (info.inputSlot() != null) {
                inputs.add(info.inputSlot());
            }
        }
        return inputs;
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

    // ------------------------------------------------------------------ the machine's own shared secondary slot

    /**
     * Gives the machine's single shared secondary ("yellow") slot the "Extra Stack Upgrade" multiplier.
     * <p>
     * That slot — not the per-process row — is what the stack upgrade enlarges when it is installed on its own, which
     * the new mutual exclusion makes the normal case. The slot is built here, inside {@code addSlots}, so substituting
     * it at this exact moment is safe: nothing has been registered with it yet (the holder, the side config and the
     * recipe handlers all take it from the field the vanilla code writes the redirect's return value into). Replacing
     * a slot <i>after</i> the machine has been built is what breaks those references, and that is not what happens
     * here.
     * <p>
     * Only this one call is redirected — the redirect is scoped to {@code addSlots}, and the main input slots are
     * created by the superclass call, so they keep their vanilla limits.
     */
    @Redirect(
          method = "addSlots(Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)V",
          at = @At(value = "INVOKE",
                target = "Lmekanism/common/inventory/slot/InputInventorySlot;at(Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lmekanism/common/inventory/slot/InputInventorySlot;"),
          remap = false
    )
    private InputInventorySlot mekaddon$sharedSecondarySlot(Predicate<ItemStack> valid, IContentsListener listener,
          int x, int y) {
        // Declared to return the exact type the redirected call returns (the instance is a subclass): a redirect
        // handler's descriptor has to line up with the call it replaces.
        return CombiningSharedSecondarySlot.create((TileEntityMekanism) (Object) this, valid, listener, x, y);
    }
}
