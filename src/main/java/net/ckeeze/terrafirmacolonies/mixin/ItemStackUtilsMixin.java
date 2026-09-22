package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.items.FLItems;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.items.TFCItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemStackUtils.class)
public class ItemStackUtilsMixin {

    @Inject(
            method = "consumeFood",
            at = @At(value = "HEAD"),
            remap = false
    )
    private static void injectConsumeFood(ItemStack foodStack, AbstractEntityCitizen citizen, Inventory inventory, CallbackInfo ci) {
        ICitizenData citizenDataTFC = citizen.getCitizenData();
        ItemStack itemUseReturnTFC = null;
        if (foodStack.getDescriptionId().contains("pie")) {
            itemUseReturnTFC = new ItemStack(FLItems.PIE_PAN.get());
        } else if (foodStack.is(TFCTags.Items.UNSEALED_JARS)) {
            itemUseReturnTFC = new ItemStack(TFCItems.EMPTY_JAR.get());
        } else if (foodStack.getTag() != null) {
            if (foodStack.getTag().toString().contains("ceramic")) {
                itemUseReturnTFC = new ItemStack(TFCBlocks.CERAMIC_BOWL.get().asItem());
            } else if (foodStack.getTag().toString().contains("minecraft:bowl")) {
                itemUseReturnTFC = new ItemStack(Items.BOWL);
            }
        }
        if (itemUseReturnTFC != null && itemUseReturnTFC.getItem() != foodStack.getItem()) {
            if (!citizenDataTFC.getInventory().isFull() && (inventory == null || inventory.add(itemUseReturnTFC))) {
                InventoryUtils.addItemStackToItemHandler(citizenDataTFC.getInventory(), itemUseReturnTFC);
            } else {
                InventoryUtils.spawnItemStack(citizen.level(), citizen.getX(), citizen.getY(), citizen.getZ(), itemUseReturnTFC);
            }
        }
    }
}
