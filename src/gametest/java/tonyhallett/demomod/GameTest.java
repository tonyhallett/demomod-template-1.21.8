package tonyhallett.demomod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import java.text.MessageFormat;

@SuppressWarnings("UnstableApiUsage")
public class GameTest implements FabricClientGameTest {
    @SuppressWarnings({"unused"})
    private void demoAddingBlockRelativeToPlayer(ClientGameTestContext context, TestSingleplayerContext testSingleplayerContext){
        try {
            context.runOnClient(client -> {
                var world = client.world;
                if (world == null || client.player == null) {
                    throw new Exception("No world or player");
                }
                var playerPos = client.player.getBlockPos();
                world.setBlockState(playerPos.add(0, 0, 3), Blocks.CHEST.getDefaultState(), 3);
            });
            testSingleplayerContext.getClientWorld().waitForChunksRender();
        }
        catch(Exception e){
            //
        }
    }
    @Override
    public void runTest(ClientGameTestContext context) {
        // in try due to AutoClosable
        try (TestSingleplayerContext singlePlayer = context.worldBuilder().create()) {
            var playerPos = getPlayerPos(context);
            var templatePos = playerPos.pos.offset(playerPos.direction, 3);
            singlePlayer.getServer().runCommand(getPlaceTemplateAt(templatePos,"demomod:minecart_killer_function_test"));
            /*
                note that this is not sufficient
                singlePlayerContext.getClientWorld().waitForChunksRender();

                could wait for sufficient ticks - context.waitTicks
             */
            var itemEntityBox = new Box(templatePos).expand(20);
            context.waitFor(client -> {
                assert client.world != null;
                return client.world.hasEntities(EntityType.ITEM, itemEntityBox, entity -> entity.isAlive() && entity.getStack().isOf(Items.MINECART));
            });

            context.takeScreenshot("example-mod-singleplayer-test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private String getPlaceTemplateAt(BlockPos pos, String templateName){
        return MessageFormat.format("/place template {0} {1} {2} {3}", templateName, pos.getX(),pos.getY(),pos.getZ());
    }

    private PlayerPosition getPlayerPos(ClientGameTestContext context) throws Exception {
        return context.computeOnClient(client -> {
            var world = client.world;
            if (world == null || client.player == null) {
                throw new Exception("No world or player");
            }
            return new PlayerPosition(client.player.getFacing(), client.player.getBlockPos());
        });
    }

    record PlayerPosition(Direction direction, BlockPos pos){}
}