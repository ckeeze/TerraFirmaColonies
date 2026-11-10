package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.core.network.messages.server.colony.citizen.TransferItemsToCitizenRequestMessage;
import net.ckeeze.terrafirmacolonies.api.FoodStackSafety;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Both the availability count and the removed source items must match the copied food. */
@Mixin(value = TransferItemsToCitizenRequestMessage.class, remap = false)
public abstract class CitizenRequestFoodTransferMixin {
    @Redirect(method = {"lambda$onExecute$0", "lambda$onExecute$1"}, at = @At(value = "INVOKE",
            target = "Lcom/minecolonies/api/util/ItemStackUtils;compareItemStacksIgnoreStackSize(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Boolean;"), require = 2)
    private Boolean terrafirmacolonies$matchActualFood(ItemStack selected, ItemStack candidate) {
        return ItemStackUtils.compareItemStacksIgnoreStackSize(selected, candidate)
                && FoodStackSafety.sameFood(selected, candidate);
    }
}
