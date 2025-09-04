package tonyhallett.demomod;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class Palettes {
    public static final String PALETTE_KEY = "palette";
    public static final String PALETTES_KEY = "palettes";

    private final List<Palette> palettes;

    private Palettes(List<Palette> palettes) {
        this.palettes = palettes;
    }

    private Palettes(Palette palette) {
        this(List.of(palette));
    }

    public Stream<String> getBlockIds() {
        return palettes.stream()
                .flatMap(palette -> palette.blocks.stream().map(b -> b.blockId));
    }

    public static boolean hasKeys(Set<String> keys) {
        return keys.contains(Palettes.PALETTE_KEY) || keys.contains(Palettes.PALETTES_KEY);
    }

    public static Palettes Parse(NbtCompound nbtCompound) {
        if (nbtCompound.contains(Palettes.PALETTE_KEY)) {
            var blocks = nbtCompound.getListOrEmpty(Palettes.PALETTE_KEY);
            return new Palettes(Palette.parse(blocks));
        }

        // NbtList of NbtList
        var palettesNbtList = nbtCompound.getListOrEmpty(Palettes.PALETTES_KEY);
        var palettes = palettesNbtList.stream().map(blocksNbtElement -> Palette.parse((NbtList) blocksNbtElement)).toList();
        return new Palettes(palettes);
    }

}
