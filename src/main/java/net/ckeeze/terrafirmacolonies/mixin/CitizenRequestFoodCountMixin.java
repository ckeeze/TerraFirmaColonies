package net.ckeeze.terrafirmacolonies.mixin;

import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.Predicate;

/** Keep the transfer amount and the following request-completion amount in agreement. */
@Mixin(targets = "com.minecolonies.core.client.gui.citizen.RequestWindowCitizen$CitizenRequestTreeWindowModule", remap = false)
public abstract class CitizenRequestFoodCountMixin {
    @Redirect(method = "onFulfill", at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/InventoryUtils;getItemCountInItemHandler(Lnet/minecraftforge/items/IItemHandler;Ljava/util/function/Predicate;)I"), require = 1)
    private int terrafirmacolonies$countSelectedFood(IItemHandler inventory, Predicate<ItemStack> matchesRequest) {
        return FoodStackSafety.countSelectedRequestFood(inventory, matchesRequest);
    }
}
