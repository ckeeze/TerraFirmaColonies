package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCowboy;
import com.minecolonies.core.colony.jobs.JobCowboy;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkCowboy;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
import net.dries007.tfc.common.entities.livestock.TFCAnimal;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EntityAIWorkCowboy.class, remap = false)
public abstract class EntityAIWorkCowboyMixin extends AbstractEntityAIHerder<JobCowboy, BuildingCowboy> {
    @Shadow
    private int stewCoolDown;
    @Shadow
    private static final VisibleCitizenStatus HERD_COW = null;

    public EntityAIWorkCowboyMixin(@NotNull JobCowboy job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason instead of an internal timer check if the animals actually have milk product ready to be collected, and mushroom milking is removed
     */
    @Override
    @Overwrite
    public IAIState decideWhatToDo() {
        IAIState result = super.decideWhatToDo();
        //Simplified the timer based branches
        if (this.building != null && this.building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToMilk() && result.equals(AIWorkerState.START_WORKING) && terrafirmacolonies$AnimalsCanBeMilked()) {
            return AIWorkerState.COWBOY_MILK;
        }
        return result;
    }

    /**
     * @author Ckeeze
     * @reason Get Wooden bucket
     */
    @Override
    @Overwrite(remap = false)
    public @NotNull List<ItemStorage> getExtraItemsNeeded() {
        List<ItemStorage> list = super.getExtraItemsNeeded();
        if (this.building != null && this.building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToMilk()) {
            //changed from milkingItemOutput
            list.add(new ItemStorage(TFCItems.WOODEN_BUCKET.get(), 8));
        }
        //Deleted mooshroom branch
        return list;
    }

    /**
     * @author Ckeeze
     * @reason Rewrite AIstate to milk TFC DairyAnimals
     */
    @Overwrite(remap = false)
    private IAIState milkCows() {
        this.worker.getCitizenData().setVisibleStatus(HERD_COW);
        //Changed inventory check
        if (!this.worker.getCitizenInventoryHandler().hasItemInInventory(TFCItems.WOODEN_BUCKET.get())) {
            if (InventoryUtils.hasBuildingEnoughElseCount(this.building, new ItemStorage(new ItemStack(TFCItems.WOODEN_BUCKET.get(), 1)), 1) <= 0 || !this.walkToBuilding()) {
                //Removed cooldown
                return AIWorkerState.DECIDE;
            }
            //Changed from getMilkInputItem()
            this.checkAndTransferFromHut(new ItemStack(TFCItems.WOODEN_BUCKET.get(), 1));
        }
        //Changed from a check that returns a non baby vanilla Cow, Goat or Mooshroom
        Animal dairyAnimal = this.searchForAnimals((a) -> a instanceof DairyAnimal && !a.isBaby()).stream()
                .filter(a -> ((DairyAnimal) a).hasProduct())
                .filter(a -> ((DairyAnimal) a).getFamiliarity() >= 0.2F)
                .findFirst().orElse(null);

        if (dairyAnimal == null) {
            this.stewCoolDown = 10;
            return AIWorkerState.DECIDE;

        }
        //Changed arguements
        //Older branch from minecolonies 1.1.170 utalizes FakePlayers, it is a preferable implementation for compatibility reasons
        else if (this.equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(new ItemStorage(TFCItems.WOODEN_BUCKET.get()))) && !this.walkingToAnimal(dairyAnimal)) {
            FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) this.worker.level());
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TFCItems.WOODEN_BUCKET.get()));
            if (dairyAnimal.mobInteract(fakePlayer, InteractionHand.MAIN_HAND).equals(InteractionResult.SUCCESS)) {
                if (InventoryUtils.addItemStackToItemHandler(this.worker.getInventoryCitizen(), fakePlayer.getMainHandItem())) {
                    this.building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).onMilked();
                    CitizenItemUtils.removeHeldItem(this.worker);
                    InventoryUtils.tryRemoveStackFromItemHandler(this.worker.getInventoryCitizen(), new ItemStack(TFCItems.WOODEN_BUCKET.get()));
                    this.worker.queueSound(SoundEvents.COW_MILK, dairyAnimal.blockPosition(), 10, 0, 0.9F, this.worker.getRandom().nextFloat());
                }
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            this.incrementActionsDone();
            this.worker.decreaseSaturationForContinuousAction();
            StatsUtil.trackStat(this.building, "milking_attempts", 1);
            this.worker.getCitizenExperienceHandler().addExperience(1.0F);
            return AIWorkerState.INVENTORY_FULL;
        } else {
            return this.getState();
        }
    }


    //Uniques
    @Unique
    private boolean terrafirmacolonies$AnimalsCanBeMilked() {
        Predicate<TFCAnimal> actualpredicate = (a) -> a instanceof DairyAnimal;
        List<? extends TFCAnimal> animals = WorldUtil.getEntitiesWithinBuilding(this.world, TFCAnimal.class, this.building, actualpredicate);
        for (TFCAnimal animal : animals) {
            if (animal instanceof DairyAnimal && animal.hasProduct() && animal.getFamiliarity() > 0.2F) {
                return true;
            }
        }
        return false;
    }
}
