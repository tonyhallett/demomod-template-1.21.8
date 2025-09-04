package tonyhallett.demomod;

import net.minecraft.nbt.NbtCompound;

public class StructureBlockInfo {
    public final String blockId;
    private StructureBlockInfo(String blockId){
        this.blockId = blockId;
    }
    public static StructureBlockInfo parse(NbtCompound blockInfoCompound) throws Exception {
        var name = blockInfoCompound.getString("Name");
        if (name.isPresent()) {
            return new StructureBlockInfo(name.get());
        }
        throw new Exception("unexpected");
    }

}
