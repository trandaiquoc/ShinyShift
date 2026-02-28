package com.rakoon.shinyshift.items;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.rakoon.shinyshift.effect.EvolutionEffectHandler;
import com.rakoon.shinyshift.structure.AltarTemplateLoader;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;
import net.minecraft.MethodsReturnNonnullByDefault;

public class ShinyExchangeGemItem extends Item {

    private static final int COLOR_START = 0x00FFFF;
    private static final int COLOR_MID   = 0xFF00FF;
    private static final int COLOR_END   = 0xAA00FF;

    public ShinyExchangeGemItem(Properties properties) {
        super(properties);
    }

    // =========================
    // GRADIENT NAME
    // =========================
    @Override
    @MethodsReturnNonnullByDefault
    public Component getName(ItemStack stack) {

        String text = Component
                .translatable("item.shinyshift.shiny_exchange_gem")
                .getString();

        int length = text.length();
        if (length == 0) return Component.empty();

        MutableComponent result = Component.empty();

        float divisor = length == 1 ? 1f : (length - 1);

        for (int i = 0; i < length; i++) {

            float progress = i / divisor;
            int color = getGradientColor(progress);

            result.append(
                    Component.literal(String.valueOf(text.charAt(i)))
                            .withStyle(style ->
                                    style.withColor(color).withBold(true)
                            )
            );
        }

        return result;
    }

    private static int getGradientColor(float t) {
        return (t < 0.5f)
                ? blendColor(COLOR_START, COLOR_MID, t * 2f)
                : blendColor(COLOR_MID, COLOR_END, (t - 0.5f) * 2f);
    }

    private static int blendColor(int a, int b, float t) {

        int ar = (a >> 16) & 255;
        int ag = (a >> 8) & 255;
        int ab = a & 255;

        int br = (b >> 16) & 255;
        int bg = (b >> 8) & 255;
        int bb = b & 255;

        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | bl;
    }

    // =========================
    // ALWAYS GLINT and LIGHT
    // =========================
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // =========================
    // TOOLTIP
    // =========================
    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.shinyshift.only_owner")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("tooltip.shinyshift.not_wild")
                .withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.translatable("tooltip.shinyshift.consume")
                .withStyle(ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.shinyshift.shiny_only")
                .withStyle(ChatFormatting.GOLD));
    }

    // =========================
    // MAIN LOGIC
    // =========================
    @MethodsReturnNonnullByDefault
    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(target instanceof PokemonEntity pokemonEntity))
            return InteractionResult.PASS;

        if (player.level().isClientSide())
            return InteractionResult.SUCCESS;

        Pokemon pokemon = pokemonEntity.getPokemon();

        // Fast fail chain (ít branch lồng nhau hơn)
        if (pokemon.isWild())
            return InteractionResult.FAIL;

        UUID owner = pokemon.getOwnerUUID();
        if (owner == null || !owner.equals(player.getUUID()))
            return InteractionResult.FAIL;

        if (pokemon.getShiny())
            return InteractionResult.SUCCESS;

        ServerLevel level = (ServerLevel) player.level();

        BlockPos stonePos = pokemonEntity.blockPosition().below();

        if (!AltarTemplateLoader.matches(level, stonePos))
            return InteractionResult.FAIL;

        /*
         * Không shrink item ở đây — thay vào đó đăng kí pending consumption.
         * Truyền player's UUID + hand (không giữ tham chiếu Player/ItemStack).
         */
        EvolutionEffectHandler.start(level, pokemonEntity, player.getUUID(), hand);

        return InteractionResult.SUCCESS;
    }
}