package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.ItemStackUtils;
import net.ckeeze.terrafirmacolonies.api.FoodContainerReturns;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ItemStackUtils.class)
public class ItemStackUtilsMixin {

    @Redirect(method = "consumeFood", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            remap = true), remap = false, require = 1)
    private static ItemStack terrafirmacolonies$returnContainerAfterEating(ItemStack stack, Level level,
            LivingEntity eater, ItemStack food, AbstractEntityCitizen citizen, Inventory playerInventory) {
        // Capture the bowl before the last portion becomes empty; consume first so the
        // former food slot is available for the returned container.
        ItemStack container = FoodContainerReturns.getContainer(stack);
        int countBefore = stack.getCount();
        ItemStack result = stack.finishUsingItem(level, eater);
        if (container.isEmpty() || stack.getCount() >= countBefore) {
            return result;
        }
        FoodContainerReturns.returnContainer(container, citizen, playerInventory);
        // We handled this TFC/Firmalife container. Do not let the native method also
        // process a container returned by finishUsingItem.
        return stack;
    }
}
