package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.core.entity.other.NewBobberEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = NewBobberEntity.class, remap = false)
public abstract class NewBobberEntityMixin extends Projectile implements IEntityAdditionalSpawnData {

    protected NewBobberEntityMixin(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }
}
