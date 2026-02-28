package com.rakoon.shinyshift.fabric.mixin;

import com.rakoon.shinyshift.items.ShinyShiftItems;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/item/ItemDisplayContext;"
                    + "ZLcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At("HEAD"),
            ordinal = 0
    )

    private int shinyshift$forceFullbright(int packedLight, ItemStack stack) {
        if (stack.is(ShinyShiftItems.SHINY_EXCHANGE_GEM.get())) {
            return 0xF000F0; // fullbright
        }
        return packedLight;
    }
}