package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.BiConsumer;

public abstract class ServerHopperTestBase
{
    protected boolean isHopperPipe;

    @SuppressWarnings({"unused"})
    @net.fabricmc.fabric.api.gametest.v1.GameTest(maxTicks = 30)
    public void hopperPipeNoCollisionTest(TestContext context){
        hopperInventoryTest(
                context,
                true,
                (spawnItem, hopperPos) -> spawnInHopperInputAreaShape(context, hopperPos, spawnItem));
    }

    private static void spawnInHopperInputAreaShape(TestContext context, BlockPos hopperPos, Item spawnItem){
        /*
            public interface Hopper extends Inventory {
	            Box INPUT_AREA_SHAPE = (Box)Block.createColumnShape(16.0, 11.0, 32.0).getBoundingBoxes().get(0);
        */
        context.spawnItem(spawnItem, new Vec3d(hopperPos.getX() + 0.5,  hopperPos.getY() + 0.7, hopperPos.getZ() + 0.5));
    }

    @SuppressWarnings({"unused"})
    @net.fabricmc.fabric.api.gametest.v1.GameTest(maxTicks = 30)
    public void hopperPipeNoExtractTest(TestContext context){
        hopperInventoryTest(
                context,
                false,
                (spawnItem, hopperPos) -> context.spawnItem(spawnItem, hopperPos.up()));
    }

    private void hopperInventoryTest(TestContext context, boolean blockFromAbove, BiConsumer<Item, BlockPos> spawner){
        var spawnItem = Items.MINECART;
        var hopperPos = new BlockPos( 0,0,0);
        if (blockFromAbove)
        {
            ServerHopperCreator.createHopperBlockedFromAbove(context, hopperPos, isHopperPipe);
        }
        else
        {
            ServerHopperCreator.createHopperBlock(context, hopperPos, isHopperPipe);
        }
        spawner.accept(spawnItem, hopperPos);

        context.runAtTick(30, () -> {
            expectExtracts(context, hopperPos, spawnItem);
            context.complete();
        });
    }

    protected abstract void expectExtracts(TestContext context, BlockPos hopperPos, Item spawnItem);

    @GameTest
    @SuppressWarnings({"unused"})
    public void hopperPushes(TestContext context){
        var topHopperItem = Items.MINECART;
        var pushIntoPos = new BlockPos(0,0,0);
        createHoppersIntoInventory(context, topHopperItem, pushIntoPos);
        context.addInstantFinalTask(() ->
            context.expectContainerWithSingle(pushIntoPos, topHopperItem)
        );
    }

    private void createHoppersIntoInventory(TestContext context, Item topHopperItem, BlockPos pushIntoPos){
        context.setBlockState(pushIntoPos, Blocks.CHEST);
        ServerHopperCreator.createHopperBlock(context, pushIntoPos.up(),isHopperPipe);
        ServerHopperCreator.createHopperBlockWithItem(context, pushIntoPos.up(2),false, topHopperItem);
    }
}
