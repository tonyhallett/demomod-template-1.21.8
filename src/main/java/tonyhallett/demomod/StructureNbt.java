package tonyhallett.demomod;

import net.minecraft.nbt.NbtCompound;

public class StructureNbt{
    public static StructureNbt tryCreate(NbtCompound nbtCompound){
        var keys = nbtCompound.getKeys();
        if(!Palettes.hasKeys(keys)){
            return null;
        }
        return new StructureNbt(Palettes.Parse(nbtCompound));
    }

    private StructureNbt(Palettes palettes){
        this.palettes = palettes;
    }

    public final Palettes palettes;

}
