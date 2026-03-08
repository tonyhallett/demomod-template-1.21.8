package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.BiConsumer;

public class ServerHopperTest
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
            if (isHopperPipe)
            {
                context.expectEmptyContainer(hopperPos);
            }
            else
            {
                context.expectContainerWithSingle(hopperPos, spawnItem);
            }
            context.complete();
        });
    }

    @GameTest
    @SuppressWarnings({"unused"})
    public void hopperPipePushes(TestContext context){
        var pushIntoInventory = createHopperPipeVerticalChain(context);
        context.addInstantFinalTask(() ->
            context.assertFalse(pushIntoInventory.isEmpty(), Text.literal("hopper should have pushed"))
        );
    }

    private Inventory createHopperPipeVerticalChain(TestContext context){
        var insertChestPos = new BlockPos(0,0,0);
        context.setBlockState(insertChestPos, Blocks.CHEST);
        var insertChestBlockEntity = context.getBlockEntity(insertChestPos, ChestBlockEntity.class);
        ServerHopperCreator.createHopperBlock(context, insertChestPos.up(),isHopperPipe);
        ServerHopperCreator.createHopperBlock(context, insertChestPos.up(2),false);
        var extractInventoryPos = insertChestPos.up(3);
        context.setBlockState(extractInventoryPos, Blocks.CHEST);
        var extractChestBlockEntity = context.getBlockEntity(extractInventoryPos, ChestBlockEntity.class);
        extractChestBlockEntity.setStack(0, new ItemStack(Items.MINECART));
        return insertChestBlockEntity;
    }

}
