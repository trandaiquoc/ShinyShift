package com.rakoon.shinyshift.neoforge;

import com.rakoon.shinyshift.ShinyShift;
import net.neoforged.fml.common.Mod;

@Mod(ShinyShift.MOD_ID)
public class ShinyShiftNeoForge {

    public ShinyShiftNeoForge() {
        // gọi entry chung
        ShinyShift.init();
    }
}
