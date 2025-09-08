package tonyhallett.demomod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record StructureNbtExpanded(
 int dataVersion,
 BlockPos size,
 List<StructureBlockInfoX> palette, // single palette
 List<List<StructureBlockInfoX>> palettes, // multiple palettes
 List<StructureBlock> blocks
) {
     public static final MapCodec<StructureNbtExpanded> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
         Codec.INT.fieldOf("DataVersion").forGetter(StructureNbtExpanded::dataVersion),
         BlockPos.CODEC.fieldOf("size").forGetter(StructureNbtExpanded::size),
         StructureBlockInfoX.MAP_CODEC.codec().listOf().optionalFieldOf("palette", Collections.emptyList()).forGetter(StructureNbtExpanded::palette),
         StructureBlockInfoX.MAP_CODEC.codec().listOf().listOf().optionalFieldOf("palettes", Collections.emptyList()).forGetter(StructureNbtExpanded::palettes),
         StructureBlock.MAP_CODEC.codec().listOf().fieldOf("blocks").forGetter(StructureNbtExpanded::blocks)
     ).apply(instance, StructureNbtExpanded::new));

     // helper to unify palette / palettes
     public List<List<StructureBlockInfoX>> getPalettes() {
         if (!palettes.isEmpty()) return palettes;
         if (!palette.isEmpty()) return List.of(palette);
         return Collections.emptyList();
     }
 // --- Nested Records ---
 public record StructureBlock(int state, BlockPos pos, NbtCompound blockEntityNbt) {
     public static final MapCodec<StructureBlock> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT.fieldOf("state").forGetter(StructureBlock::state),
        BlockPos.CODEC.fieldOf("pos").forGetter(StructureBlock::pos),
        NbtCompound.CODEC.optionalFieldOf("blockEntityNbt").forGetter(
            sb -> Optional.ofNullable(sb.blockEntityNbt)
        )
     ).apply(inst, (state, pos, optNbt) -> new StructureBlock(state, pos, optNbt.orElse(null))));
 }


}

