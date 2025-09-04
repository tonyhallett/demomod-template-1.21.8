package tonyhallett.demomod;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.visitor.NbtTextFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class HopperStructureInfo {
    public static void writeHopperStructureInfo(Path structurePath, Path outputPath) throws IOException {
        var structureNbtCompounds = GetStructureNbts(structurePath);
        var output = new StringBuilder();
        structureNbtCompounds.forEach(structureNbtFile -> {
            var structureNbtCompound = structureNbtFile.nbt();
            var structureNbt = StructureNbt.tryCreate(structureNbtCompound);
            if (structureNbt != null){
                var hasHopperBlock = structureNbt.palettes.getBlockIds().anyMatch(blockId -> blockId.contains("hopper"));
                if (hasHopperBlock){
                    var formatted = new NbtTextFormatter("  ").apply(structureNbtCompound);
                    output.append(structureNbtFile.path());
                    output.append("\n");
                    output.append("\n");
                    output.append(formatted.getString());
                    output.append("\n");
                    output.append("----------------");
                    output.append("\n");
                    output.append("\n");
                }
            }
        });

        try {
            Files.writeString(outputPath, output);
        } catch (IOException e) {
            // ignore
        }
    }

    private static Stream<NbtFile> GetStructureNbts(Path structurePath) throws IOException {
        return findNbtFiles(structurePath).map(path -> {
            try{
                return loadCompressedNbt(path);
            }catch(IOException e){
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull);
    }

    private static Stream<Path> findNbtFiles(Path root) throws IOException {
        return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".nbt"));

    }


    private static NbtFile loadCompressedNbt(Path path) throws IOException {
        var nbtCompound =  NbtIo.readCompressed(path, NbtSizeTracker.ofUnlimitedBytes());
        return new NbtFile(path, nbtCompound);
    }
}
