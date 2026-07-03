package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.jobs.JobMiner;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.minecraft.world.level.block.state.properties.SlabType.DOUBLE;

@Mixin(value = EntityAIStructureMiner.class, remap = false)
public abstract class EntityAIStructureMinerMixin extends AbstractEntityAIStructureWithWorkOrder<JobMiner, BuildingMiner> {

    public EntityAIStructureMinerMixin(@NotNull JobMiner job) {
        super(job);
    }

    @Shadow
    protected abstract void setBlockFromInventory(@NotNull BlockPos location, @NotNull Block block);

    @Shadow
    protected abstract void setBlockFromInventory(@NotNull final BlockPos location, final Block block, final BlockState metadata);

    @Inject(
        method = "setBlockFromInventory(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/block/state/BlockState;)V",
        at = @At(value = "INVOKE", target = "Lcom/minecolonies/api/inventory/InventoryCitizen;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    public void consume_2_SlabsIfDouble(BlockPos location, Block block, BlockState metadata, CallbackInfo ci,
                                        int slot) {
        boolean needToConsumeOneMore = block instanceof SlabBlock && metadata.getValue(SlabBlock.TYPE) == DOUBLE;
        if (needToConsumeOneMore)
            getInventory().extractItem(slot, 1, false);
    }

    @Redirect(
        method = "getNextBlockInShaftToMine",
        at = @At(value = "INVOKE", target = "Lcom/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner;setBlockFromInventory(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V")
    )
    public void placeDoubleSlab1(EntityAIStructureMiner instance, BlockPos location, Block block) {
        setBlockFromInventory_ConsiderDoubleSlab(location, block);
    }

    @Redirect(
        method = "secureBlock",
        at = @At(value = "INVOKE", target = "Lcom/minecolonies/core/entity/ai/workers/production/EntityAIStructureMiner;setBlockFromInventory(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V")
    )
    public void placeDoubleSlab2(EntityAIStructureMiner instance, BlockPos location, Block block) {
        setBlockFromInventory_ConsiderDoubleSlab(location, block);
    }

    @Unique
    private void setBlockFromInventory_ConsiderDoubleSlab(BlockPos location, Block block) {
        if (block instanceof SlabBlock) {
            setBlockFromInventory(location, block, block.defaultBlockState().setValue(SlabBlock.TYPE, DOUBLE));
        } else {
            setBlockFromInventory(location, block);
        }
    }
}
