package tonyhallett.demomod;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.Items;
import net.minecraft.test.TestContext;

import java.lang.reflect.Method;

public class ServerMinecartKillerTest  implements CustomTestMethodInvoker {
    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }

    @SuppressWarnings({"unused"})
    @GameTest(structure = "demomod:minecart_killer_function_test", maxTicks = 100)

    public void minecartKillerTest(TestContext context) {
        var testBox = context.getTestBox();
        var world = context.getWorld();

        // minecart killer
        context.runAtTick(99, () -> context.expectItem(Items.MINECART));
    }


}
