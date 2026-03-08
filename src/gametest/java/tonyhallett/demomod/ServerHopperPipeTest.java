package tonyhallett.demomod;

import net.minecraft.item.Item;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public class ServerHopperPipeTest extends ServerHopperTestBase
{
    public ServerHopperPipeTest(){
        this.isHopperPipe = true;
    }

    @Override
    protected void expectExtracts(TestContext context, BlockPos hopperPos, Item spawnItem) {
        context.expectEmptyContainer(hopperPos);
    }
}
