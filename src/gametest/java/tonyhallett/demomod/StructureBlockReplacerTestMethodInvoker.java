package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Method;

public abstract class StructureBlockReplacerTestMethodInvoker implements CustomTestMethodInvoker {
    protected Block findBlock = Blocks.REDSTONE_BLOCK;
    protected Block replaceBlock = Blocks.REDSTONE_BLOCK;

    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        var testStructureBlockFinder = new TestStructureBlockFinder(context);
        var foundBlockPositions = testStructureBlockFinder.findBlocksInTestStructure(findBlock);
        if (!foundBlockPositions.isEmpty()){
            replace(context, foundBlockPositions.getFirst());
        }

        invoke(context, method);
    }

    protected void invoke(TestContext context, Method method) throws ReflectiveOperationException{
        method.invoke(this, context);
    }

    private void replace(TestContext context, BlockPos pos){
        context.setBlockState(pos, Blocks.AIR.getDefaultState());
        context.setBlockState(pos, replaceBlock.getDefaultState());
    }
}
