package tonyhallett.demomod;

import java.lang.reflect.Method;

import net.minecraft.test.TestContext;

import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class ServerGameTest implements CustomTestMethodInvoker {
    @SuppressWarnings({"unused"})
    @GameTest
    public void test(TestContext context) {
        // todo - replicate what have done with block based test
        context.complete();
    }

    @Override
    public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
        method.invoke(this, context);
    }
}