package tonyhallett.demomod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public record StructureBlockInfoX(String name, Optional<NbtCompound> properties) {
    public static final MapCodec<StructureBlockInfoX> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.fieldOf("Name").forGetter(StructureBlockInfoX::name),
            NbtCompound.CODEC.optionalFieldOf("BlockStateProperties").forGetter(StructureBlockInfoX::properties)
    ).apply(inst, StructureBlockInfoX::new));
}
