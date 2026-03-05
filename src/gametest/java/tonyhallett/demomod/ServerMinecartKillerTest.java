package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;

public class ServerMinecartKillerTest extends BaseServerTest {
    @SuppressWarnings({"unused"})
    @GameTest(structure = "demomod:minecart_killer_function_test")
    public void minecartKillerTest(TestContext context) {
        context.addInstantFinalTask(() -> context.expectItem(Items.MINECART));
    }
}
