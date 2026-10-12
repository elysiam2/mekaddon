package com.mekaddon.mixin;

import com.mekaddon.bridge.InsertGuardView;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.common.inventory.slot.BasicInventorySlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the mod add one extra acceptance rule to a slot it does not own.
 * <p>
 * The rule is consulted <b>after</b> the slot's own answer, so it can only ever turn "yes" into "no": a slot's built-in
 * validation stays authoritative about what it would accept in principle, and the guard only narrows that down at
 * runtime.
 * <p>
 * Both entry points are hooked because they are not the same question:
 * <ul>
 *     <li>{@code isItemValidForInsertion} is what {@code BasicInventorySlot#insertItem} asks, so it covers the mod's own
 *     insertions, item automation and — via {@code InventoryContainerSlot#mayPlace}, which probes the slot with a
 *     simulated insert — the player dragging or shift-clicking the item in;</li>
 *     <li>{@code isItemValid} is the plain "would you take this?" query other code uses for warnings and previews.</li>
 * </ul>
 * Slots with no guard installed (every slot in the game except the one the mod registers one on) pay a single null
 * check.
 */
@Mixin(BasicInventorySlot.class)
public abstract class BasicInventorySlotGuardMixin implements InsertGuardView {

    @Unique
    private Predicate<ItemStack> mekaddon$insertGuard;

    @Override
    public void mekaddon$setInsertGuard(Predicate<ItemStack> guard) {
        this.mekaddon$insertGuard = guard;
    }

    @Inject(method = "isItemValidForInsertion", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$guardInsertion(ItemStack stack, AutomationType automationType,
          CallbackInfoReturnable<Boolean> cir) {
        mekaddon$applyInsertGuard(stack, cir);
    }

    @Inject(method = "isItemValid", at = @At("RETURN"), cancellable = true, remap = false)
    private void mekaddon$guardValidation(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        mekaddon$applyInsertGuard(stack, cir);
    }

    @Unique
    private void mekaddon$applyInsertGuard(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && mekaddon$insertGuard != null && mekaddon$insertGuard.test(stack)) {
            cir.setReturnValue(false);
        }
    }
}
