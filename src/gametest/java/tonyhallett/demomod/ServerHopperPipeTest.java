package tonyhallett.demomod;

import java.lang.reflect.Method;

import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/*
    It is not necessary to implement CustomTestMethodInvoker
    but allows common set up code on the TestContext
    can use reflection info on the Method to make decisions
 */
public class ServerHopperPipeTest implements CustomTestMethodInvoker {

    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }



    @SuppressWarnings({"unused"})
    @GameTest(maxTicks = 30)
    public void hopperPipeNoCollisionTest(TestContext context){
        var hopperExpectation = setupForCollision(context, false);
        var hopperPipeExpectation = setupForCollision(context, true);

        context.runAtTick(30, () -> {
            hopperExpectation.run();
            hopperPipeExpectation.run();
            context.complete();
        });
    }

    private Runnable setupForCollision(TestContext context,boolean isHopperPipe){
        var hopperPos = new BlockPos(isHopperPipe ? 0 : 3,0,0);
        HopperCreator.createHopperBlockedFromAbove(context, hopperPos, isHopperPipe);
        /*
            public interface Hopper extends Inventory {
	            Box INPUT_AREA_SHAPE = (Box)Block.createColumnShape(16.0, 11.0, 32.0).getBoundingBoxes().get(0);
        */
        context.spawnItem(Items.MINECART, new Vec3d(hopperPos.getX() + 0.5,  hopperPos.getY() + 0.7, hopperPos.getZ() + 0.5));
        return getHopperExpectation(context, isHopperPipe, hopperPos);
    }

    @SuppressWarnings({"unused"})
    @GameTest(maxTicks = 120)
    public void hopperPipeNoExtractTest(TestContext context){
        var hopperExpectation = setupForExtract(context, false);
        var hopperPipeExpectation = setupForExtract(context, true);

        context.runAtTick(30, () -> {
            hopperExpectation.run();
            hopperPipeExpectation.run();
            context.complete();
        });
    }

    private Runnable setupForExtract(TestContext context, boolean isHopperPipe){
        var hopperPos = new BlockPos(isHopperPipe ? 0 : 3,0,0);
        HopperCreator.createHopperBlock(context, hopperPos, isHopperPipe);
        context.spawnItem(Items.MINECART, hopperPos.up());
        return getHopperExpectation(context, isHopperPipe, hopperPos);
    }

    private Runnable getHopperExpectation(TestContext context, boolean isHopperPipe, BlockPos hopperPos){
        return () -> {
            if (isHopperPipe)
            {
                context.expectEmptyContainer(hopperPos);
            }
            else
            {
                context.expectContainerWithSingle(hopperPos,Items.MINECART);
            }
        };

    }


}

class HopperCreator
{
    public static void createHopperBlock(TestContext context, BlockPos hopperPos, boolean isHopperPipe){
        var hopperBlock = isHopperPipe ? ModBlocks.HOPPER_PIPE_BLOCK : Blocks.HOPPER;
        context.setBlockState(hopperPos, hopperBlock);
    }

    public static void createHopperBlockedFromAbove(TestContext context, BlockPos hopperPos, boolean isHopperPipe){
        createHopperBlock(context, hopperPos, isHopperPipe);
        context.setBlockState(hopperPos.up(), Blocks.CHEST);
    }
}