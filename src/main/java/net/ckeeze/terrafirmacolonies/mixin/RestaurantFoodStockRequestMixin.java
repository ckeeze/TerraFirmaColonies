package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.buildings.ICommonBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import net.ckeeze.terrafirmacolonies.api.RestaurantFoodRequests;
import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

@Mixin(value = RestaurantMenuModule.class, remap = false)
public abstract class RestaurantFoodStockRequestMixin {
    @Shadow public abstract Set<ItemStorage> getMenu();

    private boolean terrafirmacolonies$isListedFood(ItemStack stack) {
        return FoodCapability.get(stack) != null
                && getMenu().stream().anyMatch(entry -> entry.getItem() == stack.getItem());
    }

    @ModifyArg(method = "onColonyTick", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/colony/requestsystem/requestable/MinimumStack;<init>(Lnet/minecraft/world/item/ItemStack;ZZLnet/minecraft/world/item/ItemStack;II)V"),
            index = 0, require = 1)
    private ItemStack terrafirmacolonies$markMenuRequest(ItemStack prototype) {
        return terrafirmacolonies$isListedFood(prototype)
                ? RestaurantFoodRequests.markPrototype(prototype) : prototype;
    }

    @Redirect(method = "getMatchingRequest", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/ItemStackUtils;compareItemStacksIgnoreStackSize(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Boolean;"), require = 1)
    private Boolean terrafirmacolonies$findExistingMenuRequest(ItemStack requested, ItemStack prototype) {
        if (RestaurantFoodRequests.isMenuRequest(requested)) {
            return requested.getItem() == prototype.getItem();
        }
        return ItemStackUtils.compareItemStacksIgnoreStackSize(requested, prototype);
    }

    @Redirect(method = "onColonyTick", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/InventoryUtils;hasBuildingEnoughElseCount(Lcom/minecolonies/api/colony/buildings/ICommonBuilding;Lcom/minecolonies/api/crafting/ItemStorage;I)I"), require = 2)
    private int terrafirmacolonies$countActualFood(ICommonBuilding building, ItemStorage prototype, int target) {
        if (terrafirmacolonies$isListedFood(prototype.getItemStack())) {
            return RestaurantFoodRequests.countStock(building, prototype.getItemStack(), target);
        }
        return InventoryUtils.hasBuildingEnoughElseCount(building, prototype, target);
    }

    // Courier retention must recognize the same usable dish variants as replenishment.
    // In particular, the menu prototype has no bowl tag while real soups/salads do.
    // Scope this to finished menu food; recipe inputs and other stock rules stay native.
    @Redirect(method = "lambda$alterItemsToBeKept$0", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/ItemStackUtils;compareItemStacksIgnoreStackSize(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;ZZ)Z"), require = 1)
    private static boolean terrafirmacolonies$keepUsableMenuFood(ItemStack actual, ItemStack prototype,
            boolean matchDamage, boolean matchNbt) {
        return FoodCapability.get(prototype) != null
                ? RestaurantFoodRequests.matches(prototype, actual)
                : ItemStackUtils.compareItemStacksIgnoreStackSize(actual, prototype, matchDamage, matchNbt);
    }
}
