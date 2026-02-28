package com.rakoon.shinyshift;

import net.minecraft.resources.ResourceLocation;

public final class ShinyShiftConfig {

    private ShinyShiftConfig() {}

    public static boolean CONSUME_ITEM = true;
    public static boolean PLAY_SOUND = true;

    public static final ResourceLocation SHINY_SOUND =
            ResourceLocation.fromNamespaceAndPath("shinyshift", "shiny");
}