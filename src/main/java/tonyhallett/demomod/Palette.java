package tonyhallett.demomod;

import net.minecraft.nbt.NbtList;

import java.util.List;
import java.util.Objects;

public class Palette {
    public final List<StructureBlockInfo> blocks;

    private Palette(List<StructureBlockInfo> blocks) {
        this.blocks = blocks;
    }

    public static Palette parse(NbtList nbtBlocks) {
        var structureBlockInfos = nbtBlocks.streamCompounds().map(nbtBlock -> {
            try {
                return StructureBlockInfo.parse(nbtBlock);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).toList();
        return new Palette(structureBlockInfos);
    }
}
