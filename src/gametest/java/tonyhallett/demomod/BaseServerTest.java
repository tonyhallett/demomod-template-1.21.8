package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public class BaseServerTest  implements CustomTestMethodInvoker {

    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        var testStructureBlockFinder = new TestStructureBlockFinder(context);
        // could use other marker blocks instead such as a TestBlock
        var redstoneBlockPositions = testStructureBlockFinder.findBlocksInTestStructure(Blocks.REDSTONE_BLOCK);
        if (!redstoneBlockPositions.isEmpty()){
            replaceSame(context, redstoneBlockPositions.getFirst(), Blocks.REDSTONE_BLOCK);
        }

        method.invoke(this, context);
    }

    @SuppressWarnings("SameParameterValue")
    private static void replaceSame(TestContext context, BlockPos pos, Block block){
        context.setBlockState(pos, Blocks.AIR.getDefaultState());
        context.setBlockState(pos, block.getDefaultState());
    }
}
