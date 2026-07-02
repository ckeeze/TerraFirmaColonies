package net.ckeeze.terrafirmacolonies.mixin;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenJobHandler;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.ckeeze.terrafirmacolonies.api.TorchLitUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityCitizen.class)
public abstract class EntityCitizenMixin extends AbstractEntityCitizen {

    @Shadow
    public abstract ICitizenJobHandler getCitizenJobHandler();

    public EntityCitizenMixin(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    @Unique
    private int litCooldown = 0;
    @Unique
    private BlockPos lastLit;

    @Inject(method = "aiStep", at = @At("RETURN"))
    public void addTorchLitLogic(CallbackInfo ci) {
        if (TorchLitUtils.litJobs.contains(getCitizenJobHandler().getColonyJob().getJobRegistryEntry().getKey())) {
            litCooldown++;
            if (litCooldown > 20) {
                litCooldown = 0;
                BlockPos current = blockPosition();
                if (lastLit == null || current.distSqr(lastLit) > 16 * 16) {
                    TorchLitUtils.litAround(this, this.level(), current);
                    lastLit = current;
                }
            }
        }
    }
}
