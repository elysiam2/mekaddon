package com.mekaddon;

import com.mojang.logging.LogUtils;
import com.mekaddon.registry.ModCreativeTabs;
import com.mekaddon.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mekanism Combining Factory Slot Addon.
 * <p>
 * Adds the "Combining Slot Upgrade" item which, when used (Shift + right-click) on a Mekanism
 * Combining Factory, permanently unlocks an extra row of slots the same size as the factory's input row.
 */
@Mod(MekAddon.MODID)
public class MekAddon {

    public static final String MODID = "mekaddon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MekAddon(ModContainer modContainer, IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        LOGGER.info("MekAddon (Combining Factory slot upgrade) initialized.");
    }
}
