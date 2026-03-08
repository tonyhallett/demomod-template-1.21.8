package tonyhallett.demomod;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public class ServerHopperCreator
{
    public static void createHopperBlock(TestContext context, BlockPos hopperPos, boolean isHopperPipe){
        var hopperBlock = isHopperPipe ? ModBlocks.HOPPER_PIPE_BLOCK : Blocks.HOPPER;
        context.setBlockState(hopperPos, hopperBlock);
    }

    public static void createHopperBlockWithItem(TestContext context, BlockPos hopperPos, boolean isHopperPipe, Item item){
        createHopperBlock(context, hopperPos, isHopperPipe);
        context.getBlockEntity(hopperPos, HopperBlockEntity.class).setStack(0, new ItemStack(item));
    }

    public static void createHopperBlockedFromAbove(TestContext context, BlockPos hopperPos, boolean isHopperPipe){
        createHopperBlock(context, hopperPos, isHopperPipe);
        context.setBlockState(hopperPos.up(), Blocks.CHEST);
    }
}
