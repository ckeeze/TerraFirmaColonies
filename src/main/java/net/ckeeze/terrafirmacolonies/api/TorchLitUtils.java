package net.ckeeze.terrafirmacolonies.api;

import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.util.WorkerUtil;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.Map.Entry;

public class TorchLitUtils {

    public static final Set<ResourceLocation> litJobs = Set.of(
        ModJobs.MINER_ID,
        ModJobs.DELIVERY_ID
    );

    private static final int minute = 1000 * 60;
    private static final int torchCacheUpdateCooldownMillis = 10 * minute;

    public static void litAround(AbstractEntityCitizen citizen, Level world, BlockPos pos) {
        CacheEntry cacheEntry = cachedTorchPoses.computeIfAbsent(new ChunkPos(pos), __ -> new CacheEntry());
        if (System.currentTimeMillis() - cacheEntry.lastCheckTime > torchCacheUpdateCooldownMillis) {
            cacheEntry.lastCheckTime = System.currentTimeMillis();
            int startX = pos.getX() >> 4 << 4;
            int startY = pos.getY() >> 4 << 4;
            int startZ = pos.getZ() >> 4 << 4;
            MutableBlockPos p = new MutableBlockPos();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        p.set(startX + x, startY + y, startZ + z);
                        if (world.getBlockState(p).getBlock() == TFCBlocks.TORCH.get()) {
                            cacheEntry.knownTorches.add(p.immutable());
                        }
                    }
                }
            }
        }
        Iterator<BlockPos> it = cacheEntry.knownTorches.iterator();
        while (it.hasNext()) {
            BlockPos torchPos = it.next();
            Block actualBlock = world.getBlockState(torchPos).getBlock();

            if (actualBlock == TFCBlocks.DEAD_TORCH.get()) {
                WorkerUtil.faceBlock(torchPos, citizen);
                WorldUtil.setBlockState(world, torchPos, TFCBlocks.TORCH.get().defaultBlockState());
                world.getBlockEntity(torchPos, TFCBlockEntities.TICK_COUNTER.get()).ifPresent(TickCounterBlockEntity::resetCounter);

            } else if (actualBlock != TFCBlocks.TORCH.get()) {
                it.remove();
            }
        }
    }

    private static class CacheEntry {
        Set<BlockPos> knownTorches = new HashSet<>();
        long lastCheckTime = 0;
    }

    private static final int avgOnline = 20;
    private static final int lruSize = 24 * 24 * avgOnline;

    private static final Map<ChunkPos, CacheEntry> cachedTorchPoses = new LinkedHashMap<>(lruSize, 1, true) {
        @Override
        protected boolean removeEldestEntry(Entry<ChunkPos, CacheEntry> eldest) {
            return size() > lruSize;
        }
    };
}
