package tonyhallett.demomod;

import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.NbtReadView;
import net.minecraft.util.ErrorReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

class Dummy implements RegistryWrapper.WrapperLookup{

    @Override
    public Stream<RegistryKey<? extends Registry<?>>> streamAllRegistryKeys() {
        return Stream.empty();
    }

    @Override
    public <T> Optional<? extends RegistryWrapper.Impl<T>> getOptional(RegistryKey<? extends Registry<? extends T>> registryRef) {
        return Optional.empty();
    }
}

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
                    // how could have done with codecs
                    var nbtReadView = NbtReadView.create(ErrorReporter.EMPTY, new Dummy(), structureNbtCompound);
                    // deprecated
                    var optionalStructureNbtExpanded = nbtReadView.read(StructureNbtExpanded.MAP_CODEC);
                    if(optionalStructureNbtExpanded.isPresent()){
                        hasHopperBlock = optionalStructureNbtExpanded.get().getPalettes().stream().flatMap(Collection::stream)
                                .anyMatch(structureBlockInfo -> structureBlockInfo.name().contains("hopper"));
                    }
                    // instead of using deprecated - not the full object but what interested in
                    // know that it is palette and not palettes
                    var palette = nbtReadView.getTypedListView("palette",StructureBlockInfoX.MAP_CODEC.codec());
                    hasHopperBlock = palette.stream().anyMatch(structureBlockInfo -> structureBlockInfo.name().contains("hopper"));
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
