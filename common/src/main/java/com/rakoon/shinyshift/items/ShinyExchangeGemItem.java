package com.rakoon.shinyshift.items;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.rakoon.shinyshift.ShinyShiftConfig;
import com.rakoon.shinyshift.effect.EvolutionEffectHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ShinyExchangeGemItem extends Item {

    public ShinyExchangeGemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        // ❌ Không phải Pokémon
        if (!(target instanceof PokemonEntity pokemonEntity)) {
            return InteractionResult.PASS;
        }

        // ❌ Client chỉ để animation
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        Pokemon pokemon = pokemonEntity.getPokemon();

        // ❌ Pokémon hoang dã
        if (pokemon.isWild()) {
            return InteractionResult.FAIL;
        }

        // ❌ Không phải Pokémon của player
        if (pokemon.getOwnerUUID() == null ||
                !pokemon.getOwnerUUID().equals(player.getUUID())) {
            return InteractionResult.FAIL;
        }

        // ❌ Đã shiny
        if (pokemon.getShiny()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel level = (ServerLevel) player.level();

        // 🔥 bắt đầu hiệu ứng tiến hoá giả
        EvolutionEffectHandler.start(level, pokemonEntity);

        // consume item
        if (ShinyShiftConfig.CONSUME_ITEM && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
