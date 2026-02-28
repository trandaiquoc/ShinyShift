package com.rakoon.shinyshift.registry;

import com.rakoon.shinyshift.ShinyShift;
import com.rakoon.shinyshift.items.ShinyShiftItems;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ShinyShiftTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(ShinyShift.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> MAIN_TAB =
            TABS.register("main",
                    () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .title(Component.translatable("itemGroup.shinyshift.main"))
                            .icon(() -> new ItemStack(ShinyShiftItems.SHINY_EXCHANGE_GEM.get()))
                            .displayItems((parameters, output) ->
                                    output.accept(ShinyShiftItems.SHINY_EXCHANGE_GEM.get())
                            )
                            .build()
            );

    public static void register() {
        TABS.register();
    }
}