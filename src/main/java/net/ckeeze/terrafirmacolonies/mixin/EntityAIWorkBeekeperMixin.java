package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.FLHelpers;
import com.eerussianguy.firmalife.common.blockentities.FLBeehiveBlockEntity;
import com.eerussianguy.firmalife.common.blockentities.FLBlockEntities;
import com.eerussianguy.firmalife.common.capabilities.bee.IBee;
import com.eerussianguy.firmalife.common.items.FLItems;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobBeekeeper;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkBeekeeper;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.ckeeze.terrafirmacolonies.api.TFCEquipmentTypes;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@Mixin(value = EntityAIWorkBeekeeper.class, remap = false)
public abstract class EntityAIWorkBeekeperMixin extends AbstractEntityAIInteract<JobBeekeeper, BuildingBeekeeper> {

    public EntityAIWorkBeekeperMixin(@NotNull JobBeekeeper job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason Change required items
     */
    @Overwrite(remap = false)
    private IAIState prepareForHerding() {
        this.setDelay(40);
        if (!this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honey") && this.checkForToolOrWeapon(TFCEquipmentTypes.tfcknife.get())) {
            return this.getState();
        } else {
            if (!this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honeycomb")) {
                this.checkIfRequestForItemExistOrCreateAsync(new ItemStack(TFCItems.EMPTY_JAR.get()));
            }

            return AIWorkerState.DECIDE;
        }
    }

    /**
     * @author Ckeeze
     * @reason Rewriting simpler AI
     */
    @Overwrite(remap = false)
    private IAIState decideWhatToDo() {

        this.setDelay(40 + (99 / this.getSecondarySkillLevel() - 1));

        Set<BlockPos> hives = this.building.getHives();
        if (hives.isEmpty()) {
            this.worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable("entity.beekeeper.messagenohives"), ChatPriority.BLOCKING));
            this.setDelay(100);
            return AIWorkerState.DECIDE;
        }
        BlockPos hive = this.getHiveToHarvest();
        if (hive != null) {
            return AIWorkerState.BEEKEEPER_HARVEST;
        } else {
            return AIWorkerState.DECIDE;
        }
    }

    /**
     * @author Ckeeze
     * @reason rewriting harvest state
     */
    @Overwrite(remap = false)
    private IAIState harvestHoney() {

        if (this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honeycomb")
                || this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.both")) {
            if (!this.equipTool(InteractionHand.MAIN_HAND, TFCEquipmentTypes.tfcknife.get())) {
                return AIWorkerState.PREPARING;
            }
        } else if (!this.equipItem(InteractionHand.MAIN_HAND, new ItemStack(TFCItems.EMPTY_JAR.get()))) {
            return AIWorkerState.PREPARING;
        }
        BlockPos hive = this.getHiveToHarvest();
        if (hive == null) {
            return AIWorkerState.DECIDE;
        } else if (!this.walkToWorkPos(hive)) {
            return this.getState();
        } else {
            this.worker.swing(InteractionHand.MAIN_HAND);
            ItemStack itemStack = this.worker.getMainHandItem();
            //Wax
            if (!this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honey") && TFCEquipmentTypes.tfcknife.get().checkIsEquipment(itemStack)) {
                CitizenItemUtils.damageItemInHand(this.worker, InteractionHand.MAIN_HAND, 1);

                terrafirmacolonies$killBee(hive);

                ItemStack waxStack = new ItemStack(FLItems.BEESWAX.get(), 1);
                StatsUtil.trackStatByStack(this.building, "items_collected", waxStack, 1);
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(waxStack, this.worker.getItemHandlerCitizen());
                this.worker.getCitizenExperienceHandler().addExperience(5.0F);
            }
            //Honey
            else if (!this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honeycomb") && itemStack.getItem() == TFCItems.EMPTY_JAR.get()) {
                int honeyamount = terrafirmacolonies$getHoneyLevel(hive);
                terrafirmacolonies$takeHoneyFromHive(hive, honeyamount);
                ItemStack honeyStack = new ItemStack(FLItems.RAW_HONEY.get(), honeyamount);
                StatsUtil.trackStatByStack(this.building, "items_collected", honeyStack, honeyamount);
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(honeyStack, this.worker.getItemHandlerCitizen());
                this.worker.getCitizenExperienceHandler().addExperience(5.0F);
            }

            this.incrementActionsDoneAndDecSaturation();
            return AIWorkerState.START_WORKING;
        }
    }

    /**
     * @author Ckeeze
     * @reason getting TFC hives
     */
    @Overwrite
    private BlockPos getHiveToHarvest() {
        for (BlockPos pos : this.building.getHives()) {
            BlockEntity hive = this.world.getBlockEntity(pos);
            if (hive instanceof FLBeehiveBlockEntity) {
                if (((FLBeehiveBlockEntity) hive).getHoney() > 0 && this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honey")) {
                    return pos;
                }
                IBee[] bees = ((FLBeehiveBlockEntity) hive).getCachedBees();
                if (Arrays.stream(bees).allMatch(Objects::nonNull) && this.building.getHarvestTypes().equals("com.minecolonies.core.apiary.setting.honeycomb")) {
                    if (bees[0].hasQueen() && bees[1].hasQueen() && bees[2].hasQueen() && bees[3].hasQueen()) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    // Uniques
    @Unique
    private void terrafirmacolonies$killBee(BlockPos pos) {
        BlockEntity flhive = this.world.getBlockEntity(pos);
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) this.worker.level());
        if (flhive instanceof FLBeehiveBlockEntity) {
            FLHelpers.consumeInventory(this.world, pos, FLBlockEntities.BEEHIVE, (hive, inv) -> FLHelpers.takeOneAny(this.world, 0, 3, inv, fakePlayer));
            FLHelpers.consumeInventory(this.world, pos, FLBlockEntities.BEEHIVE, (hive, inv) -> FLHelpers.insertOneAny(this.world, FLItems.BEEHIVE_FRAME.get().getDefaultInstance(), 0, 3, inv, fakePlayer));
            ((FLBeehiveBlockEntity) flhive).setAndUpdateSlots(0);
        }
    }

    @Unique
    private int terrafirmacolonies$getHoneyLevel(BlockPos pos) {
        BlockEntity hive = this.world.getBlockEntity(pos);
        if (hive instanceof FLBeehiveBlockEntity) {
            return ((FLBeehiveBlockEntity) hive).getHoney();
        }
        return 0;
    }

    @Unique
    private void terrafirmacolonies$takeHoneyFromHive(BlockPos pos, int amount) {
        BlockEntity hive = this.world.getBlockEntity(pos);
        if (hive instanceof FLBeehiveBlockEntity) {
            ((FLBeehiveBlockEntity) hive).takeHoney(amount);
            ((FLBeehiveBlockEntity) hive).updateState();
        }
    }

    //Shadows
    @Shadow
    public boolean equipTool(InteractionHand hand, EquipmentTypeEntry toolType) {
        return true;
    }

    @Shadow
    public boolean equipItem(InteractionHand hand, ItemStack itemStack) {
        return true;
    }
}
