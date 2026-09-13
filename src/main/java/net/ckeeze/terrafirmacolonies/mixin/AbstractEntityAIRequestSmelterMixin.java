package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.FLTags;
import com.eerussianguy.firmalife.common.blockentities.OvenBottomBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.OvenTopBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.VatBlockEntity;
import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.*;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAIRequestSmelter;
import net.ckeeze.terrafirmacolonies.api.mixininterfaces.*;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.BellowsBlockEntity;
import net.dries007.tfc.common.blockentities.CharcoalForgeBlockEntity;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.devices.CharcoalForgeBlock;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mixin(value = AbstractEntityAIRequestSmelter.class)
public abstract class AbstractEntityAIRequestSmelterMixin<J extends AbstractJobCrafter<?, J>, B extends AbstractBuilding> extends AbstractEntityAICrafting<J, B> {
    @Shadow
    protected abstract List<ItemStack> getActivePossibleFuels();

    @Shadow
    private static Predicate<ItemStack> isCorrectFuel(List<ItemStack> possibleFuels) {
        return null;
    }

    @Shadow
    private BlockPos furnacePos;

    public AbstractEntityAIRequestSmelterMixin(@NotNull J job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason Remove unused functionality - DONE
     */
    @Overwrite(remap = false)
    private boolean accelerateFurnaces() {
        if (this.building instanceof BuildingStoneSmeltery b && this.currentRecipeStorage != null && this.currentRecipeStorage.getIntermediate() == Blocks.FURNACE) {
            BlockEntity entity = world.getBlockEntity(((StoneSmelterNewVariables) b).getBellowPos());
            if (entity instanceof BellowsBlockEntity bellows) {
                bellows.onRightClick();
            }
        }
        return false;
    }

    /**
     * @author Ckeeze
     * @reason Counting input slots of all available devices - DONE can be improved
     */
    @Override
    @Overwrite(remap = false)
    protected int getExtendedCount(ItemStack stack) {
        int count = 0;

        if (this.building instanceof BuildingBaker b) {
            count = ((BakerNewVariables) b).getOvenList().size() * 4;
        }
        if (this.building instanceof BuildingKitchen b) {
            count = ((ChefNewVaraibles) b).getOvenList().size() * 4;
        }
        if (this.building instanceof BuildingDyer) {
            count = 1;
        }
        if (this.building instanceof BuildingGlassblower) {
            count = 1;
        }
        if (this.building instanceof BuildingStoneSmeltery) {
            count = 5;
        }
        return count;
    }

    /**
     * @author Ckeeze
     * @reason Fueling TFC devices - DONE
     */
    @Overwrite(remap = false)
    private IAIState addFuelToFurnace() {
        List<ItemStack> possibleFuels = this.getActivePossibleFuels();
        if (!InventoryUtils.hasItemInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
            if (InventoryUtils.hasBuildingEnoughElseCount(this.building, isCorrectFuel(possibleFuels), 1) >= 1) {
                this.needsCurrently = new Tuple<>(isCorrectFuel(possibleFuels), 64);
                return AIWorkerState.GATHERING_REQUIRED_MATERIALS;
            } else {
                this.furnacePos = null;
                return AIWorkerState.IDLE;
            }
        } else if (this.furnacePos == null) {
            return AIWorkerState.IDLE;
        } else if (!this.walkToWorkPos(this.furnacePos)) {
            return this.getState();
        } else {
            if (WorldUtil.isBlockLoaded(this.world, this.furnacePos)) {

                //Checking for ovens (Chef, Baker)
                BlockEntity entity = this.world.getBlockEntity(this.furnacePos.below());
                if (entity instanceof OvenBottomBlockEntity ovenB) {

                    if (InventoryUtils.hasItemInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
                        LazyOptional<IItemHandler> capabilityOpt = ovenB.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> {
                            if (handler.getStackInSlot(0).isEmpty()) {
                                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels), 1, handler, 0);
                            }
                        });
                    }
                }

                //Checking for pot (Chef)
                entity = this.world.getBlockEntity(this.furnacePos);
                if (entity instanceof PotBlockEntity pot) {
                    if (InventoryUtils.hasItemInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
                        LazyOptional<IItemHandler> capabilityOpt = pot.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> {
                            if (handler.getStackInSlot(0).isEmpty()) {
                                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels), 1, handler, 0);
                            }
                        });
                    }
                }

                //Checking for charcoal forge (Stonesmelter, glassblower)
                if (entity instanceof CharcoalForgeBlockEntity cforge) {

                    if (InventoryUtils.hasItemInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
                        LazyOptional<IItemHandler> capabilityOpt = cforge.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> {
                            if (handler.getStackInSlot(0).isEmpty()) {
                                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels), 1, handler, 0);
                            }
                            if (handler.getStackInSlot(1).isEmpty()) {
                                InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels), 1, handler, 1);
                            }
                        });
                    }
                }

            }

            this.furnacePos = null;
        }

        return AIWorkerState.IDLE;
    }

    @Unique
    private boolean terrafirmacolonies$hasProductReady() {
        AtomicBoolean hasProduct = new AtomicBoolean(false);
        if (this.furnacePos != null && this.currentRecipeStorage != null && this.currentRequest != null) {
            BlockEntity entity = this.world.getBlockEntity(this.furnacePos);
            if (entity instanceof OvenTopBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> hasProduct.set(
                        (!handler.getStackInSlot(0).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(0).getItem())) ||
                                (!handler.getStackInSlot(1).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(1).getItem())) ||
                                (!handler.getStackInSlot(2).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(2).getItem())) ||
                                (!handler.getStackInSlot(3).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(3).getItem()))));
            }
            if (entity instanceof PotBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> hasProduct.set(
                        (!handler.getStackInSlot(4).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(4).getItem())) ||
                                (!handler.getStackInSlot(5).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(5).getItem())) ||
                                (!handler.getStackInSlot(6).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(6).getItem())) ||
                                (!handler.getStackInSlot(7).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(7).getItem())) ||
                                (!handler.getStackInSlot(8).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(8).getItem()))));
            }
            if (entity instanceof CharcoalForgeBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> hasProduct.set(
                        (!handler.getStackInSlot(5).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(5).getItem())) ||
                                (!handler.getStackInSlot(6).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(6).getItem())) ||
                                (!handler.getStackInSlot(7).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(7).getItem())) ||
                                (!handler.getStackInSlot(8).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(8).getItem())) ||
                                (!handler.getStackInSlot(9).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(9).getItem()))));
            }
        }
        return hasProduct.get();
    }

    /**
     * @author Ckeeze
     * @reason Retrieving product from tfc device - DONE
     */
    @Overwrite(remap = false)
    private IAIState retrieveProductFromFurnace() {
        if (this.furnacePos != null && this.currentRecipeStorage != null && this.currentRequest != null) {
            BlockEntity entity = this.world.getBlockEntity(this.furnacePos);

            if (terrafirmacolonies$hasProductReady()) {
                if (!this.walkToWorkPos(this.furnacePos)) {
                    return this.getState();
                }
                //oven
                if (entity instanceof OvenTopBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> {

                        int count = 0;

                        if (!handler.getStackInSlot(0).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(0).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(0).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(0, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(1).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(1).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(1).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(1, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(2).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(2).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(2).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(2, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(3).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(3).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(3).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(3, 1, false));
                            count++;
                        }
                        ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                        requestStack.setCount(count);
                        this.currentRequest.addDelivery(requestStack);
                    });
                }
                //Pot
                if (entity instanceof PotBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> {

                        int count = 0;

                        if (!handler.getStackInSlot(4).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(4).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(4).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(4, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(5).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(5).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(5).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(5, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(6).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(6).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(6).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(6, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(7).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(7).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(7).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(7, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(8).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(8).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(8).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(8, 1, false));
                            count++;
                        }

                        ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                        requestStack.setCount(count);
                        this.currentRequest.addDelivery(requestStack);

                    });
                }

                if (entity instanceof CharcoalForgeBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> {

                        int count = 0;

                        if (!handler.getStackInSlot(5).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(5).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(5).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(5, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(6).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(6).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(6).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(6, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(7).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(7).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(7).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(7, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(8).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(8).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(8).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(8, 1, false));
                            count++;
                        }
                        if (!handler.getStackInSlot(9).isEmpty() && this.currentRecipeStorage.getPrimaryOutput().is(handler.getStackInSlot(9).getItem())) {
                            this.recordSmeltingBuildingStats(handler.getStackInSlot(9).getHoverName(), 1);
                            InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(9, 1, false));
                            count++;
                        }

                        ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                        requestStack.setCount(count);
                        this.currentRequest.addDelivery(requestStack);
                        this.job.setCraftCounter(this.job.getCraftCounter() + count);

                    });
                }

                return AIWorkerState.START_WORKING;
            }

            //Removed vanilla segment

            this.furnacePos = null;
            return AIWorkerState.START_WORKING;
        } else {
            return AIWorkerState.START_WORKING;
        }
    }

    /**
     * @author Ckeeze
     * @reason Retrieving unrelated product from tfc device -DONE
     */
    @Overwrite(remap = false)
    private IAIState retrieveUnrelatedProductFromFurnace() {
        LOGGER.info("retrieveUnrelatedProductFromFurnace");
        if (this.furnacePos == null) {
            return AIWorkerState.START_WORKING;
        } else {
            BlockEntity entity = this.world.getBlockEntity(this.furnacePos);

            if (!this.walkToWorkPos(this.furnacePos)) {
                return this.getState();
            }
            AtomicInteger count = new AtomicInteger();
            if (entity instanceof OvenTopBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> {

                    if (!handler.getStackInSlot(0).isEmpty() && (this.currentRecipeStorage == null || !this.currentRecipeStorage.getCleanedInput().get(0).getItemStack().is(handler.getStackInSlot(0).getItem()))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(0, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(1).isEmpty() && (this.currentRecipeStorage == null || !this.currentRecipeStorage.getCleanedInput().get(0).getItemStack().is(handler.getStackInSlot(1).getItem()))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(1, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(2).isEmpty() && (this.currentRecipeStorage == null || !this.currentRecipeStorage.getCleanedInput().get(0).getItemStack().is(handler.getStackInSlot(2).getItem()))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(2, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(3).isEmpty() && (this.currentRecipeStorage == null || !this.currentRecipeStorage.getCleanedInput().get(0).getItemStack().is(handler.getStackInSlot(3).getItem()))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(3, 1, false));
                        count.getAndIncrement();
                    }
                    if (this.currentRequest != null) {
                        ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                        requestStack.setCount(count.get());
                        this.currentRequest.addDelivery(requestStack);
                    }
                });
            }

            if (entity instanceof PotBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> {


                    if (!handler.getStackInSlot(4).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(4)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(4, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(5).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(5)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(5, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(6).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(6)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(6, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(7).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(7)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(7, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(8).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(8)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(8, 1, false));
                        count.getAndIncrement();
                    }
                    ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                    requestStack.setCount(count.get());
                    this.currentRequest.addDelivery(requestStack);
                });
            }

            if (entity instanceof CharcoalForgeBlockEntity) {
                LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                capabilityOpt.ifPresent(handler -> {


                    if (!handler.getStackInSlot(5).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(5)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(5, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(6).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(6)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(6, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(7).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(7)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(7, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(8).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(8)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(8, 1, false));
                        count.getAndIncrement();
                    }
                    if (!handler.getStackInSlot(9).isEmpty() && (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(this.currentRecipeStorage.getCleanedInput().get(0).getItemStack(), handler.getStackInSlot(9)))) {
                        InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), handler.extractItem(9, 1, false));
                        count.getAndIncrement();
                    }
                    ItemStack requestStack = this.currentRequest.getRequest().getStack().copy();
                    requestStack.setCount(count.get());
                    this.currentRequest.addDelivery(requestStack);
                });
            }
            //TODO: make him collect ash
            if (count.get() > 0) {
                return AIWorkerState.INVENTORY_FULL;
            }


            this.furnacePos = null;
            return AIWorkerState.START_WORKING;
        }
    }

    /**
     * @author Ckeeze
     * @reason Filling devices with ingredients, also this is where we ignite them - DONE
     */
    @Overwrite(remap = false)
    private IAIState fillUpFurnace() {
        LOGGER.info("Entered fillupfurnace state!");
        if (this.furnacePos != null && this.currentRecipeStorage != null) {
            if (this.job.getMaxCraftingCount() == 0) {
                this.job.setMaxCraftingCount(this.currentRequest.getRequest().getCount());
            }

            ItemStack inputStack = this.currentRecipeStorage.getCleanedInput().get(0).getItemStack();
            Predicate<ItemStack> smeltablePredicate = (stack) -> ItemStackUtils.compareItemStacksIgnoreStackSize(inputStack, stack);
            int smeltableInInventory = InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), (stack) -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, inputStack));
            int smeltableInFurnaces = this.getExtendedCount(inputStack);
            int resultInFurnaces = this.getExtendedCount(this.currentRecipeStorage.getPrimaryOutput());
            int targetCount = this.job.getMaxCraftingCount() - this.job.getCraftCounter() - smeltableInFurnaces - resultInFurnaces - smeltableInInventory;
            if ((this.job).getMaxCraftingCount() - (this.job).getCraftCounter() - smeltableInFurnaces - resultInFurnaces <= 0) {
                return AIWorkerState.START_WORKING;
            } else if (smeltableInInventory == 0) {
                this.needsCurrently = new Tuple<>(smeltablePredicate, targetCount);
                this.furnacePos = null;
                return AIWorkerState.GATHERING_REQUIRED_MATERIALS;
            } else {
                int burningFurnaces = this.countOfBurningFurnaces();
                int maxFurnaces = this.getMaxUsableFurnaces();
                if (burningFurnaces > maxFurnaces) {
                    return AIWorkerState.START_WORKING;
                } else if (!this.walkToWorkPos(this.furnacePos)) {
                    return this.getState();
                } else {
                    int pendingCount = (this.job).getMaxCraftingCount() - (this.job).getCraftCounter() - smeltableInFurnaces - resultInFurnaces;
                    if (pendingCount <= 0) {
                        return AIWorkerState.START_WORKING;
                    } else {
                        BlockEntity entity = this.world.getBlockEntity(this.furnacePos);
                        BlockEntity bottomentity = this.world.getBlockEntity(this.furnacePos.below());
                        this.furnacePos = null;

                        //insert here
                        if (entity instanceof OvenTopBlockEntity) {
                            if (this.worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                                this.worker.setItemInHand(InteractionHand.MAIN_HAND, inputStack.copy());
                            }
                            LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                            capabilityOpt.ifPresent(handler -> {
                                if (handler.getStackInSlot(0).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 0);
                                }
                                if (handler.getStackInSlot(1).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 1);
                                }
                                if (handler.getStackInSlot(2).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 2);
                                }
                                if (handler.getStackInSlot(3).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 3);
                                }
                            });

                            if (bottomentity instanceof OvenBottomBlockEntity) {
                                AtomicBoolean hasFuel = new AtomicBoolean(false);
                                LazyOptional<IItemHandler> capabilityOpt2 = bottomentity.getCapability(Capabilities.ITEM, null);
                                capabilityOpt2.ifPresent(handler -> {
                                    if (!handler.getStackInSlot(0).isEmpty()) {
                                        hasFuel.set(true);
                                    }
                                });
                                if (hasFuel.get() && !bottomentity.getBlockState().getValue(BlockStateProperties.LIT)) {
                                    ((OvenBottomBlockEntity) bottomentity).light(bottomentity.getBlockState());
                                }
                            }
                        }
                        if (entity instanceof PotBlockEntity) {
                            if (this.worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                                this.worker.setItemInHand(InteractionHand.MAIN_HAND, inputStack.copy());
                            }
                            AtomicBoolean hasFuel = new AtomicBoolean(false);
                            LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                            capabilityOpt.ifPresent(handler -> {
                                if (handler.getStackInSlot(4).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 4);
                                }
                                if (handler.getStackInSlot(5).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 5);
                                }
                                if (handler.getStackInSlot(6).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 6);
                                }
                                if (handler.getStackInSlot(7).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 7);
                                }
                                if (handler.getStackInSlot(8).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 8);
                                }

                                if (!handler.getStackInSlot(0).isEmpty()) {
                                    hasFuel.set(true);
                                }
                                if (hasFuel.get() && !entity.getBlockState().getValue(BlockStateProperties.LIT)) {
                                    ((PotBlockEntity) entity).light(entity.getBlockState());
                                }
                            });
                        }

                        if (entity instanceof CharcoalForgeBlockEntity) {
                            if (this.worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                                this.worker.setItemInHand(InteractionHand.MAIN_HAND, inputStack.copy());
                            }
                            AtomicBoolean hasFuel = new AtomicBoolean(false);
                            LazyOptional<IItemHandler> capabilityOpt = entity.getCapability(Capabilities.ITEM, null);
                            capabilityOpt.ifPresent(handler -> {

                                if (handler.getStackInSlot(5).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 5);
                                }
                                if (handler.getStackInSlot(6).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 6);
                                }
                                if (handler.getStackInSlot(7).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 7);
                                }
                                if (handler.getStackInSlot(8).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 8);
                                }
                                if (handler.getStackInSlot(9).isEmpty()) {
                                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(this.worker.getInventoryCitizen(), smeltablePredicate, 1, handler, 9);
                                }

                                if (!handler.getStackInSlot(0).isEmpty()) {
                                    hasFuel.set(true);
                                }
                                if (hasFuel.get() && entity.getBlockState().getValue(CharcoalForgeBlock.HEAT) == 0) {
                                    ((CharcoalForgeBlockEntity) entity).light(entity.getBlockState());
                                }
                            });
                        }
                        return AIWorkerState.CRAFT;
                    }
                }
            }
        } else {
            return AIWorkerState.START_WORKING;
        }
    }

    @Unique
    private boolean terrafirmacolonies$hasNecessaryBlocks() {
        if (this.building instanceof BuildingDyer b) {
            return ((DyerNewVariables) b).getQuernPos() != null && ((DyerNewVariables) b).getPotPos() != null;
        }
        if (this.building instanceof BuildingBaker b) {
            return !((BakerNewVariables) b).getOvenList().isEmpty();
        }
        if (this.building instanceof BuildingKitchen b) {
            if (this.building.getBuildingLevel() > 3) {
                return !((ChefNewVaraibles) b).getOvenList().isEmpty() && ((ChefNewVaraibles) b).getPotPos() != null && ((ChefNewVaraibles) b).getVatPos() != null;
            }
            return !((ChefNewVaraibles) b).getOvenList().isEmpty() && ((ChefNewVaraibles) b).getPotPos() != null;
        }
        if (this.building instanceof BuildingGlassblower b) {
            return ((GlassBlowerNewVaraibles) b).getTablePos() != null && ((GlassBlowerNewVaraibles) b).getCharcoalPos() != null && ((GlassBlowerNewVaraibles) b).getBasinPos() != null;
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            LOGGER.info("drying block list: " + ((StoneSmelterNewVariables) b).getDryingBlockList());
            return !((StoneSmelterNewVariables) b).getDryingBlockList().isEmpty() && ((StoneSmelterNewVariables) b).getBellowPos() != null && ((StoneSmelterNewVariables) b).getCharcoalPos() != null;
        }
        return true;
    }

    /**
     * @author Ckeeze
     * @reason CraftingAction rewrite - DONE
     */
    @Overwrite(remap = false)
    @Override
    public IAIState executeCraftingAction(int toolSlot) {
        LOGGER.info("executeCraftingAction!");

        if (!terrafirmacolonies$hasNecessaryBlocks()) {
            LOGGER.info("can't find workblocks!");
            if (this.worker.getCitizenData() != null) {
                if (this.building instanceof BuildingGlassblower) {
                    this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.glassblowerBuildingComplain"), ChatPriority.BLOCKING));
                }
                if (this.building instanceof BuildingKitchen) {
                    if (this.building.getBuildingLevel() > 3) {
                        this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.chefBuildingComplain4"), ChatPriority.BLOCKING));
                    } else {
                        this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.chefBuildingComplain"), ChatPriority.BLOCKING));
                    }
                }
                if (this.building instanceof BuildingBaker) {
                    this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.bakerBuildingComplain"), ChatPriority.BLOCKING));
                }
                if (this.building instanceof BuildingStoneSmeltery) {
                    this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.stoneSmelterBuildingComplain"), ChatPriority.BLOCKING));
                }
                if (this.building instanceof BuildingDyer) {
                    this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("net.ckeeze.terrafirmacolonies.dyerBuildingComplain"), ChatPriority.BLOCKING));
                }
            }
            this.setDelay(5);
            return AIWorkerState.START_WORKING;
        }

        if (this.currentRecipeStorage == null) {
            LOGGER.info("no recipe found!");
            return AIWorkerState.START_WORKING;
        } else if (!(this.currentRecipeStorage.getIntermediate().defaultBlockState().is(BlockTags.SAND)) && this.currentRecipeStorage.getIntermediate() != Blocks.FURNACE && this.currentRecipeStorage.getIntermediate() != TFCBlocks.CHARCOAL_FORGE.get() && this.currentRecipeStorage.getIntermediate() != TFCBlocks.POT.get() && this.currentRecipeStorage.getIntermediate() != FLBlocks.VAT.get()) {
            LOGGER.info("no intermedieta found!");
            return super.executeCraftingAction(toolSlot);
        } else if (!this.areFurnacesLoaded()) {
            LOGGER.info("furnaces not loaded!");
            return AIWorkerState.START_WORKING;
        } else {
            LOGGER.info("getting fuel list!");
            List<ItemStack> possibleFuels = this.getAllowedFuel();
            if (possibleFuels.isEmpty()) {
                if (this.worker.getCitizenData() != null) {
                    this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("com.minecolonies.coremod.furnaceuser.nofuel"), ChatPriority.BLOCKING));
                }
                LOGGER.info("Possible Fuel list empty!");
                return this.getState();
            } else {
                this.furnacePos = this.getFurnaceToRetrieveOutputFrom();
                if (this.furnacePos != null) {
                    LOGGER.info("Retrieving product!");
                    return AIWorkerState.RETRIEVING_END_PRODUCT_FROM_FURNACE;
                } else {
                    LOGGER.info("No ready furnace found!");
                    this.furnacePos = this.getFurnaceToRetrieveUnrelatedInputFrom();
                    if (this.furnacePos != null) {
                        LOGGER.info("Retrieving unrelated product!");
                        return AIWorkerState.RETRIEVING_UNRELATED_PRODUCT_FROM_FURNACE;
                    } else {
                        if (InventoryUtils.hasBuildingEnoughElseCount(this.building, isCorrectFuel(possibleFuels), 1) > 1 || InventoryUtils.hasItemInItemHandler(this.worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
                            LOGGER.info("Have fuel!");
                            this.furnacePos = this.getFurnaceWithoutFuel();
                            if (this.furnacePos != null) {
                                LOGGER.info("Adding fuel!");
                                return AIWorkerState.ADD_FUEL_TO_FURNACE;
                            }
                        }

                        this.furnacePos = this.getEmptyFurnaceWithFuel();
                        LOGGER.info("Fill up furnaces!");
                        return this.furnacePos != null ? AIWorkerState.FILL_UP_FURNACES : AIWorkerState.START_WORKING;
                    }
                }
            }
        }
    }

    /**
     * @author Ckeeze
     * @reason getting empty but fueled TFC heating devices - DONE
     */
    @Overwrite(remap = false)
    private BlockPos getEmptyFurnaceWithFuel() {
        AtomicBoolean topempty = new AtomicBoolean(true);
        AtomicBoolean botempty = new AtomicBoolean(false);

        //if (this.building instanceof BuildingDyer) {}
        if (this.building instanceof BuildingBaker b) {
            for (BlockPos ovenPos : ((BakerNewVariables) b).getOvenList()) {
                BlockEntity oven = this.world.getBlockEntity(ovenPos);
                BlockEntity bottomOven = this.world.getBlockEntity(ovenPos.below());

                if (oven instanceof OvenTopBlockEntity) {
                    if (bottomOven instanceof OvenBottomBlockEntity) {
                        LazyOptional<IItemHandler> capabilityOpt = oven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> topempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (topempty.get() && !botempty.get()) {
                            return ovenPos;
                        }
                    }
                }
            }
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            if (((StoneSmelterNewVariables) b).getCharcoalPos() != null) {
                BlockPos forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
                BlockEntity forge = this.world.getBlockEntity(forgePos);
                if (forge instanceof CharcoalForgeBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = forge.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> {
                        topempty.set(handler.getStackInSlot(5).isEmpty() && handler.getStackInSlot(6).isEmpty() && handler.getStackInSlot(7).isEmpty() && handler.getStackInSlot(8).isEmpty() && handler.getStackInSlot(9).isEmpty());
                        botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty() && handler.getStackInSlot(4).isEmpty());
                    });
                    if (topempty.get() && !botempty.get()) {
                        return forgePos;
                    }
                }
            }
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            if (((StoneSmelterNewVariables) b).getCharcoalPos() != null) {
                BlockPos forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
                BlockEntity forge = this.world.getBlockEntity(forgePos);
                if (forge instanceof CharcoalForgeBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = forge.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> {
                        topempty.set(handler.getStackInSlot(5).isEmpty() && handler.getStackInSlot(6).isEmpty() && handler.getStackInSlot(7).isEmpty() && handler.getStackInSlot(8).isEmpty() && handler.getStackInSlot(9).isEmpty());
                        botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty() && handler.getStackInSlot(4).isEmpty());
                    });
                    if (topempty.get() && !botempty.get()) {
                        return forgePos;
                    }
                }
            }
        }
        if (this.building instanceof BuildingKitchen b) {
            for (BlockPos ovenPos : ((ChefNewVaraibles) b).getOvenList()) {
                BlockEntity oven = this.world.getBlockEntity(ovenPos);
                BlockEntity bottomOven = this.world.getBlockEntity(ovenPos.below());

                if (oven instanceof OvenTopBlockEntity) {
                    if (bottomOven instanceof OvenBottomBlockEntity) {
                        LazyOptional<IItemHandler> capabilityOpt = oven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> topempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (topempty.get() && !botempty.get()) {
                            return ovenPos;
                        }
                    }
                }
            }
            if (((ChefNewVaraibles) b).getVatPos() != null) {
                BlockPos vatPos = ((ChefNewVaraibles) b).getVatPos();
                if (this.world.getBlockEntity(vatPos) instanceof VatBlockEntity vat) {
                    if (this.world.getBlockEntity(vatPos.below()) instanceof OvenBottomBlockEntity bottomOven) {
                        LazyOptional<IItemHandler> capabilityOpt = vat.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> topempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (topempty.get() && !botempty.get()) {
                            return vatPos;
                        }
                    }
                }
            }
            if (((ChefNewVaraibles) b).getPotPos() != null) {
                BlockPos potPos = ((ChefNewVaraibles) b).getPotPos();
                if (this.world.getBlockEntity(potPos) instanceof VatBlockEntity vat) {
                    if (this.world.getBlockEntity(potPos.below()) instanceof OvenBottomBlockEntity bottomOven) {
                        LazyOptional<IItemHandler> capabilityOpt = vat.getCapability(Capabilities.ITEM, null);
                        capabilityOpt.ifPresent(handler -> topempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (topempty.get() && !botempty.get()) {
                            return potPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @author Ckeeze
     * @reason getting non fueled TFC heating devices - DONE
     */
    @Overwrite(remap = false)
    private BlockPos getFurnaceWithoutFuel() {
        AtomicBoolean botempty = new AtomicBoolean(false);

        //if (this.building instanceof BuildingDyer b) {}
        if (this.building instanceof BuildingBaker b) {
            LOGGER.info("Baker get furnace without fuel!");
            LOGGER.info("Oveposlist: " + ((BakerNewVariables) b).getOvenList());
            for (BlockPos ovenPos : ((BakerNewVariables) b).getOvenList()) {
                BlockEntity oven = this.world.getBlockEntity(ovenPos);
                BlockEntity bottomOven = this.world.getBlockEntity(ovenPos.below());

                if (oven instanceof OvenTopBlockEntity) {
                    LOGGER.info("pos instance of oventopblockentity");
                    if (bottomOven instanceof OvenBottomBlockEntity) {
                        LOGGER.info("pos.below instance of ovenbottomblockentity");
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (botempty.get()) {
                            return ovenPos;
                        }
                    }
                }
            }
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            if (((StoneSmelterNewVariables) b).getCharcoalPos() != null) {
                BlockPos forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
                BlockEntity forge = this.world.getBlockEntity(forgePos);
                if (forge instanceof CharcoalForgeBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = forge.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty() && handler.getStackInSlot(4).isEmpty()));
                    if (botempty.get()) {
                        return forgePos;
                    }
                }
            }
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            if (((StoneSmelterNewVariables) b).getCharcoalPos() != null) {
                BlockPos forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
                BlockEntity forge = this.world.getBlockEntity(forgePos);
                if (forge instanceof CharcoalForgeBlockEntity) {
                    LazyOptional<IItemHandler> capabilityOpt = forge.getCapability(Capabilities.ITEM, null);
                    capabilityOpt.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty() && handler.getStackInSlot(4).isEmpty()));
                    if (botempty.get()) {
                        return forgePos;
                    }
                }
            }
        }
        if (this.building instanceof BuildingKitchen b) {
            for (BlockPos ovenPos : ((ChefNewVaraibles) b).getOvenList()) {
                BlockEntity oven = this.world.getBlockEntity(ovenPos);
                BlockEntity bottomOven = this.world.getBlockEntity(ovenPos.below());

                if (oven instanceof OvenTopBlockEntity) {
                    if (bottomOven instanceof OvenBottomBlockEntity) {
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (botempty.get()) {
                            return ovenPos;
                        }
                    }
                }
            }
            if (((ChefNewVaraibles) b).getVatPos() != null) {
                BlockPos vatPos = ((ChefNewVaraibles) b).getVatPos();
                if (this.world.getBlockEntity(vatPos) instanceof VatBlockEntity) {
                    if (this.world.getBlockEntity(vatPos.below()) instanceof OvenBottomBlockEntity bottomOven) {
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (botempty.get()) {
                            return vatPos;
                        }
                    }
                }
            }
            if (((ChefNewVaraibles) b).getPotPos() != null) {
                BlockPos potPos = ((ChefNewVaraibles) b).getPotPos();
                if (this.world.getBlockEntity(potPos) instanceof VatBlockEntity) {
                    if (this.world.getBlockEntity(potPos.below()) instanceof OvenBottomBlockEntity bottomOven) {
                        LazyOptional<IItemHandler> capabilityOpt2 = bottomOven.getCapability(Capabilities.ITEM, null);
                        capabilityOpt2.ifPresent(handler -> botempty.set(handler.getStackInSlot(0).isEmpty() && handler.getStackInSlot(1).isEmpty() && handler.getStackInSlot(2).isEmpty() && handler.getStackInSlot(3).isEmpty()));
                        if (botempty.get()) {
                            return potPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @author Ckeeze
     * @reason getting TFC heating devices with matching outputs - DONE
     */
    @Overwrite(remap = false)
    private BlockPos getFurnaceToRetrieveOutputFrom() {
        AtomicBoolean shouldReturn = new AtomicBoolean(false);
        BlockPos forgePos = null;
        BlockPos potPos = null;
        List<BlockPos> ovenPosList = List.of();
        if (this.building instanceof BuildingDyer b) {
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingBaker b) {
            ovenPosList = ((BakerNewVariables) b).getOvenList();
        }
        if (this.building instanceof BuildingKitchen b) {
            ovenPosList = ((ChefNewVaraibles) b).getOvenList();
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }
        if (this.building instanceof BuildingGlassblower b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }

        if (forgePos != null && this.world.getBlockEntity(forgePos) instanceof CharcoalForgeBlockEntity cForge) {
            LazyOptional<IItemHandler> capabilityOpt = cForge.getCapability(Capabilities.ITEM, null);

            capabilityOpt.ifPresent(handler -> {
                if (!handler.getStackInSlot(5).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(5), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(6).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(6), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(7).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(7), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(8).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(8), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(9).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(9), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
            });
            if (shouldReturn.get()) {
                return forgePos;
            }
        }

        if (potPos != null && this.world.getBlockEntity(potPos) instanceof PotBlockEntity pot) {
            LazyOptional<IItemHandler> capabilityOpt = pot.getCapability(Capabilities.ITEM, null);

            capabilityOpt.ifPresent(handler -> {
                if (!handler.getStackInSlot(4).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(4), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(5).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(5), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(6).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(6), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(7).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(7), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(8).isEmpty()) {
                    if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(8), this.currentRecipeStorage.getPrimaryOutput())) {
                        shouldReturn.set(true);
                    }
                }
            });
            if (shouldReturn.get()) {
                return potPos;
            }

        }

        if (!ovenPosList.isEmpty()) {
            for (BlockPos ovenPos : ovenPosList) {
                if (this.world.getBlockEntity(ovenPos) instanceof OvenTopBlockEntity oven) {
                    LazyOptional<IItemHandler> capabilityOpt = oven.getCapability(Capabilities.ITEM, null);

                    capabilityOpt.ifPresent(handler -> {
                        if (!handler.getStackInSlot(0).isEmpty()) {
                            if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(0), this.currentRecipeStorage.getPrimaryOutput())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(1).isEmpty()) {
                            if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(1), this.currentRecipeStorage.getPrimaryOutput())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(2).isEmpty()) {
                            if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(2), this.currentRecipeStorage.getPrimaryOutput())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(3).isEmpty()) {
                            if (this.currentRecipeStorage != null && ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(3), this.currentRecipeStorage.getPrimaryOutput())) {
                                shouldReturn.set(true);
                            }
                        }
                    });

                    if (shouldReturn.get()) {
                        return ovenPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @author Ckeeze
     * @reason Returning the position of a device that has inputs unrelated to the current crafting task - DONE
     */
    @Overwrite(remap = false)
    private BlockPos getFurnaceToRetrieveUnrelatedInputFrom() {
        AtomicBoolean shouldReturn = new AtomicBoolean(false);
        BlockPos forgePos = null;
        BlockPos potPos = null;
        List<BlockPos> ovenPosList = List.of();
        if (this.building instanceof BuildingDyer b) {
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingBaker b) {
            ovenPosList = ((BakerNewVariables) b).getOvenList();
        }
        if (this.building instanceof BuildingKitchen b) {
            ovenPosList = ((ChefNewVaraibles) b).getOvenList();
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }
        if (this.building instanceof BuildingGlassblower b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }

        if (forgePos != null && this.world.getBlockEntity(forgePos) instanceof CharcoalForgeBlockEntity cForge) {
            LazyOptional<IItemHandler> capabilityOpt = cForge.getCapability(Capabilities.ITEM, null);

            capabilityOpt.ifPresent(handler -> {
                if (!handler.getStackInSlot(5).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(5), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(6).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(6), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(7).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(7), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(8).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(8), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(9).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(9), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
            });
            if (shouldReturn.get()) {
                return forgePos;
            }
        }

        if (potPos != null && this.world.getBlockEntity(potPos) instanceof PotBlockEntity pot) {
            LazyOptional<IItemHandler> capabilityOpt = pot.getCapability(Capabilities.ITEM, null);

            capabilityOpt.ifPresent(handler -> {
                if (!handler.getStackInSlot(4).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(4), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(5).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(5), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(6).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(6), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(7).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(7), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
                if (!handler.getStackInSlot(8).isEmpty()) {
                    if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(8), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                        shouldReturn.set(true);
                    }
                }
            });
            if (shouldReturn.get()) {
                return potPos;
            }

        }

        if (!ovenPosList.isEmpty()) {
            for (BlockPos ovenPos : ovenPosList) {
                if (this.world.getBlockEntity(ovenPos) instanceof OvenTopBlockEntity oven) {
                    LazyOptional<IItemHandler> capabilityOpt = oven.getCapability(Capabilities.ITEM, null);

                    capabilityOpt.ifPresent(handler -> {
                        if (!handler.getStackInSlot(0).isEmpty()) {
                            if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(0), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(1).isEmpty()) {
                            if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(1), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(2).isEmpty()) {
                            if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(2), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                                shouldReturn.set(true);
                            }
                        }
                        if (!handler.getStackInSlot(3).isEmpty()) {
                            if (this.currentRecipeStorage == null || !ItemStackUtils.compareItemStacksIgnoreStackSize(handler.getStackInSlot(3), this.currentRecipeStorage.getCleanedInput().get(0).getItemStack())) {
                                shouldReturn.set(true);
                            }
                        }
                    });

                    if (shouldReturn.get()) {
                        return ovenPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @author Ckeeze
     * @reason Detect if the new device positions are loaded in the world - DONE
     */
    @Overwrite(remap = false)
    private boolean areFurnacesLoaded() {
        BlockPos forgePos = null;
        BlockPos potPos = null;
        List<BlockPos> ovenPosList = List.of();
        if (this.building instanceof BuildingDyer b) {
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingBaker b) {
            ovenPosList = ((BakerNewVariables) b).getOvenList();
        }
        if (this.building instanceof BuildingKitchen b) {
            ovenPosList = ((ChefNewVaraibles) b).getOvenList();
            potPos = ((ChefNewVaraibles) b).getPotPos();
        }
        if (this.building instanceof BuildingStoneSmeltery b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }
        if (this.building instanceof BuildingGlassblower b) {
            forgePos = ((StoneSmelterNewVariables) b).getCharcoalPos();
        }

        if (potPos != null) {
            if (!WorldUtil.isBlockLoaded(this.world, potPos)) {
                return false;
            }
        }
        if (forgePos != null) {
            if (!WorldUtil.isBlockLoaded(this.world, forgePos)) {
                return false;
            }
        }
        for (BlockPos ovenPos : ovenPosList) {
            if (!WorldUtil.isBlockLoaded(this.world, ovenPos)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @author Ckeeze
     * @reason don't need behavior - DONE
     */
    @Overwrite(remap = false)
    private int countOfBurningFurnaces() {
        return 0;
    }

    @Shadow(remap = false)
    private int getMaxUsableFurnaces() {
        return 0;
    }

    @Shadow
    protected abstract void recordSmeltingBuildingStats(Component hoverName, int count);

    /**
     * @author Ckeeze
     * @reason getting fuel - DONE
     */
    @Overwrite(remap = false)
    private List<ItemStack> getAllowedFuel() {
        List<Item> forgeFuelsTag = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.FORGE_FUEL).stream().toList();
        List<Item> ovenFuelsTag = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(FLTags.Items.OVEN_FUEL).stream().toList();
        List<ItemStack> list = new ArrayList<>();
        if (this.building instanceof BuildingKitchen || this.building instanceof BuildingBaker || this.building instanceof BuildingDyer) {
            for (Item item : ovenFuelsTag) {
                list.add(new ItemStack(item, 16));
            }

        } else {
            for (Item item : forgeFuelsTag) {
                list.add(new ItemStack(item, 16));
            }
        }
        return list;
    }
}
