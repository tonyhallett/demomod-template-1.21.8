package tonyhallett.demomod;

import net.minecraft.item.Item;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public class ServerHopperTest extends ServerHopperTestBase
{
    @Override
    protected void expectExtracts(TestContext context, BlockPos hopperPos, Item spawnItem) {
        context.expectContainerWithSingle(hopperPos, spawnItem);
    }
}
