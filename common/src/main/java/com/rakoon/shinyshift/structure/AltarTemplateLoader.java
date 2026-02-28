package com.rakoon.shinyshift.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import com.rakoon.shinyshift.ShinyShift;

import java.io.InputStream;
import java.util.*;

public class AltarTemplateLoader {

    private static final ResourceLocation ALTAR_ID =
            new ResourceLocation("shinyshift", "structure/shiny_altar.nbt");

    private static final Block SHINY_STONE =
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("cobblemon:shiny_stone_block"));

    private static final Block THUNDER_STONE =
            BuiltInRegistries.BLOCK.get(ResourceLocation.parse("cobblemon:thunder_stone_block"));

    private static boolean loaded = false;

    private static final List<TemplateBlock> blocks = new ArrayList<>();
    private static final List<TemplateBlock> anchorBlocks = new ArrayList<>();

    private record TemplateBlock(int x, int y, int z, Block block) {}

    private static void load(ServerLevel level) {

        if (loaded) return;

        try {
            Optional<Resource> resource =
                    level.getServer().getResourceManager().getResource(ALTAR_ID);

            if (resource.isEmpty()) return;

            try (InputStream stream = resource.get().open()) {

                CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());

                ListTag palette = tag.getList("palette", 10);
                List<Block> paletteBlocks = new ArrayList<>(palette.size());

                for (int i = 0; i < palette.size(); i++) {
                    CompoundTag entry = palette.getCompound(i);
                    String name = entry.getString("Name");
                    Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
                    paletteBlocks.add(block);
                }

                ListTag blockList = tag.getList("blocks", 10);

                for (int i = 0; i < blockList.size(); i++) {

                    CompoundTag blockTag = blockList.getCompound(i);
                    ListTag posList = blockTag.getList("pos", 3);

                    int x = posList.getInt(0);
                    int y = posList.getInt(1);
                    int z = posList.getInt(2);

                    int stateIndex = blockTag.getInt("state");
                    Block block = paletteBlocks.get(stateIndex);

                    if (!block.defaultBlockState().isAir()) {

                        TemplateBlock tb = new TemplateBlock(x, y, z, block);
                        blocks.add(tb);

                        if (block == SHINY_STONE || block == THUNDER_STONE) {
                            anchorBlocks.add(tb);
                        }
                    }
                }
            }

            ShinyShift.LOGGER.info("Loaded altar template with {} blocks", blocks.size());
            loaded = true;

        } catch (Exception e) {
            ShinyShift.LOGGER.error("Failed to load shiny_altar.nbt", e);
        }
    }

    public static boolean matches(ServerLevel level, BlockPos stonePos) {

        load(level);
        if (!loaded) return false;

        BlockState stoneState = level.getBlockState(stonePos);

        if (!stoneState.is(SHINY_STONE) && !stoneState.is(THUNDER_STONE)) {
            return false;
        }

        if (anchorBlocks.isEmpty()) return false;

        int stoneX = stonePos.getX();
        int stoneY = stonePos.getY();
        int stoneZ = stonePos.getZ();

        for (TemplateBlock anchor : anchorBlocks) {

            for (int rot = 0; rot < 4; rot++) {

                // rotate anchor bằng toán học (không tạo object)
                int ax = anchor.x;
                int ay = anchor.y;
                int az = anchor.z;

                int rax, raz;

                switch (rot) {
                    case 0 -> { rax = ax;      raz = az; }
                    case 1 -> { rax = -az;     raz = ax; }
                    case 2 -> { rax = -ax;     raz = -az; }
                    case 3 -> { rax = az;      raz = -ax; }
                    default -> throw new IllegalStateException();
                }

                int originX = stoneX - rax;
                int originY = stoneY - ay;
                int originZ = stoneZ - raz;

                boolean valid = true;

                for (TemplateBlock tb : blocks) {

                    int x = tb.x;
                    int y = tb.y;
                    int z = tb.z;

                    int rx, rz;

                    switch (rot) {
                        case 0 -> { rx = x;      rz = z; }
                        case 1 -> { rx = -z;     rz = x; }
                        case 2 -> { rx = -x;     rz = -z; }
                        case 3 -> { rx = z;      rz = -x; }
                        default -> throw new IllegalStateException();
                    }

                    int worldX = originX + rx;
                    int worldY = originY + y;
                    int worldZ = originZ + rz;

                    if (!level.getBlockState(new BlockPos(worldX, worldY, worldZ)).is(tb.block())) {
                        valid = false;
                        break;
                    }
                }

                if (valid) return true;
            }
        }

        return false;
    }
}