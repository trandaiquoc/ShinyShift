package com.rakoon.shinyshift.effect;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.rakoon.shinyshift.ShinyShiftConfig;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class EvolutionEffectHandler {

    private static final Map<UUID, EvolutionTask> TASKS = new HashMap<>();

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            Iterator<EvolutionTask> it = TASKS.values().iterator();
            while (it.hasNext()) {
                EvolutionTask task = it.next();
                if (task.tick()) {
                    it.remove();
                }
            }
        });
    }

    public static void start(ServerLevel level, PokemonEntity entity) {
        TASKS.put(entity.getUUID(), new EvolutionTask(level, entity));
    }

    // ================= TASK =================

    private static class EvolutionTask {

        private final ServerLevel level;
        private final PokemonEntity entity;
        private final Pokemon pokemon;

        private int tick = 0;
        private boolean soundPlayed = false;
        private boolean finished = false;

        EvolutionTask(ServerLevel level, PokemonEntity entity) {
            this.level = level;
            this.entity = entity;
            this.pokemon = entity.getPokemon();

            entity.setNoGravity(true);
            entity.setDeltaMovement(0, 0, 0);
            entity.setGlowingTag(true);
        }

        boolean tick() {
            if (!entity.isAlive()) return true;

            tick++;

            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() * 0.5;
            double z = entity.getZ();

            // 🌀 hiệu ứng trong thời gian delay
            if (tick < ShinyShiftConfig.DELAY_TICKS) {
                entity.setDeltaMovement(0, 0.04, 0);
                entity.setYRot(entity.getYRot() + 10f);
                entity.yBodyRot = entity.getYRot();

                level.sendParticles(
                        ParticleTypes.CLOUD,
                        x, y, z,
                        25,
                        0.6, 1.0, 0.6,
                        0.01
                );

                level.sendParticles(
                        ParticleTypes.ENCHANT,
                        x, y, z,
                        30,
                        0.4, 0.8, 0.4,
                        0.1
                );

                return false;
            }

            // ✨ chỉ chạy 1 lần
            if (!finished) {
                finished = true;

                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        60,
                        0.6, 0.6, 0.6,
                        0.05
                );

                if (ShinyShiftConfig.PLAY_SOUND && !soundPlayed) {
                    soundPlayed = true;

                    SoundEvent sound = SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(
                                    ShinyShiftConfig.SOUND_NAMESPACE,
                                    ShinyShiftConfig.SOUND_PATH
                            )
                    );

                    level.playSound(
                            null,
                            entity.blockPosition(),
                            sound,
                            SoundSource.PLAYERS,
                            1.9F,
                            1.0F
                    );
                }

                // ⚠️ shiny SET SAU KHI phát sound
                pokemon.setShiny(true);

                entity.setNoGravity(false);
                entity.setGlowingTag(false);
            }

            return true;
        }
    }
}

