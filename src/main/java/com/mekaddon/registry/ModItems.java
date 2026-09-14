package com.mekaddon.registry;

import com.mekaddon.MekAddon;
import com.mekaddon.item.ItemCombiningSlotUpgrade;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekAddon.MODID);

    public static final DeferredItem<ItemCombiningSlotUpgrade> COMBINING_SLOT_UPGRADE = ITEMS.registerItem(
          "combining_slot_upgrade",
          ItemCombiningSlotUpgrade::new,
          new Item.Properties());

    private ModItems() {
    }
}
