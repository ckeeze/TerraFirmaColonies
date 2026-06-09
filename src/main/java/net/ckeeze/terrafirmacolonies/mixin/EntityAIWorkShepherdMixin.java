package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingShepherd;
import com.minecolonies.core.colony.jobs.JobShepherd;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkShepherd;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.common.entities.livestock.TFCAnimal;
import net.dries007.tfc.common.entities.livestock.TFCAnimalProperties;
import net.dries007.tfc.common.entities.livestock.WoolyAnimal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EntityAIWorkShepherd.class, remap = false)
public abstract class EntityAIWorkShepherdMixin extends AbstractEntityAIHerder<JobShepherd, BuildingShepherd> {

    public EntityAIWorkShepherdMixin(@NotNull JobShepherd job) {
        super(job);
    }

    /**
     * @author Ckeeze
     * @reason check for shearable TFC animals
     */
    @Override
    @Overwrite(remap = false)
    public IAIState decideWhatToDo() {
        IAIState result = super.decideWhatToDo();
        WoolyAnimal shearingSheep = this.terrafirmacolonies$findShearableAnimal();
        if (this.building.getSetting(BuildingShepherd.SHEARING).getValue() && result.equals(AIWorkerState.START_WORKING) && shearingSheep != null) {
            return AIWorkerState.SHEPHERD_SHEAR;
        } else {
            this.worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return result;
        }
    }

    /**
     * @author Ckeeze
     * @reason Increase butchering damage to improve worker Reliability
     */
    @Override
    @Overwrite(remap = false)
    public double getButcheringAttackDamage() {
        return 5.0F + ((double) this.getPrimarySkillLevel() / (double) 10.0F);
    }

    /**
     * @author Ckeeze
     * @reason Remove dye mechainc, change skill bonus, change to TFC animals
     */
    @Overwrite(remap = false)
    private IAIState shearSheep() {
        WoolyAnimal sheep = this.terrafirmacolonies$findShearableAnimal();
        if (sheep == null) {
            return AIWorkerState.DECIDE;
        } else if (!this.equipTool(InteractionHand.MAIN_HAND, net.ckeeze.terrafirmacolonies.api.ModEquipmentTypes.tfcshears.get())) {
            return AIWorkerState.PREPARING;
        } else {
            if (this.worker.getMainHandItem() != null) {
                if (this.walkingToAnimal(sheep)) {
                    return this.getState();
                }
                double skillmodifier = Math.min(1.0F, (double) this.getPrimarySkillLevel() / (double) 50.0F);

                this.worker.swing(InteractionHand.MAIN_HAND);
                List<ItemStack> items = new ArrayList<>();
                if (!this.world.isClientSide) {
                    sheep.addUses(1);
                    sheep.setProductsCooldown();
                    items.add(sheep.getWoolItem());
                    if (ColonyConstants.rand.nextDouble() < skillmodifier) {
                        items.add(sheep.getWoolItem());
                    }
                }
                sheep.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 1.0F);
                CitizenItemUtils.damageItemInHand(this.worker, InteractionHand.MAIN_HAND, 1);
                this.worker.getCitizenExperienceHandler().addExperience(0.5F);
                this.incrementActionsDoneAndDecSaturation();
                for (ItemStack item : items) {
                    this.building.getModule(BuildingModules.STATS_MODULE).incrementBy("item_obtained;" + item.getItem().getDescriptionId(), item.getCount());
                    InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(item, this.worker.getInventoryCitizen());
                }
            }
            return AIWorkerState.DECIDE;
        }
    }

    //Unique methods
    @Unique
    private @Nullable WoolyAnimal terrafirmacolonies$findShearableAnimal() {
        assert current_module != null;
        List<? extends TFCAnimal> animals = this.searchForAnimals(current_module::isCompatible);
        for (TFCAnimal animal : animals) {
            if (animal instanceof WoolyAnimal sheep
                    && animal.hasProduct()
                    && animal.getFamiliarity() > 0.3F
                    && !animal.getAgeType().equals(TFCAnimalProperties.Age.OLD)) {
                return sheep;
            }
        }
        return null;
    }

    @Override
    public List<? extends TFCAnimal> searchForAnimals(Predicate<Animal> predicate) {
        Predicate<TFCAnimal> actualpredicate = (a) -> a instanceof WoolyAnimal;
        return WorldUtil.getEntitiesWithinBuilding(this.world, TFCAnimal.class, this.building, actualpredicate);
    }

    /**
     * @author Ckeeze
     * @reason fix TFC shears not being accepted
     */
    @Override
    @Overwrite(remap = false)
    public @NotNull List<EquipmentTypeEntry> getExtraToolsNeeded() {
        List<EquipmentTypeEntry> toolsNeeded = super.getExtraToolsNeeded();
        if (this.building.getSetting(BuildingShepherd.SHEARING).getValue()) {
            toolsNeeded.add(net.ckeeze.terrafirmacolonies.api.ModEquipmentTypes.tfcshears.get());
        }
        return toolsNeeded;
    }
}
