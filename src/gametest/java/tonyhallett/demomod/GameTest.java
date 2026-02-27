package tonyhallett.demomod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

public class GameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {

        try (TestSingleplayerContext singlePlayer = context.worldBuilder().create()) {
            AtomicReference<BlockPos> origin = new AtomicReference<>(new BlockPos(0, 0, 0));
            context.runOnClient(client -> {
                var world = client.world;
                if(world == null || client.player == null){
                    throw new Exception("No world or player");
                }
                var clientPos = client.player.getBlockPos();
                origin.set(clientPos.add(0, 0, 3));
                world.setBlockState(origin.get(), Blocks.CHEST.getDefaultState(),3);
            });

            singlePlayer.getClientWorld().waitForChunksRender();
            // can only be called from the client gametest thread
            context.takeScreenshot("example-mod-singleplayer-test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}