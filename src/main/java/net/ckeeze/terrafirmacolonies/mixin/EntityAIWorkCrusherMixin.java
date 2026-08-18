package net.ckeeze.terrafirmacolonies.mixin;

import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCrusher;
import com.minecolonies.core.colony.jobs.JobCrusher;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkCrusher;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.client.LocalizedParticleEffectMessage;
import com.minecolonies.core.util.WorkerUtil;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.blockentities.QuernBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

@Mixin(value = EntityAIWorkCrusher.class)
public abstract class EntityAIWorkCrusherMixin extends AbstractEntityAICrafting<JobCrusher, BuildingCrusher> {

    @Shadow
    @Final
    private static VisibleCitizenStatus CRUSHING;
    @Unique
    private BlockPos terrafirmacolonies$quernPos;

    public EntityAIWorkCrusherMixin(@NotNull JobCrusher job) {
        super(job);
    }

    //Improvement for later:
    //requesting and placing handstone into empty quern

    /**
     * @author Ckeeze
     * @reason making crusher use the quern
     */
    @Overwrite(remap = false)
    protected IAIState crush() {
        //getting quenrpos
        BlockPos quernPos = terrafirmacolonies$getQuernPos();
        if (quernPos == null) {
            return AIWorkerState.START_WORKING;
        }

        if (this.world.getBlockEntity(quernPos) instanceof QuernBlockEntity quern && quern.hasHandstone()) {
            if (!this.walkToWorkPos(quernPos)) {
                return this.getState();
            } else {
                this.worker.getCitizenData().setVisibleStatus(CRUSHING);
                this.job.setProgress(this.job.getProgress() + 40);
                BuildingCrusher crusherBuilding = this.building;
                WorkerUtil.faceBlock(quernPos, this.worker);
                IRecipeStorage recipeMode = crusherBuilding.getSetting(BuildingCrusher.MODE).getValue(crusherBuilding);
                int dailyLimit = crusherBuilding.getSetting(BuildingCrusher.DAILY_LIMIT).getValue();
                if (this.currentRecipeStorage == null) {
                    this.currentRecipeStorage = recipeMode;
                }

                if ((this.getState() == AIWorkerState.CRAFT || crusherBuilding.getCurrentDailyQuantity() < dailyLimit) && this.currentRecipeStorage != null) {
                    IAIState check = this.checkForItems(this.currentRecipeStorage);
                    if (this.job.getProgress() > 50 - Math.min(this.getSecondarySkillLevel() / 2 + 1, 50)) {
                        this.job.setProgress(0);
                        if (check != AIWorkerState.CRAFT) {
                            if (this.getState() != AIWorkerState.CRAFT) {
                                this.currentRecipeStorage = recipeMode;
                                int requestQty = Math.min((dailyLimit - crusherBuilding.getCurrentDailyQuantity()) * 2, 64);
                                if (requestQty <= 0) {
                                    return AIWorkerState.START_WORKING;
                                }
                                ItemStack stack = this.currentRecipeStorage.getInput().get(0).getItemStack().copy();
                                stack.setCount(requestQty);
                                this.checkIfRequestForItemExistOrCreateAsync(stack);
                                return AIWorkerState.START_WORKING;
                            }
                            return check;
                        }
                        if (this.getState() != AIWorkerState.CRAFT) {
                            crusherBuilding.setCurrentDailyQuantity(crusherBuilding.getCurrentDailyQuantity() + 1);
                            if (crusherBuilding.getCurrentDailyQuantity() >= dailyLimit) {
                                this.incrementActionsDoneAndDecSaturation();
                            }
                        }
                        if (this.currentRequest != null) {
                            this.currentRequest.addDelivery(this.currentRecipeStorage.getPrimaryOutput());
                        }
                        this.worker.swing(InteractionHand.MAIN_HAND);

                        //damaging handstone
                        LazyOptional<IItemHandler> capabilityOpt = quern.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> {
                            ItemStack stack = handler.getStackInSlot(0);
                            stack.setDamageValue(stack.getDamageValue() + 1);
                        });

                        this.job.setCraftCounter(this.job.getCraftCounter() + 1);
                        this.currentRecipeStorage.fullfillRecipe(this.getLootContext(), ImmutableList.of(this.worker.getItemHandlerCitizen()));
                        this.worker.decreaseSaturationForContinuousAction();
                        this.worker.getCitizenExperienceHandler().addExperience(0.1);
                        this.recordCraftingBuildingStats(this.currentRequest, this.currentRecipeStorage);
                    }

                    if (check == AIWorkerState.CRAFT) {
                        Network.getNetwork().sendToTrackingEntity(new LocalizedParticleEffectMessage(this.currentRecipeStorage.getInput().get(0).getItemStack().copy(), quernPos), this.worker);
                        Network.getNetwork().sendToTrackingEntity(new LocalizedParticleEffectMessage(this.currentRecipeStorage.getPrimaryOutput().copy(), quernPos), this.worker);
                        this.worker.queueSound(TFCSounds.QUERN_DRAG.get(), quernPos, 10, 0, 0.9F, this.worker.getRandom().nextFloat());
                        this.job.playSound(this.building.getID(), (EntityCitizen) this.worker);
                    }

                    return this.getState();
                } else {
                    return AIWorkerState.START_WORKING;
                }
            }

        }
        return AIWorkerState.START_WORKING;
    }

    /**
     * @author Ckeeze
     * @reason making crusher use the quern
     */
    @Override
    @Overwrite(remap = false)
    protected IAIState craft() {
        //getting quern Position
        BlockPos quernPos = terrafirmacolonies$getQuernPos();
        if (quernPos == null) {
            return AIWorkerState.START_WORKING;
        }

        if (this.world.getBlockEntity(quernPos) instanceof QuernBlockEntity quern && quern.hasHandstone()) {
            if (this.currentRecipeStorage == null) {
                return AIWorkerState.START_WORKING;
            } else if (this.currentRequest == null && this.job.getCurrentTask() != null) {
                return AIWorkerState.GET_RECIPE;
            } else if (!this.walkToWorkPos(quernPos)) {
                return this.getState();
            } else {
                this.job.setProgress(this.job.getProgress() + 1);
                this.worker.setItemInHand(InteractionHand.MAIN_HAND, this.currentRecipeStorage.getCleanedInput().get(this.worker.getRandom().nextInt(this.currentRecipeStorage.getCleanedInput().size())).getItemStack().copy());
                this.worker.setItemInHand(InteractionHand.OFF_HAND, this.currentRecipeStorage.getPrimaryOutput().copy());
                CitizenItemUtils.hitBlockWithToolInHand(this.worker, quernPos);

                //damaging handstone
                LazyOptional<IItemHandler> capabilityOpt = quern.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> {
                    ItemStack stack = handler.getStackInSlot(0);
                    stack.setDamageValue(stack.getDamageValue() + 1);
                });

                this.currentRequest = this.job.getCurrentTask();
                if (this.currentRequest == null || this.currentRequest.getState() != RequestState.CANCELLED && this.currentRequest.getState() != RequestState.FAILED) {
                    IAIState check = this.crush();
                    if (check == this.getState()) {
                        if (this.job.getCraftCounter() >= this.job.getMaxCraftingCount()) {
                            this.incrementActionsDone(this.getActionRewardForCraftingSuccess());
                            this.currentRecipeStorage = null;
                            this.worker.decreaseSaturationForAction();
                            this.resetValues();
                            if (this.inventoryNeedsDump() && this.job.getMaxCraftingCount() == 0 && this.job.getProgress() == 0 && this.job.getCraftCounter() == 0 && this.currentRequest != null) {
                                this.job.finishRequest(true);
                            }
                        }
                    } else {
                        this.currentRequest = null;
                        this.job.finishRequest(false);
                        this.resetValues();
                    }

                    return this.getState();
                } else {
                    this.currentRequest = null;
                    this.incrementActionsDone(this.getActionRewardForCraftingSuccess());
                    this.currentRecipeStorage = null;
                    return AIWorkerState.START_WORKING;
                }
            }
        }
        return AIWorkerState.START_WORKING;


    }

    //getting the last, or the closest quern
    @Unique
    private BlockPos terrafirmacolonies$getQuernPos() {
        if (terrafirmacolonies$quernPos != null && this.world.getBlockState(terrafirmacolonies$quernPos).is(TFCBlocks.QUERN.get())) {
            return terrafirmacolonies$quernPos;
        } else {
            BlockPos startPos = this.building.getPosition();
            for (int x = startPos.getX() - 10; x < startPos.getX() + 10; ++x) {
                for (int z = startPos.getZ() - 10; z < startPos.getZ() + 10; ++z) {
                    for (int y = startPos.getY() - 3; y < startPos.getY() + 3; ++y) {
                        if (world.getBlockState(new BlockPos(x, y, z)).is(TFCBlocks.QUERN.get())) {
                            terrafirmacolonies$quernPos = new BlockPos(x, y, z);
                            return terrafirmacolonies$quernPos;
                        }
                    }
                }
            }
            return null;
        }
    }
}
