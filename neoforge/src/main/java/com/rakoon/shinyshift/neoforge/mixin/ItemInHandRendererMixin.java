package com.rakoon.shinyshift.neoforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rakoon.shinyshift.items.ShinyShiftItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @ModifyVariable(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            ordinal = 0
    )
    private int shinyshift$forceFullbrightFirstPerson(
            int packedLight,
            AbstractClientPlayer player,
            float g,
            float h,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int originalLight
    ) {
        if (stack.is(ShinyShiftItems.SHINY_EXCHANGE_GEM.get())) {
            return 0xF000F0;
        }
        return packedLight;
    }
}