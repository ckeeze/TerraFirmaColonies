package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.items.FLItems;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFlorist;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobFlorist;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFlorist;
import com.minecolonies.core.util.WorkerUtil;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntityAIWorkFlorist.class)
public abstract class EntityAIWorkFloristMixin extends AbstractEntityAIInteract<JobFlorist, BuildingFlorist> {
    /**
     * @author Ckeeze
     * @reason knife
     */
    @Overwrite(remap = false)
    private boolean checkOrEquipShears() {
        if (this.checkForToolOrWeapon(TFCEquipmentTypes.tfcknife.get())) {
            return false;
        } else {
            int shearSlot = InventoryUtils.getFirstSlotOfItemHandlerContainingEquipment(this.worker.getInventoryCitizen(), TFCEquipmentTypes.tfcknife.get(), 0, this.building.getMaxEquipmentLevel());
            if (shearSlot >= 0) {
                CitizenItemUtils.setHeldItem(this.worker, InteractionHand.MAIN_HAND, shearSlot);
                return true;
            } else {
                return false;
            }
        }
    }

    @Shadow
    private static final VisibleCitizenStatus GARDENING = null;
    @Shadow
    private BlockPos harvestPosition;

    @Shadow
    private BlockPos compostPosition;

    public EntityAIWorkFloristMixin(@NotNull JobFlorist job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason Rewriting decide state for TFC tasks
     */
    @Overwrite(remap = false)
    private IAIState decide() {
        this.worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        if (this.building.getPlantGround().isEmpty()) {
            this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("com.minecolonies.coremod.florist.noplantground"), ChatPriority.BLOCKING));
            return AIWorkerState.IDLE;
        } else if (!this.checkOrEquipShears()) {
            return AIWorkerState.IDLE;
        } else {
            this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            long distance = BlockPosUtil.getDistance2D(this.worker.blockPosition(), this.building.getPosition());
            if (distance > 50L && !this.walkToBuilding()) {
                return AIWorkerState.DECIDE;
            } else {
                //  ItemStackUtils.Is_Compost changed to seed_ball non predicate variable
                int amountOfSeedBallInInv = InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), FLItems.SEED_BALL.get());
                if (amountOfSeedBallInInv <= 0) {
                    int amountOfSeedBallInBuilding = InventoryUtils.hasBuildingEnoughElseCount(this.building, new ItemStorage(FLItems.SEED_BALL.get()), 1);
                    if (amountOfSeedBallInBuilding > 0) {
                        this.checkAndTransferFromHut(new ItemStack(FLItems.SEED_BALL.get(), 8));
                    }

                    this.checkIfRequestForItemExistOrCreateAsync(new ItemStack(FLItems.SEED_BALL.get(), 16));
                }

                this.harvestPosition = this.areThereFlowersToGather();
                if (this.harvestPosition != null) {
                    return AIWorkerState.FLORIST_HARVEST;
                } else if (amountOfSeedBallInInv <= 0) {
                    if (!WorkerUtil.isThereCompostedLand(this.building, this.world)) {
                        this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("com.minecolonies.coremod.florist.nocompost"), ChatPriority.BLOCKING));
                        return AIWorkerState.START_WORKING;
                    } else {
                        return AIWorkerState.DECIDE;
                    }
                } else {
                    this.compostPosition = this.getFirstNotCompostedLand();
                    return AIWorkerState.FLORIST_COMPOST;
                }
            }
        }
    }


    /**
     * @author Ckeeze
     * @reason rewriting compost state for TFC tasks
     */
    @Overwrite(remap = false)
    private IAIState compost() {
        if (this.compostPosition == null) {
            return AIWorkerState.START_WORKING;
        } else {
            this.worker.getCitizenData().setVisibleStatus(GARDENING);
            if (!this.walkToWorkPos(this.compostPosition)) {
                return this.getState();
            } else {
                //using seedball
                // lvl 0 skill, lvl 1 building => 20% consumption chance (5 herb / seedball)
                // lvl 99 skill, lvl 5 building => 2% consumption chance (50 herb / seedball)
                //random  - this.getPrimarySkillLevel() /   10.5 - this.getBuilding.getLevel() < 80
                // 1-100   -  (      0-100              / ( 10.5 -           1-5 ))            < 80
                BlockState blocks = this.world.getBlockState(this.compostPosition);
                if (blocks.is(TFCTags.Blocks.GRASS_PLANTABLE_ON)) {
                    int slot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(FLItems.SEED_BALL.get());
                    if (slot != -1) {
                        if (this.worker.getRandom().nextInt(1, 101) - (this.getPrimarySkillLevel() / 10.5 - this.building.getBuildingLevel()) > 80) {
                            this.getInventory().extractItem(slot, 1, false);
                        }
                        world.setBlockAndUpdate(this.compostPosition.above(), FLBlocks.BUTTERFLY_GRASS.get().defaultBlockState());
                    }
                }

                this.incrementActionsDone();
                this.worker.decreaseSaturationForContinuousAction();
                this.compostPosition = null;
                return AIWorkerState.START_WORKING;
            }
        }
    }


    /**
     * @author Ckeeze
     * @reason Checking for Butterfly grass
     */
    @Overwrite(remap = false)
    private BlockPos getFirstNotCompostedLand() {
        for (BlockPos pos : this.building.getPlantGround()) {
            if (WorldUtil.isBlockLoaded(world, pos)) {
                BlockState butterflyblock = world.getBlockState(pos.above());
                if (!butterflyblock.is(FLBlocks.BUTTERFLY_GRASS.get())) {
                    return pos;
                }
            } else {
                this.building.removePlantableGround(pos);
            }
        }
        return null;
    }


    /**
     * @author Ckeeze
     * @reason Leaving out butterfly grass
     */
    @Overwrite(remap = false)
    private @Nullable BlockPos areThereFlowersToGather() {
        for (BlockPos pos : this.building.getPlantGround()) {
            if (!this.world.isEmptyBlock(pos.above()) && !this.world.getBlockState(pos.above()).is(FLBlocks.BUTTERFLY_GRASS.get())) {
                return pos.above();
            }
        }
        return null;
    }

    /**
     * @author Ckeeze
     * @reason changing tag from itemTags.flowers() to tfc.compost_greens
     */
    @Overwrite(remap = false)
    protected List<String> getFlowerDropAtPos(Level world, BlockPos pos) {
        List<String> flowerDrops = new ArrayList<>();
        BlockState state = world.getBlockState(pos);

        for (ItemStack drop : Block.getDrops(state, (ServerLevel) world, pos, null, this.worker, this.worker.getMainHandItem())) {
            if (drop.is(TFCTags.Items.COMPOST_GREENS)) {
                flowerDrops.add(drop.getItem().getDescriptionId());
            }
        }

        return flowerDrops;
    }

}
