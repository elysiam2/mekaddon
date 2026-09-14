package com.mekaddon.mixin;

import com.mekaddon.MekAddon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Last line of defence against a client/server slot-count mismatch.
 * <p>
 * {@code AbstractContainerMenu#initializeContents} walks the incoming slot contents and calls {@code getSlot(i)} for
 * every one of them. If the server's container is larger than the client's menu — which can happen when a GUI is open
 * while the machine's inventory is rebuilt — that throws {@code IndexOutOfBoundsException} and NeoForge disconnects the
 * player with a "Network Protocol Error" report. Three separate rounds of this addon's development ended in exactly
 * that kick.
 * <p>
 * The real fix is to keep the two sides in step (see {@code SlotRebuilder} and {@code UpdateTagReadMixin}); this mixin
 * only converts the remaining race into a logged warning plus a menu that is missing the tail of its contents, instead
 * of a disconnect. It never changes behaviour when the counts match, and it never triggers for a smaller packet
 * (vanilla simply leaves the extra slots alone).
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerContentGuardMixin {

    @Inject(
          method = "initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V",
          at = @At("HEAD"),
          cancellable = true
    )
    private void mekaddon$guardSlotCount(int stateId, List<ItemStack> items, ItemStack carried, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        int slots = menu.slots.size();
        if (items.size() <= slots) {
            return;
        }
        MekAddon.LOGGER.error(
              "Server sent {} slot contents for a {}-slot menu ({}); truncating to avoid a disconnect. "
                    + "The GUI layout is out of sync - reopen it.",
              items.size(), slots, menu.getClass().getSimpleName());
        // Applying the matching prefix is strictly better than dropping the whole packet: everything the client does
        // have room for still lands.
        menu.initializeContents(stateId, new ArrayList<>(items.subList(0, slots)), carried);
        ci.cancel();
    }
}
