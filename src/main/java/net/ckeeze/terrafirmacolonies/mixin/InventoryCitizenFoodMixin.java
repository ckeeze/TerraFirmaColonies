package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.inventory.InventoryCitizen;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InventoryCitizen.class, remap = false)
public abstract class InventoryCitizenFoodMixin {
    @Shadow public abstract ItemStack getStackInSlot(int slot);

    @Inject(method = "insertItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void terrafirmacolonies$preserveFood(int slot, ItemStack incoming, boolean simulate,
                                               CallbackInfoReturnable<ItemStack> cir) {
        ItemStack existing = getStackInSlot(slot);
        if (!existing.isEmpty() && !incoming.isEmpty() && !FoodStackSafety.sameFood(existing, incoming)) {
            // The caller may try another slot. Simulation must have exactly the same acceptance rule.
            cir.setReturnValue(incoming);
        }
    }
}
