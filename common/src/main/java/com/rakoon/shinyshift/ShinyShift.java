package com.rakoon.shinyshift;

import com.rakoon.shinyshift.items.ShinyShiftItems;
import com.rakoon.shinyshift.effect.EvolutionEffectHandler;
import com.rakoon.shinyshift.registry.ShinyShiftTabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class ShinyShift {

    public static final String MOD_ID = "shinyshift";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        ShinyShiftItems.register();
        ShinyShiftTabs.register();
        EvolutionEffectHandler.init();
    }
}
