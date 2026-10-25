package net.ckeeze.terrafirmacolonies.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** NBT helpers for equipment that may not exist in an unfinished building. */
public final class BuildingEquipmentNbt {
    private BuildingEquipmentNbt() {
    }

    public static void writeOptionalPosition(CompoundTag compound, String key, @Nullable BlockPos pos) {
        if (pos == null) {
            compound.remove(key);
        } else {
            compound.put(key, NbtUtils.writeBlockPos(pos));
        }
    }

    @Nullable
    public static BlockPos readOptionalPosition(CompoundTag compound, String key) {
        if (!compound.contains(key, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag position = compound.getCompound(key);
        if (!position.contains("X", Tag.TAG_ANY_NUMERIC)
                || !position.contains("Y", Tag.TAG_ANY_NUMERIC)
                || !position.contains("Z", Tag.TAG_ANY_NUMERIC)) {
            return null;
        }
        return NbtUtils.readBlockPos(position);
    }
}
