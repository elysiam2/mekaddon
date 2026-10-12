package com.mekaddon.mixin.extras;

import com.jerry.mekextras.common.inventory.slot.StackableInputInventorySlot;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraCombiningFactory;
import com.jerry.mekextras.common.tile.factory.TileEntityExtraFactory;
import com.mekaddon.bridge.CombiningRowTile;
import com.mekaddon.bridge.ExtraFactoryView;
import com.mekaddon.bridge.SlotPositionView;
import com.mekaddon.slot.ExtraRowSlots;
import com.mekaddon.slot.ExtrasStackedInputSlot;
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
import mekanism.common.recipe.lookup.monitor.FactoryRecipeCacheLookupMonitor;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
            mekaddon$vanillaExtraSlot = extraSlot;
            mekaddon$vanillaExtraHandler = extraInputHandler;
        }
        mekaddon$rowActive = true;
        mekaddon$selectProcess(0);
        return mekaddon$secondary;
    }

    @Override
    public void mekaddon$uninstallRow() {
        if (!mekaddon$rowActive) {
            return;
        }
        mekaddon$rowActive = false;
        if (mekaddon$vanillaExtraSlot != null) {
            extraSlot = mekaddon$vanillaExtraSlot;
            if (mekaddon$vanillaExtraHandler != null) {
                extraInputHandler = mekaddon$vanillaExtraHandler;
            }
        }
    }

    /** Builds the per-process row once and makes it part of the machine; see the Mekanism-side twin for why. */
    @Unique
    private void mekaddon$createRow() {
        if (!mekaddon$secondary.isEmpty()) {
            return;
        }
        FactoryRecipeCacheLookupMonitor<?>[] monitors = monitors();
        TileEntityExtraFactory.ProcessInfo[] infos = processInfos();
        ExtraFactoryTier tier = extraTier();
        TileEntityExtraCombiningFactory self = (TileEntityExtraCombiningFactory) (Object) this;
        IContentsListener fallback = self::markForSave;
        int processes = Math.min(tier.processes, infos.length);

        List<IInventorySlot> created = new ArrayList<>(processes);
        for (int i = 0; i < processes; i++) {
            IInventorySlot mainInput = infos[i].inputSlot();
            // The per-process monitor, wrapped exactly the way Extras wraps the listeners of its own input slots: a
            // change here has to schedule the same sorting pass a change to a main input would, or the machine stops
            // redistributing as soon as the extra row exists.
            IContentsListener inner = i < monitors.length && monitors[i] instanceof IContentsListener cl
                  ? cl : fallback;
            IContentsListener listener = (IContentsListener) ((ExtraFactoryView) this).mekaddon$markAllMonitors(inner);
            // Column position comes from the main input slot that already exists, so any tier - including tiers this
            // mod has never heard of - lines up without a per-tier table.
            int x = mainInput instanceof SlotPositionView position ? position.mekaddon$slotX() : 27 + i * 19;
            // Not the vanilla StackableInputInventorySlot: the mod's own subclass adds the "Extra Stack Upgrade"
            // multiplier (x2 per upgrade, up to x256) on top of Extras' tier scaling.
            created.add(ExtrasStackedInputSlot.createRow(self, tier,
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
    public List<IInventorySlot> mekaddon$inputSlots() {
        List<IInventorySlot> inputs = new ArrayList<>();
        for (TileEntityExtraFactory.ProcessInfo info : processInfos()) {
            if (info.inputSlot() != null) {
                inputs.add(info.inputSlot());
            }
        }
        return inputs;
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

    // ------------------------------------------------------------------ the machine's own shared secondary slot

    /**
     * The Extras half of the same redirect the Mekanism-side mixin does: the machine's single shared secondary slot is
     * built as {@link ExtrasStackedInputSlot} instead of Extras' plain slot, so the "Extra Stack Upgrade" multiplier
     * applies to it — which, now that the two upgrades are mutually exclusive, is the normal way the multiplier is
     * used. See {@code CombiningFactoryExtraSlotsMixin#mekaddon$sharedSecondarySlot} for why substituting the slot at
     * creation time is safe while replacing one later is not.
     */
    @Redirect(
          method = "addSlots(Lmekanism/common/capabilities/holder/slot/InventorySlotHelper;Lmekanism/api/IContentsListener;Lmekanism/api/IContentsListener;)V",
          at = @At(value = "INVOKE",
                target = "Lcom/jerry/mekextras/common/inventory/slot/StackableInputInventorySlot;at(Lcom/jerry/mekextras/common/tier/ExtraFactoryTier;Ljava/util/function/Predicate;Lmekanism/api/IContentsListener;II)Lcom/jerry/mekextras/common/inventory/slot/StackableInputInventorySlot;"),
          remap = false
    )
    private StackableInputInventorySlot mekaddon$sharedSecondarySlot(ExtraFactoryTier tier, Predicate<ItemStack> valid,
          IContentsListener listener, int x, int y) {
        // Declared to return the exact type the redirected call returns (the instance is a subclass).
        return ExtrasStackedInputSlot.create((TileEntityMekanism) (Object) this, tier, valid, listener, x, y);
    }
}
