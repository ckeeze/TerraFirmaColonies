package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import net.ckeeze.terrafirmacolonies.api.RestaurantFoodRequests;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ordinary resource, recipe and minimum-stock requests retain their original NBT matching. */
@Mixin(value = Stack.class, remap = false)
public abstract class RestaurantFoodRequestMatchMixin {
    @Shadow public abstract ItemStack getStack();

    @Inject(method = "matches", at = @At("HEAD"), cancellable = true, require = 1)
    private void terrafirmacolonies$matchMenuFood(ItemStack actual, CallbackInfoReturnable<Boolean> cir) {
        if (RestaurantFoodRequests.isMenuRequest(getStack())) {
            cir.setReturnValue(RestaurantFoodRequests.matches(getStack(), actual));
        }
    }
}
