package com.rakoon.shinyshift.effect;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.rakoon.shinyshift.ShinyShiftConfig;
import com.rakoon.shinyshift.items.ShinyExchangeGemItem;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EvolutionEffectHandler {

    // dùng ConcurrentHashMap để hỗ trợ putIfAbsent atomically (tránh race + overwrite)
    private static final Map<UUID, EvolutionTask> TASKS = new ConcurrentHashMap<>();

    private static final int SHINY_TICK = 105;
    private static final int END_TICK   = 128;

    // ===== PRECOMPUTED LAYER VALUES =====
    private static final int LAYERS = 4;
    private static final double[] PHI = new double[LAYERS];
    private static final double[] SIN_PHI = new double[LAYERS];

    static {
        for (int layer = 0; layer < LAYERS; layer++) {
            double v = (layer + 0.5) / (double) LAYERS;
            PHI[layer] = Math.acos(1.0 - 2.0 * v);
            SIN_PHI[layer] = Math.sin(PHI[layer]);
        }
    }

    public static void init() {
        // server tick — remove tasks whose tick() trả true
        TickEvent.SERVER_POST.register(server ->
                TASKS.entrySet().removeIf(entry -> entry.getValue().tick())
        );
    }

    /**
     * Bắt đầu effect.
     * Thay vì truyền Player/ItemStack (gây giữ tham chiếu), ta truyền player's UUID + InteractionHand.
     * EvolutionTask chỉ giữ UUID + hand — khi cần shrink sẽ lookup ServerPlayer tại thời điểm đó.
     */
    public static void start(ServerLevel level, PokemonEntity entity, UUID playerUuid, InteractionHand hand) {
        UUID uuid = entity.getUUID();
        // dùng putIfAbsent để tránh overwrite khi cùng lúc 2 player tương tác
        TASKS.computeIfAbsent(uuid, u -> new EvolutionTask(level, entity, playerUuid, hand));
    }

    private static class EvolutionTask {

        private final ServerLevel level;
        private final PokemonEntity entity;
        private final Pokemon pokemon;
        private final double groundY;

        // player info for deferred consumption (no Player/ItemStack retained)
        private final UUID playerUuid;
        private final InteractionHand hand;

        private int tick = 0;
        private boolean shinyApplied = false;

        private float rotation = 0f;
        private float angularVelocity = 2f;

        private final Random random = new Random();

        EvolutionTask(ServerLevel level, PokemonEntity entity, UUID playerUuid, InteractionHand hand) {
            this.level = level;
            this.entity = entity;
            this.pokemon = entity.getPokemon();
            this.groundY = computeGroundY();

            this.playerUuid = playerUuid;
            this.hand = hand;

            entity.setNoGravity(true);
            entity.setDeltaMovement(0, 0, 0);
            entity.setGlowingTag(true);

            if (ShinyShiftConfig.PLAY_SOUND) {
                SoundEvent sound = SoundEvent.createVariableRangeEvent(
                        ShinyShiftConfig.SHINY_SOUND
                );

                level.playSound(
                        null,
                        entity.blockPosition(),
                        sound,
                        SoundSource.MASTER,
                        6.5F,
                        1.1F
                );
            }
        }

        private double computeGroundY() {
            BlockPos pos = entity.blockPosition();
            for (int i = 0; i < 12; i++) {
                BlockPos check = pos.below(i);
                if (!level.getBlockState(check).isAir()) {
                    return check.getY() + 1.0;
                }
            }
            return entity.getY();
        }

        /**
         * Trả về true nếu task đã hoàn tất và entry cần xoá khỏi map.
         */
        boolean tick() {

            if (!entity.isAlive()
                    || entity.isRemoved()
                    || entity.getCommandSenderWorld() != level) {
                cleanup();
                return true;
            }

            tick++;

            double x = entity.getX();
            double y = entity.getY() + entity.getBbHeight() * 0.5;
            double z = entity.getZ();

            float progress = Math.min(1f, tick / (float) SHINY_TICK);

            if (tick < SHINY_TICK) {

                entity.setDeltaMovement(0, 0.02 + 0.03 * progress, 0);

                float accel = 0.15f + progress * 0.9f;
                angularVelocity = Math.min(angularVelocity + accel, 45f);
                rotation += angularVelocity;

                entity.setYRot(rotation);
                entity.yBodyRot = rotation;
                entity.yHeadRot = rotation;
                entity.yRotO = rotation;

                int layers = LAYERS;
                double baseRadius = 9.0 * progress;
                double basePoints = 6.0 * (1.0 + progress * 0.2);

                for (int layer = 0; layer < layers; layer++) {

                    double sinPhi = SIN_PHI[layer];
                    double cosPhi = Math.cos(PHI[layer]);

                    double layerRadius = baseRadius * (0.4 + 0.6 * ((layer + 0.5) / (double) layers));
                    int points = Math.max(4, (int) Math.round(basePoints * sinPhi * 0.6));

                    double thetaOffsetRad = Math.toRadians(rotation * 0.8 + layer * 21.0);
                    double step = (2 * Math.PI) / points;

                    for (int i = 0; i < points; i++) {

                        double theta = thetaOffsetRad + i * step;

                        double dx = sinPhi * Math.cos(theta);
                        double dy = cosPhi;
                        double dz = sinPhi * Math.sin(theta);

                        dx += (random.nextDouble() - 0.5) * 0.35;
                        dy += (random.nextDouble() - 0.5) * 0.35;
                        dz += (random.nextDouble() - 0.5) * 0.35;

                        double px = x + dx * layerRadius;
                        double py = y + dy * layerRadius;
                        double pz = z + dz * layerRadius;

                        if (random.nextFloat() < 0.3f) {
                            level.sendParticles(
                                    ParticleTypes.CHERRY_LEAVES,
                                    px, py, pz,
                                    1,
                                    0, 0.01, 0,
                                    0
                            );
                        } else {
                            level.sendParticles(
                                    ParticleTypes.END_ROD,
                                    px, py, pz,
                                    0,
                                    0, 0, 0,
                                    0.03
                            );
                        }
                    }
                }

                double ringRadius = 9.0 * (1.0 - progress);

                if (ringRadius > 0.3) {
                    int points = (int) (ringRadius * 22);
                    double step = (2 * Math.PI) / points;

                    for (int i = 0; i < points; i++) {

                        double angle = i * step;

                        double px = x + Math.cos(angle) * ringRadius;
                        double pz = z + Math.sin(angle) * ringRadius;

                        level.sendParticles(
                                ParticleTypes.END_ROD,
                                px,
                                groundY + 0.02,
                                pz,
                                1,
                                0.05, 0.05, 0.05,
                                0.1
                        );
                    }
                }

                return false;
            }

            if (tick == SHINY_TICK && !shinyApplied) {

                shinyApplied = true;
                pokemon.setShiny(true);

                angularVelocity = 0f;

                level.sendParticles(
                        ParticleTypes.FLASH,
                        x, y, z,
                        1,
                        0, 0, 0,
                        0
                );

                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
                if (lightning != null) {
                    lightning.moveTo(x, y, z);
                    lightning.setVisualOnly(true);
                    level.addFreshEntity(lightning);
                }

                entity.setNoGravity(false);
                entity.setDeltaMovement(0, -2.2, 0);

                // ---------- Deferred consumption: lookup player at moment of success ----------
                if (playerUuid != null) {
                    ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
                    if (player != null) {

                        // ---- CONSUME ----
                        if (ShinyShiftConfig.CONSUME_ITEM && !player.getAbilities().instabuild) {
                            ItemStack held = player.getItemInHand(hand);
                            if (!held.isEmpty() && held.getItem() instanceof ShinyExchangeGemItem) {
                                held.shrink(1);
                            }
                        }

                        // =========================
                        //        ADVANCEMENTS
                        // =========================

                        var advancementManager = level.getServer().getAdvancements();

                        // ---- ROOT ----
                        ResourceLocation rootId = new ResourceLocation("shinyshift", "root");
                        var rootHolder = advancementManager.get(rootId);

                        if (rootHolder != null) {
                            var rootprogress = player.getAdvancements().getOrStartProgress(rootHolder);
                            if (!rootprogress.isDone()) {
                                player.getAdvancements().award(rootHolder, "root");
                            }
                        }

                        // ---- FIRST SHINY GEM ----
                        ResourceLocation firstId = new ResourceLocation("shinyshift", "first_shiny_gem");
                        var firstHolder = advancementManager.get(firstId);

                        if (firstHolder != null) {
                            var firstprogress = player.getAdvancements().getOrStartProgress(firstHolder);
                            if (!firstprogress.isDone()) {
                                player.getAdvancements().award(firstHolder, "triggered");
                            }
                        }
                    }
                }
            }

            if (tick < END_TICK) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        x, y, z,
                        4,
                        0.3, 0.4, 0.3,
                        0.01
                );
                return false;
            }

            cleanup();
            return true;
        }

        private void cleanup() {
            entity.setNoGravity(false);
            entity.setGlowingTag(false);
            // remove chỉ khi cùng value để tránh xóa task mới (safety)
            TASKS.remove(entity.getUUID(), this);
        }
    }
}