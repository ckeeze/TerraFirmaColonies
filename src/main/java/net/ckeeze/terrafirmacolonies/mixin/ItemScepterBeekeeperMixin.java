package net.ckeeze.terrafirmacolonies.mixin;

import com.eerussianguy.firmalife.common.blocks.FLBeehiveBlock;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.SoundUtils;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper;
import com.minecolonies.core.items.AbstractItemMinecolonies;
import com.minecolonies.core.items.ItemScepterBeekeeper;
import com.minecolonies.core.network.messages.client.colony.ColonyViewBuildingViewMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collection;

@Mixin(value = ItemScepterBeekeeper.class)
public abstract class ItemScepterBeekeeperMixin extends AbstractItemMinecolonies {

    public ItemScepterBeekeeperMixin(String name, Properties properties) {
        super(name, properties);
    }

    /**
     * @author Ckeeze
     * @reason TFC Hives, + slightly improved stability thanks to asserts
     */
    @Override
    @Overwrite(remap = false)
    public @NotNull InteractionResult useOn(UseOnContext useContext) {
        if (useContext.getLevel().isClientSide) {
            return InteractionResult.FAIL;
        } else {
            Player player = useContext.getPlayer();
            assert useContext.getPlayer() != null;
            ItemStack scepter = useContext.getPlayer().getItemInHand(useContext.getHand());
            CompoundTag compound = scepter.getOrCreateTag();
            IColony colony = IColonyManager.getInstance().getColonyByWorld(compound.getInt("id"), useContext.getLevel());
            BlockPos hutPos = BlockPosUtil.read(compound, "pos");
            assert colony != null;
            IBuilding hut = colony.getBuildingManager().getBuilding(hutPos);
            BuildingBeekeeper building = (BuildingBeekeeper) hut;
            if (useContext.getLevel().getBlockState(useContext.getClickedPos()).getBlock() instanceof FLBeehiveBlock) {
                Collection<BlockPos> positions = building.getHives();
                BlockPos pos = useContext.getClickedPos();
                if (positions.contains(pos)) {
                    MessageUtils.format("item.minecolonies.scepterbeekeeper.removehive").sendTo(useContext.getPlayer());
                    building.removeHive(pos);
                    SoundUtils.playSoundForPlayer((ServerPlayer) player, SoundEvents.NOTE_BLOCK_BELL.get(), 1.0F, 0.5F);
                    Network.getNetwork().sendToPlayer(new ColonyViewBuildingViewMessage(building), (ServerPlayer) player);
                } else {
                    if (positions.size() < building.getMaximumHives()) {
                        MessageUtils.format("item.minecolonies.scepterbeekeeper.addhive").sendTo(useContext.getPlayer());
                        building.addHive(pos);
                        assert player != null;
                        SoundUtils.playSuccessSound(player, player.blockPosition());
                        Network.getNetwork().sendToPlayer(new ColonyViewBuildingViewMessage(building), (ServerPlayer) player);
                    }

                    if (positions.size() >= building.getMaximumHives()) {
                        MessageUtils.format("item.minecolonies.scepterbeekeeper.maxhives").sendTo(useContext.getPlayer());
                        assert player != null;
                        player.getInventory().removeItemNoUpdate(player.getInventory().selected);
                    }
                }
            } else {
                assert player != null;
                player.getInventory().removeItemNoUpdate(player.getInventory().selected);
            }

            return super.useOn(useContext);
        }
    }
}
