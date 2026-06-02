package net.ckeeze.terrafirmacolonies.mixin;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.TFCEntities;
import net.dries007.tfc.common.entities.livestock.*;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mixin(value = AbstractEntityAIHerder.class, remap = false)
public abstract class AbstractEntityAIHerderMixin<J extends AbstractJob<?, J>, B extends AbstractBuilding> extends AbstractEntityAIInteract<J, B> {
    @Shadow
    protected @Nullable AnimalHerdingModule current_module;

    @Unique
    private List<Item> terrafirmacolonies$pigFood = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.PIG_FOOD).stream().toList();
    @Unique
    private List<Item> terrafirmacolonies$rabbitFood = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.RABBIT_FOOD).stream().toList();
    @Unique
    private List<Item> terrafirmacolonies$cowFood = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.COW_FOOD).stream().toList();
    @Unique
    private List<Item> terrafirmacolonies$chickenFood = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.CHICKEN_FOOD).stream().toList();
    @Unique
    private List<Item> terrafirmacolonies$sheepFood = Objects.requireNonNull(ForgeRegistries.ITEMS.tags()).getTag(TFCTags.Items.SHEEP_FOOD).stream().toList();

    public AbstractEntityAIHerderMixin(@NotNull J job) {
        super(job);
    }

    //Shadow methods
    @Shadow
    public @NotNull List<EquipmentTypeEntry> getExtraToolsNeeded() {
        List<EquipmentTypeEntry> toolsNeeded = new ArrayList<>();
        toolsNeeded.add(ModEquipmentTypes.axe.get());
        return toolsNeeded;
    }

    @Shadow
    public @NotNull List<ItemStorage> getExtraItemsNeeded() {
        return new ArrayList<>();
    }

    @Shadow
    public List<? extends ItemEntity> searchForItemsInArea() {
        return null;
    }

    @Shadow
    public boolean equipItem(InteractionHand hand, List<ItemStorage> itemStacks) {
        return false;
    }

    @Shadow
    public boolean walkingToAnimal(Animal animal) {
        return false;
    }

    //Overwritten methods

    /**
     * @author Ckeeze
     * @reason Adding proper breedItems to itemsNiceToHave
     */
    @Override
    @Overwrite(remap = false)
    protected @NotNull List<ItemStorage> itemsNiceToHave() {
        List<ItemStorage> list = super.itemsNiceToHave();
        List<Item> stacklist = terrafirmacolonies$getBreedingitemsInList();
        assert stacklist != null;
        for (Item item : stacklist) {
            list.add(new ItemStorage(item));
        }
        return list;
    }

    /**
     * @author Ckeeze
     * @reason Detect TFC Animals, Animalmodule predicate is completely overwritten
     */
    @Overwrite(remap = false)
    public List<? extends TFCAnimal> searchForAnimals(Predicate<Animal> predicate) {
        JobEntry job = Objects.requireNonNull(this.worker.getCitizenJobHandler().getColonyJob()).getJobRegistryEntry();
        Predicate<TFCAnimal> actualpredicate = (a) -> a instanceof Mammal;
        if (job == ModJobs.swineHerder.get()) {
            actualpredicate = (a) -> a.getType() == TFCEntities.PIG.get();
        } else if (job == ModJobs.rabbitHerder.get()) {
            actualpredicate = (a) -> a.getType() == TFCEntities.RABBIT.get();
        } else if (job == ModJobs.shepherd.get()) {
            actualpredicate = (a) -> a instanceof WoolyAnimal;
        } else if (job == ModJobs.cowboy.get()) {
            actualpredicate = (a) -> a instanceof DairyAnimal;
        } else if (job == ModJobs.chickenHerder.get()) {
            actualpredicate = (a) -> a instanceof OviparousAnimal;
        }
        return WorldUtil.getEntitiesWithinBuilding(this.world, TFCAnimal.class, this.building, actualpredicate);
    }

    /**
     * @author Ckeeze
     * @reason Detecting modded animals instead, removing separate feeding behavior
     */
    @Overwrite(remap = false)
    public IAIState decideWhatToDo() {
        this.worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        for (AnimalHerdingModule module : this.building.getModulesByType(AnimalHerdingModule.class)) {
            Objects.requireNonNull(module);
            List<? extends TFCAnimal> animals = this.searchForAnimals(module::isCompatible);
            if (!animals.isEmpty()) {
                this.current_module = module;
                int numOfFeedableAnimals = 0;

                for (TFCAnimal entity : animals) {
                    if (terrafirmacolonies$isFeedAble(entity)) {
                        ++numOfFeedableAnimals;
                    }
                }

                boolean hasBreedingItem = terrafirmacolonies$HasBreeditems();
                if (ColonyConstants.rand.nextDouble() < 0.2 && !this.searchForItemsInArea().isEmpty()) {
                    LOGGER.info("HERDER_PICKUP");
                    return AIWorkerState.HERDER_PICKUP;
                }

                if (ColonyConstants.rand.nextDouble() < this.chanceToButcher(animals)) {
                    LOGGER.info("HERDER_BUTCHER");
                    return AIWorkerState.HERDER_BUTCHER;
                }

                if (numOfFeedableAnimals >= 1 && hasBreedingItem) {
                    LOGGER.info("HERDER_FEED");
                    return AIWorkerState.HERDER_FEED;
                }
            }
        }

        return AIWorkerState.START_WORKING;
    }

    /**
     * @author Ckeeze
     * @reason Add Modded Animalfeeds
     */
    @Overwrite(remap = false)
    private IAIState prepareForHerding() {
        if (this.current_module != null) {
            for (EquipmentTypeEntry tool : this.getExtraToolsNeeded()) {
                if (this.checkForToolOrWeapon(tool)) {
                    return this.getState();
                }
            }

            int BreedItemInbuilding = InventoryUtils.hasBuildingEnoughElseCount(this.building, this::terrafirmacolonies$isBreedItem, 1);
            int BreedItemInInventory = InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), this::terrafirmacolonies$isBreedItem);
            if (this.building.getBuildingLevel() >= 1) {
                if (BreedItemInbuilding + BreedItemInInventory <= 0) {
                    if (!(this.building).hasWorkerOpenRequestsOfType(this.worker.getCitizenData().getId(), TypeToken.of(StackList.class))) {
                        List<ItemStack> breedItemStacks = new ArrayList<>();
                        for (Item item : Objects.requireNonNull(terrafirmacolonies$getBreedingitemsInList())) {
                            breedItemStacks.add(new ItemStack(item, 1));
                        }
                        this.worker.getCitizenData().createRequestAsync(new StackList(breedItemStacks, "com.minecolonies.coremod.request.stacklist", 8, 1));
                    }
                }
            }

            for (ItemStorage items : this.getExtraItemsNeeded()) {
                this.checkIfRequestForItemExistOrCreateAsync(items.getItemStack(), items.getAmount(), items.getAmount());
            }

        }
        return AIWorkerState.DECIDE;
    }

    /**
     * @author Ckeeze
     * @reason Add Take adult male and female population into account when butchering
     */
    @Overwrite(remap = false)
    public double chanceToButcher(List<? extends TFCAnimal> allAnimals) {
        int maxAnimals = this.building.getBuildingLevel() * 4;
        if (!this.building.getSetting(AbstractBuilding.BREEDING).getValue() && allAnimals.size() <= maxAnimals) {
            return 0.0F;
        } else {
            int grownUp = 0;
            int males = 0;
            int females = 0;

            for (TFCAnimal animalToButcher : allAnimals) {
                if (!animalToButcher.isBaby()) {
                    ++grownUp;
                    if (animalToButcher.getAgeType() == TFCAnimalProperties.Age.OLD) {
                        if (animalToButcher.getGender() == TFCAnimalProperties.Gender.FEMALE) {
                            ++females;
                        }
                        if (animalToButcher.getGender() == TFCAnimalProperties.Gender.MALE) {
                            ++males;
                        }
                    }
                }
            }

            if (grownUp <= 4 || males <= 2 || females <= 2) {
                return 0.0F;
            } else {
                return (double) 0.5F * (Math.pow(grownUp, 4.0F) / Math.pow(maxAnimals, 4.0F));
            }
        }
    }

    /**
     * @author Ckeeze
     * @reason Change feeding/breeding behavior
     */
    @Overwrite
    protected IAIState feedAnimal() {
        if (this.current_module == null) {
            return AIWorkerState.DECIDE;
        } else if (!this.equipItem(InteractionHand.MAIN_HAND, terrafirmacolonies$ConvertItemstoStacks(Objects.requireNonNull(terrafirmacolonies$getBreedingitemsInList())))) {
            return AIWorkerState.START_WORKING;
        } else {
            AnimalHerdingModule var10001 = this.current_module;
            Objects.requireNonNull(var10001);
            List<? extends TFCAnimal> animals = this.searchForAnimals(var10001::isCompatible);
            TFCAnimal toFeed = null;

            for (TFCAnimal animal : animals) {
                if (animal.isHungry()) {
                    if (animal.getAgeType() == TFCAnimalProperties.Age.CHILD && animal.getFamiliarity() < 0.9F) {
                        toFeed = animal;
                        break;
                    }
                    if (animal.getAgeType() == TFCAnimalProperties.Age.ADULT && (animal.isReadyToMate() || animal.getFamiliarity() < 0.9F)) {
                        toFeed = animal;
                        break;
                    }
                }
            }
            if (toFeed == null) {
                return AIWorkerState.DECIDE;
            } else if (!this.walkingToAnimal(toFeed)) {
                this.worker.swing(InteractionHand.MAIN_HAND);
                StatsUtil.trackStatByName(this.building, "item_used", this.worker.getMainHandItem().getItem().getDescriptionId(), 1);
                this.worker.getMainHandItem().shrink(1);
                this.worker.getCitizenExperienceHandler().addExperience(0.5F);
                this.worker.level().broadcastEntityEvent(toFeed, (byte) 18);

                toFeed.setFamiliarity(toFeed.getFamiliarity() + 0.06F);
                if (toFeed.isBaby() && this.getSecondarySkillLevel() >= 10) {
                    toFeed.setFamiliarity(toFeed.getFamiliarity() + 0.02F);
                }
                toFeed.setLastFed(Calendars.get(toFeed.level()).getTotalDays());
                toFeed.setLastFamiliarityDecay(Calendars.get(toFeed.level()).getTotalDays());
                toFeed.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                CitizenItemUtils.removeHeldItem(this.worker);
                return AIWorkerState.DECIDE;
            } else {
                this.worker.decreaseSaturationForContinuousAction();
                return this.getState();
            }
        }
    }

    //Unique methods
    @Unique
    private boolean terrafirmacolonies$isFeedAble(TFCAnimal animal) {

        return ((animal.isBaby() && animal.getFamiliarity() <= 0.9F) || animal.getFamiliarity() < animal.getAdultFamiliarityCap())
                && animal.getAgeType() != TFCAnimalProperties.Age.OLD
                && !animal.isFertilized()
                && animal.isHungry();
    }

    @Unique
    private boolean terrafirmacolonies$HasBreeditems() {
        int totalitems = 0;
        for (Item item : Objects.requireNonNull(terrafirmacolonies$getBreedingitemsInList())) {
            totalitems += InventoryUtils.getItemCountInItemHandler(this.worker.getInventoryCitizen(), item);
        }
        LOGGER.info("Entered if terrafirmacolonies$HasBreeditems() {}", totalitems);
        return totalitems > 0;
    }

    @Unique
    private List<Item> terrafirmacolonies$getBreedingitemsInList() {
        JobEntry job = Objects.requireNonNull(this.worker.getCitizenJobHandler().getColonyJob()).getJobRegistryEntry();
        if (job == ModJobs.swineHerder.get()) {
            return terrafirmacolonies$pigFood;
        } else if (job == ModJobs.rabbitHerder.get()) {
            return terrafirmacolonies$rabbitFood;
        } else if (job == ModJobs.shepherd.get()) {
            return terrafirmacolonies$sheepFood;
        } else if (job == ModJobs.cowboy.get()) {
            return terrafirmacolonies$cowFood;
        } else if (job == ModJobs.chickenHerder.get()) {
            return terrafirmacolonies$chickenFood;
        }
        return terrafirmacolonies$cowFood;
    }

    @Unique
    private List<ItemStorage> terrafirmacolonies$ConvertItemstoStacks(List<Item> Itemlist) {
        List<ItemStorage> ItemStorageList = new ArrayList<>();
        for (Item item : Itemlist) {
            ItemStorageList.add(new ItemStorage(item, 1));
        }
        return ItemStorageList;
    }

    @Unique
    private boolean terrafirmacolonies$isBreedItem(ItemStack stack) {
        return Objects.requireNonNull(terrafirmacolonies$getBreedingitemsInList()).contains(stack.getItem());
    }
}


