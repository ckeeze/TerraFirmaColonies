package net.ckeeze.terrafirmacolonies.mixin;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.blocks.BlockScarecrow;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingFarmer;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.IFarmland;
import net.dries007.tfc.common.blocks.GroundcoverBlockType;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.crop.Crop;
import net.dries007.tfc.common.blocks.crop.DoubleCropBlock;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.common.items.Powder;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.dries007.tfc.common.blockentities.FarmlandBlockEntity.NutrientType.*;
import static net.dries007.tfc.common.blocks.soil.FarmlandBlock.getHydration;

@Mixin(value = EntityAIWorkFarmer.class, remap = false)
public abstract class EntityAIWorkFarmerMixin extends AbstractEntityAICrafting<JobFarmer, BuildingFarmer> {

    public EntityAIWorkFarmerMixin(@NotNull JobFarmer job) {
        super(job);
    }

    @Shadow(remap = false)
    private void equipHoe() {
    }

    @Shadow
    private BlockPos getSurfacePos(final BlockPos position) {
        return position;
    }

    @Shadow
    private boolean didWork;

    /**
     * @author Ckeeze
     * @reason Request fertilizers
     */
    @Inject(
            method = {"prepareForFarming"},
            at = {@At("HEAD")},
            remap = false
    )
    private void terrafirmacolonies_prepareForFarming(CallbackInfoReturnable<IAIState> cir) {
        int FertilizerInBuilding = InventoryUtils.hasBuildingEnoughElseCount(this.building, this::isCompost, 1);
        int FertilizerInInventory = InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), this::isCompost);
        if (this.building != null && this.building.getBuildingLevel() >= 1) {
            if (FertilizerInBuilding + FertilizerInInventory <= 0) {
                if (this.building.requestFertilizer() && !this.building.hasWorkerOpenRequestsOfType(this.worker.getCitizenData().getId(), TypeToken.of(StackList.class))) {
                    List<ItemStack> compostAbleItems = new ArrayList<>();
                    //Adding TFC fertilizers to the list
                    //Saltpeter is not present to prevent colonies from wasting the important gunpowder component
                    compostAbleItems.add(new ItemStack(TFCItems.POWDERS.get(Powder.SYLVITE).get(), 1));
                    compostAbleItems.add(new ItemStack(TFCItems.POWDERS.get(Powder.WOOD_ASH).get(), 1));
                    compostAbleItems.add(new ItemStack(TFCItems.COMPOST.get(), 1));
                    compostAbleItems.add(new ItemStack(TFCBlocks.GROUNDCOVER.get(GroundcoverBlockType.GUANO).get().asItem(), 1));
                    this.worker.getCitizenData().createRequestAsync(new StackList(compostAbleItems, "com.minecolonies.coremod.request.fertilizer", 32, 1));
                }
            }
        }
    }

    /**
     * @author Ckeeze
     * @reason Detect TFC Soil
     */
    @Overwrite(remap = false)
    private BlockPos findHoeableSurface(@NotNull BlockPos position, @NotNull final FarmField farmField) {
        position = this.getSurfacePos(position);
        //IDE inspection is incorrect position can actually be null
        if (position == null
                || farmField.isNoPartOfField(world, position)
                || (world.getBlockState(position.above()).getBlock() instanceof CropBlock)
                || (world.getBlockState(position.above()).getBlock() instanceof BlockScarecrow)
                //Our arguments:
                || !(world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SANDY_LOAM).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.LOAM).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SILT).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SILTY_LOAM).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SANDY_LOAM).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.LOAM).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SILT).get())
                || world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SILTY_LOAM).get()))
        ) {
            return null;
        }
        return position;
    }

    /**
     * @author Ckeeze
     * @reason Detect TFC Pumpkin / Melon
     */
    @Overwrite(remap = false)
    private BlockPos getSurfacePos(BlockPos position, Integer depth) {
        if (Math.abs(depth) <= 5 && WorldUtil.isBlockLoaded(this.world, position)) {
            BlockState curBlockState = this.world.getBlockState(position);
            Block curBlock = curBlockState.getBlock();
            //Changed Arguments from banilla blocks
            if ((!curBlockState.isSolid() || curBlockState.is(TFCBlocks.PUMPKIN.get()) || curBlockState.is(TFCBlocks.MELON.get()) || curBlock instanceof WebBlock)) {
                return depth > 0 ? position.below() : this.getSurfacePos(position.below(), depth - 1);
            } else {
                return depth < 0 ? position : this.getSurfacePos(position.above(), depth + 1);
            }
        } else {
            return null;
        }
    }

    /**
     * @author Ckeeze
     * @reason Till TFC Soil
     */
    @Overwrite(remap = false)
    private boolean hoeIfAble(BlockPos position, FarmField farmField) {
        position = this.findHoeableSurface(position, farmField);
        if (position != null && !this.checkForToolOrWeapon(ModEquipmentTypes.hoe.get())) {
            if (this.mineBlock(position.above())) {
                this.didWork = true;
                this.equipHoe();
                this.worker.swing(this.worker.getUsedItemHand());

                //Added branches
                if (world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.LOAM).get()) ||
                        world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.LOAM).get())) {
                    world.setBlockAndUpdate(position, TFCBlocks.SOIL.get(SoilBlockType.FARMLAND).get(SoilBlockType.Variant.LOAM).get().defaultBlockState());
                } else if (world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SILTY_LOAM).get()) ||
                        world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SILTY_LOAM).get())) {
                    world.setBlockAndUpdate(position, TFCBlocks.SOIL.get(SoilBlockType.FARMLAND).get(SoilBlockType.Variant.SILTY_LOAM).get().defaultBlockState());
                } else if (world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SILT).get()) ||
                        world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SILT).get())) {
                    world.setBlockAndUpdate(position, TFCBlocks.SOIL.get(SoilBlockType.FARMLAND).get(SoilBlockType.Variant.SILT).get().defaultBlockState());
                } else if (world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.GRASS).get(SoilBlockType.Variant.SANDY_LOAM).get()) ||
                        world.getBlockState(position).is(TFCBlocks.SOIL.get(SoilBlockType.DIRT).get(SoilBlockType.Variant.SANDY_LOAM).get())) {
                    world.setBlockAndUpdate(position, TFCBlocks.SOIL.get(SoilBlockType.FARMLAND).get(SoilBlockType.Variant.SANDY_LOAM).get().defaultBlockState());
                } else {
                    return false;
                }

                CitizenItemUtils.damageItemInHand(this.worker, InteractionHand.MAIN_HAND, 1);
                this.worker.decreaseSaturationForContinuousAction();
                Objects.requireNonNull(this.worker.getCitizenColonyHandler().getColonyOrRegister()).getStatisticsManager().increment("land_tilled", this.worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * @author Ckeeze
     * @reason TFC Crop planting
     */
    @Overwrite(remap = false)
    private boolean plantCrop(ItemStack item, @NotNull BlockPos position) {
        if (item != null && !item.isEmpty()) {
            int slot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(item.getItem());
            int stickslot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(Items.STICK);
            int Hyd = getHydration(this.world, position);
            float Temp = Climate.getTemperature(this.world, position);

            boolean cropplanted = false;
            if (slot == -1) {
                return false;
            } else {
                Item seed = item.getItem();

                //Added branches
                if (seed == TFCItems.CROP_SEEDS.get(Crop.JUTE).get() && Hyd >= 25 && Temp >= 8.0 && Temp <= 34.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.JUTE).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.SUGARCANE).get() && Hyd >= 40 && Temp >= 15.0 && Temp <= 35.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.SUGARCANE).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.BARLEY).get() && Hyd >= 18 && Hyd <= 75 && Temp >= -5.0 && Temp <= 23.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.BARLEY).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.WHEAT).get() && Hyd >= 25 && Temp >= -1.0 && Temp <= 32.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.WHEAT).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.OAT).get() && Hyd >= 35 && Temp >= 6.0 && Temp <= 37.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.OAT).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.RYE).get() && Hyd >= 25 && Hyd <= 85 && Temp >= -8.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.RYE).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.PAPYRUS).get() && Hyd >= 70 && Temp >= 22.0 && Temp <= 34.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.PAPYRUS).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.MAIZE).get() && Hyd >= 75 && Temp >= 16.0 && Temp <= 37.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.MAIZE).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.GARLIC).get() && Hyd >= 15 && Hyd <= 75 && Temp >= -17.0 && Temp <= 15.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.GARLIC).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.CARROT).get() && Hyd >= 25 && Temp >= 6.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.CARROT).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.CABBAGE).get() && Hyd >= 15 && Hyd <= 65 && Temp >= -7.0 && Temp <= 24.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.CABBAGE).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.POTATO).get() && Hyd >= 50 && Temp >= 2.0 && Temp <= 34.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.POTATO).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.ONION).get() && Hyd >= 25 && Hyd <= 90 && Temp >= 3.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.ONION).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.BEET).get() && Hyd >= 15 && Hyd <= 85 && Temp >= -2.0 && Temp <= 17.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.BEET).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.SOYBEAN).get() && Hyd >= 40 && Temp >= 11.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.SOYBEAN).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.SQUASH).get() && Hyd >= 23 && Hyd <= 95 && Temp >= 8.0 && Temp <= 30.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.SQUASH).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.YELLOW_BELL_PEPPER).get() && Hyd >= 25 && Hyd <= 60 && Temp >= 19.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.YELLOW_BELL_PEPPER).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.RED_BELL_PEPPER).get() && Hyd >= 25 && Hyd <= 60 && Temp >= 19.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.RED_BELL_PEPPER).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.SOYBEAN).get() && Hyd >= 40 && Temp >= 11.0 && Temp <= 27.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.SOYBEAN).get().defaultBlockState());
                    cropplanted = true;
                }

                //Cropsticks
                else if (seed == TFCItems.CROP_SEEDS.get(Crop.GREEN_BEAN).get() && stickslot != -1 && Hyd >= 38 && Temp >= 5.0 && Temp <= 32.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.GREEN_BEAN).get().defaultBlockState().setValue(TFCBlockStateProperties.STICK, true));
                    this.world.setBlockAndUpdate(position.above(2), TFCBlocks.CROPS.get(Crop.GREEN_BEAN).get().defaultBlockState().setValue(TFCBlockStateProperties.STICK, true).setValue(TFCBlockStateProperties.DOUBLE_CROP_PART, DoubleCropBlock.Part.TOP));
                    this.getInventory().extractItem(stickslot, 1, false);
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.TOMATO).get() && stickslot != -1 && Hyd >= 30 && Hyd <= 95 && Temp >= 3.0 && Temp <= 33.0) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.TOMATO).get().defaultBlockState().setValue(TFCBlockStateProperties.STICK, true));
                    this.world.setBlockAndUpdate(position.above(2), TFCBlocks.CROPS.get(Crop.TOMATO).get().defaultBlockState().setValue(TFCBlockStateProperties.STICK, true).setValue(TFCBlockStateProperties.DOUBLE_CROP_PART, DoubleCropBlock.Part.TOP));
                    this.getInventory().extractItem(stickslot, 1, false);
                    cropplanted = true;
                }

                //Pumpkin and melon
                else if (seed == TFCItems.CROP_SEEDS.get(Crop.PUMPKIN).get() && Hyd >= 30 && Hyd <= 80 && Temp >= 3.0 && Temp <= 27.0) {
                    if (this.building.getPrevPos() != null && !this.world.isEmptyBlock(this.building.getPrevPos().above())) {
                        return true;
                    }
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.PUMPKIN).get().defaultBlockState());
                    cropplanted = true;
                } else if (seed == TFCItems.CROP_SEEDS.get(Crop.MELON).get() && Hyd >= 75 && Hyd <= 100 && Temp >= 8.0 && Temp <= 34.0) {
                    if (this.building.getPrevPos() != null && !this.world.isEmptyBlock(this.building.getPrevPos().above())) {
                        return true;
                    }
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.MELON).get().defaultBlockState());
                    cropplanted = true;
                }

                //Rice
                else if (seed == TFCItems.CROP_SEEDS.get(Crop.RICE).get() && Hyd >= 25 && Hyd <= 100 && Temp >= 12.0 && Temp <= 27.0 && this.world.getBlockState(position.above()).getFluidState().is(TFCTags.Fluids.ANY_FRESH_WATER)) {
                    this.world.setBlockAndUpdate(position.above(), TFCBlocks.CROPS.get(Crop.RICE).get().defaultBlockState());
                    cropplanted = true;
                }
                if (cropplanted) {
                    this.worker.decreaseSaturationForContinuousAction();
                    this.getInventory().extractItem(slot, 1, false);
                    this.didWork = true;
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * @author Ckeeze
     * @reason fixing not planting bug
     */
    @Overwrite(remap = false)
    private BlockPos findPlantableSurface(@NotNull BlockPos position, @NotNull FarmField farmField) {
        position = this.getSurfacePos(position);
        //position can in fact be null here
        return position != null && !farmField.isNoPartOfField(this.world, position) && ((this.world.getBlockState(position.above()).getBlock() instanceof AirBlock) || (this.world.getBlockState(position.above()).getFluidState().is(TFCTags.Fluids.ANY_INFINITE_WATER))) ? position : null;
    }

    /**
     * @author Ckeeze
     * @reason fixing not planting bug
     */
    @Overwrite(remap = false)
    private boolean isCompost(ItemStack itemStack) {
        return itemStack.getItem() == Items.BONE_MEAL
                || itemStack.getItem() == TFCItems.POWDERS.get(Powder.WOOD_ASH).get()
                || itemStack.getItem() == TFCItems.POWDERS.get(Powder.SALTPETER).get()
                || itemStack.getItem() == TFCItems.POWDERS.get(Powder.SYLVITE).get()
                || itemStack.getItem() == TFCItems.COMPOST.get()
                || itemStack.getItem() == TFCBlocks.GROUNDCOVER.get(GroundcoverBlockType.GUANO).get().asItem();
    }

    /**
     * @author Ckeeze
     * @reason fixing not planting bug
     */
    @Overwrite(remap = false)
    private BlockPos findHarvestableSurface(@NotNull BlockPos position) {
        position = this.getSurfacePos(position);
        BlockEntity Farmland = this.world.getBlockEntity(position);
        if (position == null) {
            return null;
        } else {
            BlockState state = this.world.getBlockState(position.above());
            Block block = state.getBlock();
            //Changed from vanilla Pumpkin and Melon
            if (block != TFCBlocks.PUMPKIN.get() && block != TFCBlocks.MELON.get() && !(block instanceof net.dries007.tfc.common.blocks.crop.DeadCropBlock)) {
                if (block instanceof net.dries007.tfc.common.blocks.crop.CropBlock crop) {
                    if (crop.isMaxAge(state)) {
                        return position;
                    } else {
                        int amountOfCompostInInv = InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), this::isCompost);
                        if (amountOfCompostInInv == 0) {
                            return null;
                        } else if (Farmland instanceof IFarmland farmland) {

                            //TFC Fertilizing behavior
                            int bone_mealSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(Items.BONE_MEAL);
                            int saltpeterSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(TFCItems.POWDERS.get(Powder.SALTPETER).get());
                            int wood_ashSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(TFCItems.POWDERS.get(Powder.WOOD_ASH).get());
                            int compostSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(TFCItems.COMPOST.get());
                            int sylviteSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(TFCItems.POWDERS.get(Powder.SYLVITE).get());
                            int guanoSlot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(TFCBlocks.GROUNDCOVER.get(GroundcoverBlockType.GUANO).get().asItem());

                            if (crop.getPrimaryNutrient() == PHOSPHOROUS && farmland.getNutrient(PHOSPHOROUS) < 0.5) {
                                if (bone_mealSlot != -1) {
                                    farmland.addNutrient(PHOSPHOROUS, 0.1F);
                                    this.getInventory().extractItem(bone_mealSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (wood_ashSlot != -1) {
                                    farmland.addNutrient(PHOSPHOROUS, 0.1F);
                                    farmland.addNutrient(POTASSIUM, 0.3F);
                                    this.getInventory().extractItem(wood_ashSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (compostSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.4F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.2F);
                                    farmland.addNutrient(POTASSIUM, 0.4F);
                                    this.getInventory().extractItem(compostSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (guanoSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.8F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.5F);
                                    farmland.addNutrient(POTASSIUM, 0.1F);
                                    this.getInventory().extractItem(guanoSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                }

                            }
                            if (crop.getPrimaryNutrient() == POTASSIUM && farmland.getNutrient(POTASSIUM) < 0.5) {
                                if (sylviteSlot != -1) {
                                    farmland.addNutrient(POTASSIUM, 0.5F);
                                    this.getInventory().extractItem(sylviteSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (wood_ashSlot != -1) {
                                    farmland.addNutrient(PHOSPHOROUS, 0.1F);
                                    farmland.addNutrient(POTASSIUM, 0.3F);
                                    this.getInventory().extractItem(wood_ashSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (saltpeterSlot != -1) {
                                    farmland.addNutrient(POTASSIUM, 0.4F);
                                    farmland.addNutrient(NITROGEN, 0.1F);
                                    this.getInventory().extractItem(saltpeterSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (compostSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.4F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.2F);
                                    farmland.addNutrient(POTASSIUM, 0.4F);
                                    this.getInventory().extractItem(compostSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (guanoSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.8F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.5F);
                                    farmland.addNutrient(POTASSIUM, 0.1F);
                                    this.getInventory().extractItem(guanoSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                }
                            }
                            if (crop.getPrimaryNutrient() == NITROGEN && farmland.getNutrient(NITROGEN) < 0.5) {
                                if (guanoSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.8F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.5F);
                                    farmland.addNutrient(POTASSIUM, 0.1F);
                                    this.getInventory().extractItem(guanoSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (saltpeterSlot != -1) {
                                    farmland.addNutrient(POTASSIUM, 0.4F);
                                    farmland.addNutrient(NITROGEN, 0.1F);
                                    this.getInventory().extractItem(saltpeterSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                } else if (compostSlot != -1) {
                                    farmland.addNutrient(NITROGEN, 0.4F);
                                    farmland.addNutrient(PHOSPHOROUS, 0.2F);
                                    farmland.addNutrient(POTASSIUM, 0.4F);
                                    this.getInventory().extractItem(compostSlot, 1, false);
                                    Helpers.playSound(this.world, position, TFCSounds.FERTILIZER_USE.get());
                                }
                            }
                        }
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return position;
            }
        }
    }

    /**
     * @author Ckeeze
     * @reason getting sticks when using beans or tomato
     */
    @Overwrite(remap = false)
    private IAIState canGoPlanting(@NotNull FarmField farmField) {
        if (farmField.getSeed() == null) {
            return AIWorkerState.PREPARING;
        } else {
            ItemStack seeds = farmField.getSeed();
            int slot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(seeds.getItem());
            if (slot != -1) {
                return AIWorkerState.FARMER_PLANT;
            } else if (!this.walkToBuilding()) {
                //Changed from module based seed check
                terrafirmacolonies$getSeedIfNeeded(seeds);
                return AIWorkerState.PREPARING;
            } else {
                //Changed from module based seed check
                terrafirmacolonies$getSeedIfNeeded(seeds);
                seeds.setCount(seeds.getMaxStackSize());
                if (!this.checkIfRequestForItemExistOrCreateAsync(seeds, seeds.getMaxStackSize(), 1)) {
                    farmField.nextState();
                }
                return AIWorkerState.PREPARING;
            }
        }
    }

    @Unique
    private void terrafirmacolonies$getSeedIfNeeded(@NotNull ItemStack seeds) {
        if (seeds.getItem() == TFCItems.CROP_SEEDS.get(Crop.TOMATO).get() || seeds.getItem() == TFCItems.CROP_SEEDS.get(Crop.GREEN_BEAN).get()) {
            int slot = this.worker.getCitizenInventoryHandler().findFirstSlotInInventoryWith(Items.STICK);
            if (slot == -1) {
                this.checkIfRequestForItemExistOrCreateAsync(Items.STICK.getDefaultInstance(), Items.STICK.getMaxStackSize(), 1);
            }
        }
    }
}
