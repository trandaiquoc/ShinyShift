package com.rakoon.shinyshift;

import com.rakoon.shinyshift.items.ShinyShiftItems;
import com.rakoon.shinyshift.effect.EvolutionEffectHandler;
import com.rakoon.shinyshift.registry.ShinyShiftTabs;

public class ShinyShift {

    public static final String MOD_ID = "shinyshift";

    public static void init() {
        ShinyShiftItems.register();
        EvolutionEffectHandler.init();
        ShinyShiftTabs.register();
    }
}
