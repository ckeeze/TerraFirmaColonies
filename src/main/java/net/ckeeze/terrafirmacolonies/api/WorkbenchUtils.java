package net.ckeeze.terrafirmacolonies.api;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class WorkbenchUtils {

    public static Optional<ItemStack> getCraftingResult(Level world, List<ItemStack> input) {
        Preconditions.checkArgument(input.size() == 9, "input should be 9 items");

        CraftingContainer inv = new CraftingContainer() {
            @Override
            public int getWidth() {
                return 3;
            }

            @Override
            public int getHeight() {
                return 3;
            }

            @Override
            public @NotNull List<ItemStack> getItems() {
                return input;
            }

            @Override
            public int getContainerSize() {
                return 9;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public @NotNull ItemStack getItem(int i) {
                return input.get(i);
            }

            @Override
            public @NotNull ItemStack removeItem(int i, int i1) {
                return input.get(i);
            }

            @Override
            public @NotNull ItemStack removeItemNoUpdate(int i) {
                return input.get(i);
            }

            @Override
            public void setItem(int i, @NotNull ItemStack itemStack) {
            }

            @Override
            public void setChanged() {
            }

            @Override
            public boolean stillValid(@NotNull Player player) {
                return true;
            }

            @Override
            public void clearContent() {

            }

            @Override
            public void fillStackedContents(@NotNull StackedContents stackedContents) {

            }
        };
        return world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, inv, world)
                .map(recipe -> recipe.getResultItem(world.registryAccess()))
                .filter(slab -> !slab.isEmpty());
    }
}
