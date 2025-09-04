package tonyhallett.demomod;

import net.minecraft.nbt.NbtCompound;

import java.nio.file.Path;

public record NbtFile(Path path, NbtCompound nbt) {
}
