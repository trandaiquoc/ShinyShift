package com.rakoon.shinyshift.items;

import com.rakoon.shinyshift.ShinyShift;
import com.rakoon.shinyshift.registry.ShinyShiftTabs;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ShinyShiftItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ShinyShift.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> SHINY_EXCHANGE_GEM =
            ITEMS.register(
                    "shiny_exchange_gem",
                    () -> new ShinyExchangeGemItem(
                            new Item.Properties().stacksTo(16)
                                    .arch$tab(ShinyShiftTabs.MAIN_TAB) // ← tab được gắn ở đây
                    )
            );

    public static void register() {
        ITEMS.register();
    }
}
