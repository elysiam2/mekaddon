package com.mekaddon.registry;

import com.mekaddon.MekAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
          DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekAddon.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
          () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mekaddon"))
                .icon(() -> new ItemStack(ModItems.COMBINING_SLOT_UPGRADE.get()))
                .displayItems((parameters, output) -> {
                    output.accept(ModItems.COMBINING_SLOT_UPGRADE.get());
                    output.accept(ModItems.EXTRA_STACK_UPGRADE.get());
                })
                .build());

    private ModCreativeTabs() {
    }
}
