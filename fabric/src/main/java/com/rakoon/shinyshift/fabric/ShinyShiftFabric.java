package com.rakoon.shinyshift.fabric;

import com.rakoon.shinyshift.ShinyShift;
import net.fabricmc.api.ModInitializer;

public class ShinyShiftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ShinyShift.init();
    }
}
