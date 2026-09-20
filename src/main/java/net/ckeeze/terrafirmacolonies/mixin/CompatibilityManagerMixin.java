package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.FLTags;
import com.minecolonies.api.compatibility.CompatibilityManager;
import com.minecolonies.api.crafting.ItemStorage;
import net.dries007.tfc.common.TFCTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = CompatibilityManager.class)
public abstract class CompatibilityManagerMixin {
    @Final
    @Shadow(remap = false)
    private Set<ItemStorage> fuel = new HashSet<>();

    /**
     * @author Ckeeze
     * @reason Looking for TFC fuels
     */
    @Overwrite(remap = false)
    private void discoverFuel(ItemStack stack) {
        if (stack.is(TFCTags.Items.FORGE_FUEL)) {
            this.fuel.add(new ItemStorage(stack));
        }
        if (stack.is(FLTags.Items.OVEN_FUEL)) {
            this.fuel.add(new ItemStorage(stack));
        }
    }
}
