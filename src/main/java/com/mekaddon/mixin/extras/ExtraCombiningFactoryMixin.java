package com.mekaddon.mixin.extras;

import com.jerry.mekextras.common.inventory.slot.StackableInputInventorySlot;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraCombiningFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.ExtraFactoryView;
import com.mekaddon.bridge.SlotPositionView;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.IContentsListener;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.api.recipes.cache.CachedRecipe;
import mekanism.api.recipes.cache.CachedRecipe.OperationTracker.RecipeError;
import mekanism.api.recipes.inputs.IInputHandler;
import mekanism.api.recipes.inputs.InputHelper;
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The same per-process secondary-input row as {@code CombiningFactoryExtraSlotsMixin}, for Mekanism Extras' Combining
 * Factory (the "Absolute" tier and up).
 * <p>
 * Extras' machine could not be reused as-is because it is an independent implementation of Mekanism's factory
 * framework: {@code TileEntityExtraCombiningFactory} derives from its own {@code TileEntityExtraItemToItemFactory}
 * rather than from {code TileEntityCombiningFactory}, and its secondary slot is a
 * {@link StackableInputInventorySlot}. Everything that matters is nevertheless shaped identically — one shared
 * {@code extraSlot} plus one {@code extraInputHandler} consumed by every process, {@code ProcessInfo} with
 * {@code inputSlot()}/{@code outputSlot()}, the same 27px column base and 19px spacing, inputs at y=13, outputs at
 * y=57 — so the same technique applies: re-point the two fields per process instead of duplicating the recipe
 * machinery.
 * <p>
 * The target is given as a string so the mixin is skipped when Extras is not installed.
 */
@Mixin(targets = "com.jerry.mekextras.common.tile.factory.TileEntityExtraCombiningFactory")
public abstract class ExtraCombiningFactoryMixin implements CombiningRowTile {

    @Shadow
    StackableInputInventorySlot extraSlot;

    @Shadow
    @Final
    @Mutable
    private IInputHandler<ItemStack> extraInputHandler;

    @Unique
    private List<IInventorySlot> mekaddon$secondary = List.of();

    @Unique
    private IInputHandler<ItemStack>[] mekaddon$secondaryHandlers;

    @Unique
    private StackableInputInventorySlot mekaddon$vanillaExtraSlot;

    @Unique
    private IInputHandler<ItemStack> mekaddon$vanillaExtraHandler;

    // ------------------------------------------------------------------ lifecycle

    @Override
    public List<IInventorySlot> mekaddon$installRow() {
        if (!mekaddon$secondary.isEmpty()) {
            return List.of();
        }
        mekaddon$vanillaExtraSlot = extraSlot;
        mekaddon$vanillaExtraHandler = extraInputHandler;

        FactoryRecipeCacheLookupMonitor<?>[] monitors = monitors();
        TileEntityExtraFactory.ProcessInfo[] infos = processInfos();
        ExtraFactoryTier tier = extraTier();
        TileEntityExtraCombiningFactory self = (TileEntityExtraCombiningFactory) (Object) this;
        IContentsListener fallback = ((TileEntityExtraCombiningFactory) (Object) this)::markForSave;
        int processes = Math.min(tier.processes, infos.length);

        List<IInventorySlot> created = new ArrayList<>(processes);
        for (int i = 0; i < processes; i++) {
            IInventorySlot mainInput = infos[i].inputSlot();
            IContentsListener listener = i < monitors.length && monitors[i] instanceof IContentsListener cl
                  ? cl : fallback;
            // Column position comes from the main input slot that already exists, so any tier - including tiers this
            // mod has never heard of - lines up without a per-tier table.
            int x = mainInput instanceof SlotPositionView position ? position.mekaddon$slotX() : 27 + i * 19;
            created.add(StackableInputInventorySlot.at(tier,
                  stack -> mainInput == null || mainInput.isEmpty()
                        ? self.containsRecipeB(stack)
                        : self.containsRecipeAB(mainInput.getStack(), stack),
                  listener, x, com.mekaddon.slot.ExtraRowSlots.SECONDARY_Y));
        }
        @SuppressWarnings("unchecked")
        IInputHandler<ItemStack>[] handlers = new IInputHandler[created.size()];
        for (int i = 0; i < created.size(); i++) {
            handlers[i] = (IInputHandler<ItemStack>) InputHelper.getInputHandler(created.get(i),
                  RecipeError.NOT_ENOUGH_SECONDARY_INPUT);
        }
        mekaddon$secondary = created;
        mekaddon$secondaryHandlers = handlers;
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
        StackableInputInventorySlot vanilla = mekaddon$vanillaExtraSlot;
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
        for (TileEntityExtraFactory.ProcessInfo info : processInfos()) {
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
        return "TileEntityExtraCombiningFactory";
    }

    // ------------------------------------------------------------------ recipe plumbing

    @Override
    public void mekaddon$selectProcess(int process) {
        if (mekaddon$secondaryHandlers == null || process < 0 || process >= mekaddon$secondary.size()) {
            return;
        }
        if (mekaddon$secondary.get(process) instanceof StackableInputInventorySlot slot) {
            extraSlot = slot;
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

    /** Same reasoning as the Mekanism-side override: vanilla's body is hard-wired to the single shared slot. */
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
        StackableInputInventorySlot secondary = extraSlot;
        cir.setReturnValue(secondary == null || secondary.isEmpty() || recipe.getExtraInput().testType(secondary.getStack()));
    }

    // ------------------------------------------------------------------ helpers

    @Unique
    private TileEntityExtraFactory.ProcessInfo[] processInfos() {
        Object[] infos = ((ExtraFactoryView) (Object) this).mekaddon$processInfos();
        return infos == null ? new TileEntityExtraFactory.ProcessInfo[0] : (TileEntityExtraFactory.ProcessInfo[]) infos;
    }

    @Unique
    private ExtraFactoryTier extraTier() {
        return (ExtraFactoryTier) ((ExtraFactoryView) (Object) this).mekaddon$extraTier();
    }

    @Unique
    private FactoryRecipeCacheLookupMonitor<?>[] monitors() {
        // The monitor array lives on Extras' own base class next to processInfoSlots; going through the tile's field
        // directly keeps this mixin independent of how Extras exposes it.
        Object[] monitors = ((ExtraFactoryView) (Object) this).mekaddon$monitors();
        return monitors == null ? new FactoryRecipeCacheLookupMonitor<?>[0] : (FactoryRecipeCacheLookupMonitor<?>[]) monitors;
    }
}
