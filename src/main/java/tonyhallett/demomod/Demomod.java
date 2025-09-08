package tonyhallett.demomod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class Demomod implements ModInitializer {
	public static final String MOD_ID = "demomod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        DemoNbt();
	}

    private void DemoNbt(){
        var structurePath = Path.of("C:\\Users\\tonyh\\Downloads\\1.21.8\\data\\minecraft\\structure\\trial_chambers");
        var outputPath  = Path.of("C:\\Users\\tonyh\\Downloads\\structureInfo.text");
        try {
            HopperStructureInfo.writeHopperStructureInfo(structurePath, outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}