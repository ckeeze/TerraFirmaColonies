package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.modules.ItemListModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingComposter;
import com.minecolonies.core.colony.jobs.JobComposter;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkComposter;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.ComposterBlockEntity;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntityAIWorkComposter.class)
public abstract class EntityAIWorkComposterMixin extends AbstractEntityAIInteract<JobComposter, BuildingComposter> {

    @Shadow
    private BlockPos currentTarget;

    @Shadow
    @Final
    private static VisibleCitizenStatus COMPOST;

    @Shadow
    protected abstract void complain();

    public EntityAIWorkComposterMixin(@NotNull JobComposter job) {
        super(job);
    }


    /**
     * @author Ckeeze
     * @reason rewriting AI
     */
    @Overwrite(remap = false)
    private IAIState accelerateBarrels() {
        return null;
    }

    /**
     * @author Ckeeze
     * @reason getting both Green and Brown compostables
     */
    @Overwrite(remap = false)
    private IAIState getMaterials() {
        if (!this.walkToBuilding()) {
            this.setDelay(2);
            return this.getState();
        } else {
            List<ItemStorage> list = this.building.getModuleMatching(ItemListModule.class, (m) -> m.getId().equals("compostables")).getList();

            //Checking for both brown and green items
            if (list.isEmpty()) {
                this.complain();
                return this.getState();
            } else {
                if (InventoryUtils.hasItemInProvider(this.building, (stack) -> list.contains(new ItemStorage(stack)) && terrafirmacolonies$isGreenCompost(stack))) {
                    InventoryUtils.transferItemStackIntoNextFreeSlotFromProvider(this.building, InventoryUtils.findFirstSlotInProviderNotEmptyWith(this.building, (stack) -> list.contains(new ItemStorage(stack)) && terrafirmacolonies$isGreenCompost(stack)), this.worker.getInventoryCitizen());
                }
                if (InventoryUtils.hasItemInProvider(this.building, (stack) -> list.contains(new ItemStorage(stack)) && terrafirmacolonies$isBrownCompost(stack))) {
                    InventoryUtils.transferItemStackIntoNextFreeSlotFromProvider(this.building, InventoryUtils.findFirstSlotInProviderNotEmptyWith(this.building, (stack) -> list.contains(new ItemStorage(stack)) && terrafirmacolonies$isBrownCompost(stack)), this.worker.getInventoryCitizen());
                }

                //Find brown and green items
                int brownSlot = InventoryUtils.findFirstSlotInItemHandlerWith(this.worker.getInventoryCitizen(), this::terrafirmacolonies$isGreenCompost);
                int greenSlot = InventoryUtils.findFirstSlotInItemHandlerWith(this.worker.getInventoryCitizen(), this::terrafirmacolonies$isBrownCompost);
                
                if (brownSlot >= 0 && greenSlot >= 0) {
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, this.worker.getInventoryCitizen().getStackInSlot(brownSlot));
                } else {
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

                    if (!this.building.hasWorkerOpenRequests(this.worker.getCitizenData().getId())) {
                        ArrayList<ItemStack> itemList = new ArrayList<>();

                        if (brownSlot == -1) {
                            //Requesting brown items

                            for (ItemStorage item : list) {
                                if (terrafirmacolonies$isBrownCompost(item.getItemStack())) {
                                    ItemStack itemStack = item.getItemStack();
                                    itemStack.setCount(itemStack.getMaxStackSize());
                                    itemList.add(itemStack);
                                }
                            }
                            if (!itemList.isEmpty()) {
                                this.worker.getCitizenData().createRequestAsync(new StackList(itemList, "com.minecolonies.coremod.request.compostable", 64 * this.building.getBarrels().size(), 1, this.building.getSetting(BuildingComposter.MIN).getValue()));
                            }
                        }
                        if (greenSlot == -1) {
                            //Requesting green items
                            itemList.clear();
                            for (ItemStorage item : list) {
                                if (terrafirmacolonies$isGreenCompost(item.getItemStack())) {
                                    ItemStack itemStack = item.getItemStack();
                                    itemStack.setCount(itemStack.getMaxStackSize());
                                    itemList.add(itemStack);
                                }
                            }
                            if (!itemList.isEmpty()) {
                                this.worker.getCitizenData().createRequestAsync(new StackList(itemList, "com.minecolonies.coremod.request.compostable", 64 * this.building.getBarrels().size(), 1, this.building.getSetting(BuildingComposter.MIN).getValue()));
                            }
                        }

                    }

                    this.setDelay(2);
                }
                return AIWorkerState.START_WORKING;
            }
        }
    }


    /**
     * @author Ckeeze
     * @reason AI rewrite
     */
    @Overwrite(remap = false)
    private IAIState decideWhatToDo() {
        this.worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        if (!this.walkToBuilding()) {
            this.setDelay(2);
            return this.getState();
        } else {
            BuildingComposter building = this.building;

            for (BlockPos barrel : building.getBarrels()) {
                BlockEntity composter = this.world.getBlockEntity(barrel);
                if (composter instanceof ComposterBlockEntity) {
                    this.currentTarget = barrel;
                    if (((ComposterBlockEntity) composter).isReady()) {
                        this.setDelay(20);
                        this.worker.getCitizenData().setVisibleStatus(COMPOST);
                        return AIWorkerState.COMPOSTER_HARVEST;
                    }

                    if (((ComposterBlockEntity) composter).getBrown() < 16 || ((ComposterBlockEntity) composter).getGreen() < 16) {
                        this.currentTarget = barrel;
                        this.setDelay(20);
                        this.worker.getCitizenData().setVisibleStatus(COMPOST);
                        return AIWorkerState.COMPOSTER_FILL;
                    }
                }
            }

            this.setDelay(20);
            return AIWorkerState.START_WORKING;
        }
    }

    //Check if items are consumed properly

    /**
     * @author Ckeeze
     * @reason AI rewrite
     */
    @Overwrite(remap = false)
    private IAIState fillBarrels() {
        if (this.worker.getItemInHand(InteractionHand.MAIN_HAND) == ItemStack.EMPTY) {
            int slot = InventoryUtils.findFirstSlotInItemHandlerWith(this.worker.getInventoryCitizen(), (stack) -> this.building.getModuleMatching(ItemListModule.class, (m) -> m.getId().equals("compostables")).isItemInList(new ItemStorage(stack)));
            if (slot < 0) {
                return AIWorkerState.GET_MATERIALS;
            }

            this.worker.setItemInHand(InteractionHand.MAIN_HAND, this.worker.getInventoryCitizen().getStackInSlot(slot));
        }

        if (!this.walkToWorkPos(this.currentTarget)) {
            this.setDelay(2);
            return this.getState();
        } else {
            if (this.world.getBlockEntity(this.currentTarget) instanceof ComposterBlockEntity composter) {

                FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) this.worker.level());

                if (composter.getGreen() < 16) {
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, this.worker.getInventoryCitizen().getStackInSlot(InventoryUtils.findFirstSlotInItemHandlerWith(this.worker.getInventoryCitizen(), this::terrafirmacolonies$isGreenCompost)));
                    if (this.worker.getItemInHand(InteractionHand.MAIN_HAND) == ItemStack.EMPTY) {
                        return AIWorkerState.GET_MATERIALS;
                    }
                    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(this.worker.getItemInHand(InteractionHand.MAIN_HAND).getItem()));
                    composter.use(fakePlayer.getItemInHand(InteractionHand.MAIN_HAND), fakePlayer, false);
                    String compostingItem = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getItem().getDescriptionId();
                    int countBefore = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount();
                    this.worker.getCitizenExperienceHandler().addExperience(1.0F);
                    StatsUtil.trackStatByName(this.building, "items_composted", compostingItem, countBefore - this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount());
                    this.worker.getMainHandItem().shrink(1);
                    this.worker.decreaseSaturationForContinuousAction();
                    this.worker.queueSound(SoundEvents.ROOTED_DIRT_PLACE, currentTarget, 10, 0, 0.9F, this.worker.getRandom().nextFloat());
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);

                }

                if (composter.getBrown() < 16) {
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, this.worker.getInventoryCitizen().getStackInSlot(InventoryUtils.findFirstSlotInItemHandlerWith(this.worker.getInventoryCitizen(), this::terrafirmacolonies$isBrownCompost)));
                    if (this.worker.getItemInHand(InteractionHand.MAIN_HAND) == ItemStack.EMPTY) {
                        return AIWorkerState.GET_MATERIALS;
                    }

                    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(this.worker.getItemInHand(InteractionHand.MAIN_HAND).getItem()));
                    composter.use(fakePlayer.getItemInHand(InteractionHand.MAIN_HAND), fakePlayer, false);
                    String compostingItem = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getItem().getDescriptionId();
                    int countBefore = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount();
                    this.worker.getCitizenExperienceHandler().addExperience(1.0F);
                    StatsUtil.trackStatByName(this.building, "items_composted", compostingItem, countBefore - this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount());
                    this.worker.getMainHandItem().shrink(1);
                    this.worker.decreaseSaturationForContinuousAction();
                    this.worker.queueSound(SoundEvents.ROOTED_DIRT_PLACE, currentTarget, 10, 0, 0.9F, this.worker.getRandom().nextFloat());
                    this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);
                }

                /* Old code:
                CitizenItemUtils.hitBlockWithToolInHand(this.worker, this.currentTarget);
                TileEntityBarrel barrel = (TileEntityBarrel) this.world.getBlockEntity(this.currentTarget);
                CitizenItemUtils.hitBlockWithToolInHand(this.worker, this.currentTarget);
                String compostingItem = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getItem().getDescriptionId();
                int countBefore = this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount();
                barrel.addItem(this.worker.getItemInHand(InteractionHand.MAIN_HAND));
                this.worker.getCitizenExperienceHandler().addExperience((double) 1.0F);
                StatsUtil.trackStatByName(this.building, "items_composted", compostingItem, countBefore - this.worker.getItemInHand(InteractionHand.MAIN_HAND).getCount());
                this.worker.decreaseSaturationForContinuousAction();
                this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStackUtils.EMPTY);
                */
            }

            this.setDelay(5);
            return AIWorkerState.START_WORKING;
        }
    }

    /**
     * @author Ckeeze
     * @reason rewriting Worker AI
     */
    @Overwrite(remap = false)
    private IAIState harvestBarrels() {
        if (!this.walkToWorkPos(this.currentTarget)) {
            this.setDelay(2);
            return this.getState();
        } else {
            if (this.world.getBlockEntity(this.currentTarget) instanceof ComposterBlockEntity te) {
                CitizenItemUtils.hitBlockWithToolInHand(this.worker, this.currentTarget);
                te.reset();
                ItemStack compost = new ItemStack(TFCItems.COMPOST.get(), this.terrafirmacolonies$getLootMultiplier(this.worker.getRandom()));
                this.worker.queueSound(SoundEvents.ROOTED_DIRT_PLACE, currentTarget, 10, 0, 0.9F, this.worker.getRandom().nextFloat());
                StatsUtil.trackStatByName(this.building, "product_collected", compost.getItem().getDescriptionId(), compost.getCount());
                InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), compost);
                this.worker.getCitizenExperienceHandler().addExperience(1.0F);
                this.incrementActionsDoneAndDecSaturation();
            }

            this.setDelay(5);
            return AIWorkerState.START_WORKING;
        }
    }


    @Unique
    private int terrafirmacolonies$getLootMultiplier(RandomSource random) {
        int citizenLevel = (int) ((double) this.getSecondarySkillLevel() / (double) 2.0F);
        int diceResult = random.nextInt(100);
        if (diceResult <= citizenLevel * 2) {
            return 3;
        } else if (diceResult <= citizenLevel * 4) {
            return 2;
        } else {
            return 1;
        }
    }

    @Unique
    private boolean terrafirmacolonies$isBrownCompost(ItemStack item) {
        return item.is(TFCTags.Items.COMPOST_BROWNS) || item.is(TFCTags.Items.COMPOST_BROWNS_LOW) || item.is(TFCTags.Items.COMPOST_BROWNS_HIGH);
    }

    @Unique
    private boolean terrafirmacolonies$isGreenCompost(ItemStack item) {
        return item.is(TFCTags.Items.COMPOST_GREENS) || item.is(TFCTags.Items.COMPOST_GREENS_HIGH) || item.is(TFCTags.Items.COMPOST_GREENS_LOW);
    }
}
