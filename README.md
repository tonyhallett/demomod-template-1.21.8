# Resources

[View the source in the browser](https://mcsrc.dev/)
View different versions / snapshots
Package structure with search
Find all references
Go to definition / Peek definition
View inheritance hierarchy

[Fabric dev documentation](https://docs.fabricmc.net/develop/)
[Fabric modding - detailed](https://wiki.fabricmc.net/tutorial:start)

# Testing

**Note** that internal class names are provided by mappings and will not agree with the most recent deobfuscated Minecraft code.

You can test in game with block based testing, or you can test in code.

Block testing and function testing ( code ) are built into the game.
Function based ( server, no UI ) testing is hooked into by Fabric.
Fabric also provides client based testing, this would be necessary if you wanted to test screens, 
take screenshots or do pixel based comparison testing.

The documentation for Fabric testing also mentions unit testing, but it is [Game Tests](https://docs.fabricmc.net/develop/automatic-testing#game-tests) that will be discussed here.
At the top of the page is a dropdown for the Minecraft version being targeted.
**Note that the documentation is incorrect with respect to the server test context type**.

## Block or function

### Blocks and entities
Block testing is completely determined by a Minecraft structure whereas Fabric function tests are not.

Function tests will have a default empty 8*8*8 structure if you do not provide one.  
If you are not using a structure then the `TestContext` has methods for adding blocks and entities.

## Creating a structure

Structures need to be provided as files in the [NBT format](https://minecraft.wiki/w/NBT_format) normally but the StructureTemplateManagerMixin allows for SNBT string format.
If you use SNBT then the resources data directory structure is different.  

| Nbt   | Path                                                              |
|-------|-------------------------------------------------------------------|
| true  | resources/data/*namespace*/structure/*structurename*.nbt          |
| false | resources/data/*namespace*/gametest/structure/*structurename*.nbt |

Although these will be defined in src they will be read from build.

It does not matter which resources directory is used for game tests.
If you want your block based tests to be published then they need to go in main as gametest is not published.
Do not use SNBT for published block based tests as the fabric gametest api is not including in the fabric api jar.

Structure files have a specific [format](https://minecraft.wiki/w/Structure_file#NBT_structure).

I used the [Vscode nbt viewer extension](https://marketplace.visualstudio.com/items?itemName=Misodee.vscode-nbt&ssr=false#review-details) to obtain a structure as SNBT.

You can see the parsing of the nbt by following the code of the StructureTemplateManager.readTemplate.
The StructureTemplateManagerMixin converts to nbt with `NbtHelper.fromNbtProviderString(String string)`

Although it is possible to create the file manually it is simpler to create in game.

There are two methods of creating in game:

1. Using a structure block in save mode.
2. Using the command /test create *testname*

Option 2 is probably the better option, **it does have caveats though**.

Note that a Restone block in a structure does not work the same in a function test,
[see](#RedstoneBlock-in-structure).

### Creating blocks and entities in code

Fabric function tests have a `TestContext` parameter, for getting, setting and expecting the state of the server world.

See [Setting up tests](#Setting-up-tests) for the requirements.

Positions in these methods are **relative to the test structure**.

will create a block ( if air )
`public void setBlockState(BlockPos pos, BlockState state) {`
the default state
`public void setBlockState(BlockPos pos, Block block) {`
example usage
`context.setBlockState(somePos,  Blocks.CHEST)`

to get facing the correct direction - `setBlockFacing`



Entities

`public ItemEntity spawnItem(Item item, BlockPos pos) {`

`public <E extends Entity> E spawnEntity(EntityType<E> type, BlockPos pos) {` and entities

`public <E extends MobEntity> E spawnMob(EntityType<E> type, BlockPos pos) {`

**Player**
`public PlayerEntity createMockPlayer(GameMode gameMode) {`

## Test start, performing change

In a block based test you **must place** a `TestBlock` with mode `START`.  
In the inventory the different modes have a different presentation.
This will be triggered by the testing framework supplying 15 weak redstone power that can be used to perform change.

For a function test, TestBlock instances do nothing, if you need to perform change then you need to decide when.

Your function test method will be invoked once, for code to run later you need to describe the tick or ticks when it needs to be run.

There are two ways of doing this

At a specific tick

`public void runAtTick(long tick, Runnable runnable) {`

`public void waitAndRun(long ticks, Runnable runnable) {`

**This is what BlockBasedTestInstance uses to check TestBlock for failure, success and logging**
`public void runAtEveryTick(Runnable task) {` alias forEachRemainingTick

Otherwise TimedTaskRunners.

```java
public TimedTaskRunner createTimedTaskRunner() {
    return this.test.createTimedTaskRunner();
}


```

A TimedTaskRunner has a list of TimedTask that will be run in order that they were added.
TimedTaskRunner instances are run in the order they were created using `createTimedTaskRunner`;
A TimedTask can have a duration ( addFinalTask / addFinalTaskWithDuration) or not ( addInstantFinalTask).
Those tests with a duration will cause the test to fail if the Runnable ( @FunctionalInterface ) takes too many ticks
Tasks can be run silently, where any thrown GameTestException are swallowed, or reported where a thrown GameTestException fails the test.
Tasks are run silently on all ticks until the max ticks has been reached.  Tests stop ticking once the test has failed or completed successfully.
You can force the task to not run silently with `TimedTaskRunner.createAndAddReported`

This adds a task that is followed by a failing task.
**This is poorly named in the mappings**, [issue](https://github.com/FabricMC/yarn/issues/4402), in the deobfuscated source this is called **failIf**
Even failIf seems incorrect, a predicate parameter wrapped in a task would make more sense ?

Perhaps the argument should never throw ?
As is shown later the second task will run unless the first threw. 
With multiple TimedTaskRunner another could complete whilst this one is silently.

```java
public void addTask(Runnable task) {
    this.test.createTimedTaskRunner().createAndAdd(task).fail(() -> this.createError("test.error.fail"));
}

```

final tasks

There can only be one final task otherwise markFinalCause will throw.

Note that completeIfSuccessful adds a new task to run after the runnable that will mark the test as completed if there has been no failure.
Manually invoking the TestContext complete has the same effect.


```java
public void addFinalTask(Runnable runnable) {
    this.markFinalCause();
    this.test.createTimedTaskRunner().createAndAdd(0L, runnable).completeIfSuccessful();
}

public void addInstantFinalTask(Runnable runnable) {
    this.markFinalCause();
    this.test.createTimedTaskRunner().createAndAdd(runnable).completeIfSuccessful();
}

public void addFinalTaskWithDuration(int duration, Runnable runnable) {
    this.markFinalCause();
    this.test.createTimedTaskRunner().createAndAdd(duration, runnable).completeIfSuccessful();
}
```


How the test tick works - from GameTestState

Here we can see that if there has been a failure there is no need to complete.
**If there has been no exception the test will not have passed unless complete has been invoked.**

``` 
	public void tick(TestRunContext context) {
		if (!this.isCompleted()) {
            // code removed of no interest 
            
			if (this.exception != null) {
				this.complete();
			}

			if (this.tickedOnce || this.blockEntity.getBlockBox().streamChunkPos().allMatch(this.world::shouldTickTestAt)) {
				this.tickedOnce = true;
				this.tickTests();
				if (this.isCompleted()) {
					if (this.exception != null) {
						this.listeners.forEach(listener -> listener.onFailed(this, context));
					} else {
						this.listeners.forEach(listener -> listener.onPassed(this, context));
					}
				}
			}
		}
	}
```

The "runAtTick" runnables for that tick run first.  
If they throw the test has failed but timed task runners will still run.
If the max tick duration for the test has not been reached the timed tasks are run silently.
If the test does not complete in its time limit then timed tasks will run again and probably fail.

This demonstrates that timed tasks can run on each tick until they complete, or successive task completes from completeIfSuccessful.

```java
	private void tickTests() {
		this.tick++;
		if (this.tick >= 0) {
			if (!this.started) {
				this.start();
			}

			ObjectIterator<Entry<Runnable>> objectIterator = this.ticksByRunnables.object2LongEntrySet().iterator();

			while (objectIterator.hasNext()) {
				Entry<Runnable> entry = (Entry<Runnable>)objectIterator.next();
				if (entry.getLongValue() <= this.tick) {
					try {
						((Runnable)entry.getKey()).run();
					} catch (TestException var4) {
						this.fail(var4);
					} catch (Exception var5) {
						this.fail(new UnknownTestException(var5));
					}

					objectIterator.remove();
				}
			}

			if (this.tick > this.tickLimit) {
				if (this.timedTaskRunners.isEmpty()) {
					this.fail(new TickLimitExceededException(Text.translatable("test.error.timeout.no_result", this.instanceEntry.value().getMaxTicks())));
				} else {
					this.timedTaskRunners.forEach(runner -> runner.runReported(this.tick));
					if (this.exception == null) {
						this.fail(new TickLimitExceededException(Text.translatable("test.error.timeout.no_sequences_finished", this.instanceEntry.value().getMaxTicks())));
					}
				}
			} else {
				this.timedTaskRunners.forEach(runner -> runner.runSilently(this.tick));
			}
		}
	}
```

From TimedTaskRunner ( silent or reporting ) - Once a timed task runs without throwing it will not run again.
If a task throws further tasks will not run.
```java
	private void runTasks(int tick) {
		Iterator<TimedTask> iterator = this.tasks.iterator();

		while (iterator.hasNext()) {
			TimedTask timedTask = iterator.next();
			timedTask.task.run();
			iterator.remove();
			int i = tick - this.tick;
			int j = this.tick;
			this.tick = tick;
			if (timedTask.duration != null && timedTask.duration != i) {
				this.test.fail(new GameTestException(Text.translatable("test.error.sequence.invalid_tick", j + timedTask.duration), tick));
				break;
			}
		}
	}
```

If required TestContext also has `public long getTick() {`

"Change" methods, in addition to `setState`

`removeBlock` and `putAndRemoveRedstoneBlock`

`useBlock` 
will create a mock player entity if one not provided
overrides call to
```java
	public void useBlock(BlockPos pos, PlayerEntity player, BlockHitResult result) {
		BlockPos blockPos = this.getAbsolutePos(pos);
		BlockState blockState = this.getWorld().getBlockState(blockPos);
		Hand hand = Hand.MAIN_HAND;
		ActionResult actionResult = blockState.onUseWithItem(player.getStackInHand(hand), this.getWorld(), player, hand, result);
		if (!actionResult.isAccepted()) {
			if (!(actionResult instanceof ActionResult.PassToDefaultBlockAction) || !blockState.onUse(this.getWorld(), player, result).isAccepted()) {
				ItemUsageContext itemUsageContext = new ItemUsageContext(player, hand, result);
				player.getStackInHand(hand).useOnBlock(itemUsageContext);
			}
		}
	}
```

`toggleButton` / `toggleLever`

`setTime` / `useNightTime`

`setBiome`

for mob entities

`setEntityPos` / `startMovingTowards`

damage

`public void damage(Entity entity, DamageSource damageSource, float amount)`

`public void killEntity(Entity entity)`

`public void killAllEntities()`

`public void killAllEntities(Class<? extends Entity> entityClass)` e.g `CopperGolemEntity.class`

`public LivingEntity drown(LivingEntity entity)`

`public LivingEntity setHealthLow(LivingEntity entity)`

forcing ticks 

Invokes the method on the Block if it overrides
`public void forceRandomTick(BlockPos pos) {`
`public void forceScheduledTick(BlockPos pos) {`

Ice and snow is biome dependent.
Ice will be on the position below.
Snow only if it is raining.

`public void forceTickIceAndSnow(BlockPos pos) {`

all positions in the test structure
`public void forceTickIceAndSnow() {`

If these methods are not sufficient then there is `public ServerWorld getWorld()`
e.g `public void setWeather(int clearDuration, int rainDuration, boolean raining, boolean thundering) {`



### Assertions and test success

In a block based test success or failure is driven by triggering 
`TestBlock` blocks with mode `FAIL` or `SUCCESS` with redstone power.
**An accept block is required to be present**
As such it is simpler in code to assert compared to creating observing redstone components.
Success or failure will be shown in the UI with red or green beacons.

Function tests throw exceptions from the `TestContext`

You can create an exception with `createError`, although it is more likely use a method that would throw. 
 
Assertions

```java
public void assertTrue(boolean condition, Text message){}
public <N> void assertEquals(N expected, N value, Text message){}
public void assertFalse(boolean condition, Text message){}
```

The there are "expect" and "dontExpect" methods for blocks and entities at positions or areas.
Some of the methods can take the positions with both BlockPos and x,y,x.
These positions are **relative** to the test structure.

### For blocks 
expectations can be for the provided argument at the position

Presence - `public void expectBlock(Block block, BlockPos pos)`
State - `expectBlockState(BlockPos pos, BlockState state)`
Property ( a specific property in the block state)
e.g. Properties class POWERED boolean property.  ( There are Integer and Enum properties also )
`public <T extends Comparable<T>> void expectBlockProperty(BlockPos pos, Property<T> property, T value) (block state )`

Then there are predicate "check" methods where you are provided the Block, BlockState or Property ?
e.g `public void checkBlockState(BlockPos pos, Predicate<BlockState> predicate, Function<BlockState, Text> messageGetter)`

Additional block methods

state

`expectSameStates`

( getWeakRedstonePower ) 
`public void expectRedstonePower(BlockPos pos, Direction direction, IntPredicate powerPredicate, Supplier<Text> messageGetter)`


### For entities

#### General entities

These methods begin "expectEntity" or "expectEntity" and have the entity specified with either
EntityType<?>, e.g `EntityType.CHEST_MINECART`, or Entity if you already have one.  

The expectation is at a position or in an area.

There are also predicates that can be applied to an Entity - testEntity / testEntityProperty

You can also check their items or effect.

#### BlockEntity

The getter is an expectation as it will throw if not present

`public <T extends BlockEntity> T getBlockEntity(BlockPos pos, Class<T> clazz) {`
`public <T extends BlockEntity> void checkBlockEntity(BlockPos pos, Class<T> clazz, Predicate<T> predicate, Supplier<Text> messageGetter)`

LockableContainerBlockEntity

`public void expectEmptyContainer(BlockPos pos) {`
`public void expectContainerWithSingle(BlockPos pos, Item item) {`
`public void expectContainerWith(BlockPos pos, Item item) {`


#### Items

`expectItemsAt` / `expectItemAt` / `expectItem`
example usage
`context.expectItem(Items.MINECART)`

As well as
`expectEntityHoldingItem`
`expectEntityWithItem`

Some methods allow for specifying delay or as an instant final task ( end with "End")

### TestContext relative positioning

As already mentioned positions that you supply are relative to the test structure.

`forEachRelativePos`, as shown in the TestStructureBlockFinder earlier, 
will invoke the callback for each `BlockPos` in the test box.  
These are relative so can be passed to the TestContext methods that work with relative positions.

For conversion there is

`public BlockPos getAbsolutePos(BlockPos pos)` and `getRelativePos`

`public Vec3d getAbsolute(Vec3d pos)` and `getRelative`

`public Box getAbsolute(Box box)` and `getRelative`

for the test box itself
`getTestBox`
`getRotation` and `getDirection` is for how the test structure has been adjusted for the test.

# Setting up tests

Block based

Fabric code tests

1. Configure Fabric Loom - build.gradle

    ```gradle
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "example-mod-test-${project.name}"
            enableGameTests = true // Default is true
            enableClientGameTests = true // Default is true
            eula = true // By setting this to true, you agree to the Minecraft EULA.
        }
    }
    ```
2. Create directory structure, code, resources with a fabric.mod.json

    src/gametest/resources/fabric.mod.json
    
    ```json
    {
      "schemaVersion": 1,
      "id": "example-mod-test",
      "version": "1.0.0",
      "name": "Example mod",
      "icon": "assets/example-mod/icon.png",
      "environment": "*",
      "entrypoints": {
        "fabric-gametest": ["com.example.docs.ExampleModGameTest"],
        "fabric-client-gametest": ["com.example.docs.ExampleModClientGameTest"]
      }
    }
    ```
    Note that this fabric.mod.json expects a server game test at src/gametest/java/com/example/docs/ExampleModGameTest, 
    and a client game test at src/gametest/java/com/example/docs/ExampleModClientGameTest.

3. Create test classes
    
    It is the presence of the @GameTest method annotation, perhaps on a base class, that signifies that a method is a test method.
    See [here for details](#How-the-fabric-game-tests-work)
    
    This is sufficient, but you can also implement `CustomTestMethodInvoker` if you have a requirement to perform common setup or expectations.
    
    These minimal examples have to agree with the fabric-gametest entry point in the fabric.mod.json.
    
    The test signature is strict.
    
    ```java
    package tonyhallett.demomod;
    
    import net.fabricmc.fabric.api.gametest.v1.GameTest;
    import net.minecraft.test.TestContext;
    
    public class MinimalTest {
        @GameTest
        public void test(TestContext context){
    
        }
    }
    ```
    
    For CustomTestMethodInvoker, the test signature can be anything and have any access level.
    You have access to the reflected test method that you would normally invoke,
    or it could just provide data in an annotation.
    
    ```java
    package tonyhallett.demomod;
    
    import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
    import net.fabricmc.fabric.api.gametest.v1.GameTest;
    import net.minecraft.test.TestContext;
    
    import java.lang.reflect.Method;
    
    public class MinimalInvokerTest implements CustomTestMethodInvoker {
        @GameTest
        public void test(TestContext context){
    
        }
    
        @Override
        public void invokeTestMethod(TestContext context, Method method) throws ReflectiveOperationException {
            // do something with TestContext, perhaps conditionally with the reflected method
            method.invoke(this, context);
        }
    }
    ```
    
    I have used CustomTestMethodInvoker in [one of my tests](#RedstoneBlock-in-structure)
    
4. Add parameters to `@GameTest` if the [defaults](# GameTest-defaults) are not sufficient, e.g if using a structure.

## Running tests

Block based

todo - mention that they will also be run with function based unless set as manual

Function based

## Debugging tests

todo 

## The tests for this mod.

Although overkill there are block based as well as both client and server code tests.
By having all 3 understanding is gained on the processes and quirks involved. 

## Block based tests

## Code tests

## RedstoneBlock in structure

If when creating a structure, state change is triggered by the placement of RedstoneBlock this will not occur in a function test.

This base test class facilitates such a scenario, by finding a `RedstoneBlock` and replacing with another one. 


```java
public class BaseServerTest implements CustomTestMethodInvoker {

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
```

**Note** the requirement of creating a copy.

```java
public class TestStructureBlockFinder
{
    private final TestContext _context;

    public TestStructureBlockFinder(TestContext context){
        _context = context;
    }

    public List<BlockPos> findBlocksInTestStructure(Block block){
        List<BlockPos> results = new ArrayList<>();
        _context.forEachRelativePos(pos -> {
            var state = _context.getBlockState(pos);
            if (state.isOf(block)) {
                results.add(pos.mutableCopy());
            }
        });

        return results;
    }
}
```

# How the fabric game tests work

If you look in build / loom-cache / remapped_working there will be a jar containing "fabric-gametest-api".

If you view inside ( change jar to zip ).

fabric.mod.json has entry point for main - net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer

fabric-gametest-api-v1.mixins.json has 3 mixins

RegistryLoaderMixin
TestServerMixin - simple ensures isDedicated returns true
StructureTemplateManagerMixin - already described, facilitates SNBT.

The RegistryLoaderMixin is simple as it just hooks in to when test instances are required so that 
`FabricGameTestModInitializer.registerDynamicEntries(registriesList);` can register test instances.

So the main logic is inside FabricGameTestModInitializer.  Showing relevant code

The `TestAnnotationLocator` is key to registering 
test functions - Consumer<TestContext>
test instance 

```
public abstract class TestInstance {
	protected TestInstance(TestData<RegistryEntry<TestEnvironmentDefinition>> data) {
		this.data = data;
	}

	public abstract void start(TestContext context);
}
```


```java
public final class FabricGameTestModInitializer implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(FabricGameTestModInitializer.class);
	private static TestAnnotationLocator locator = new TestAnnotationLocator(FabricLoader.getInstance());

	@Override
	public void onInitialize() {
		if (!(FabricGameTestRunner.ENABLED || FabricLoader.getInstance().isDevelopmentEnvironment())) {
			// Don't try to load the tests if the game test runner is disabled or we are not in a development environment
			return;
		}

		for (TestAnnotationLocator.TestMethod testMethod : locator.getTestMethods()) {
			LOGGER.debug("Registering test method: {}", testMethod.identifier());
			Registry.register(Registries.TEST_FUNCTION, testMethod.identifier(), testMethod.testFunction());
		}
	}

	public static void registerDynamicEntries(List<RegistryLoader.Loader<?>> registriesList) {
		// Registry<TestInstance> testInstances ....
		// Registry<TestEnvironmentDefinition> testEnvironmentDefinitionRegistry

		for (TestAnnotationLocator.TestMethod testMethod : locator.getTestMethods()) {
			TestInstance testInstance = testMethod.testInstance(testEnvironmentDefinitionRegistry);
			Registry.register(testInstances, testMethod.identifier(), testInstance);
		}
	}
}

```

There are two TestInstance derivations, BlockBasedTestInstance and FunctionTestInstance

FunctionTestInstance start is simple in that it invokes the corresponding function that was registered in onInitialize
```
	public FunctionTestInstance(RegistryKey<Consumer<TestContext>> function, TestData<RegistryEntry<TestEnvironmentDefinition>> data) {
		super(data);
		this.function = function;
	}

	@Override
	public void start(TestContext context) {
		((Consumer)context.getWorld()
				.getRegistryManager()
				.getOptionalEntry(this.function)
				.map(RegistryEntry.Reference::value)
				.orElseThrow(() -> new IllegalStateException("Trying to access missing test function: " + this.function.getValue())))
			.accept(context);
	}
```

# TestAnnotationLocator.getTestMethods

my gametest / resources / fabric.mod.json - note the entry points
```json
{
"schemaVersion": 1,
"id": "mod-test",
"version": "1.0.0",
"name": "Mod tests",
"environment": "*",
"entrypoints": {
  "fabric-gametest": ["tonyhallett.demomod.ServerMinecartKillerTest", "tonyhallett.demomod.ServerHopperPipeTest"],
  "fabric-client-gametest": ["tonyhallett.demomod.GameTest"]
}
}
```
The logic is simple so probably no need to look at the code
1. For those participating - fabric.mod.json fabric-gametest entry point
2. Use reflection to find methods with the `@GameTest` annotation ( will check super)
3. Create a TestMethod for each

```java

final class TestAnnotationLocator {
	private static final String ENTRYPOINT_KEY = "fabric-gametest";
	private static final Logger LOGGER = LoggerFactory.getLogger(TestAnnotationLocator.class);

	private final FabricLoader fabricLoader;

	private List<TestMethod> testMethods = null;

	TestAnnotationLocator(FabricLoader fabricLoader) {
		this.fabricLoader = fabricLoader;
	}

	public List<TestMethod> getTestMethods() {
		if (testMethods != null) {
			return testMethods;
		}

		List<EntrypointContainer<Object>> entrypointContainers = fabricLoader
				.getEntrypointContainers(ENTRYPOINT_KEY, Object.class);

		return testMethods = entrypointContainers.stream()
				.flatMap(entrypoint -> findMagicMethods(entrypoint).stream())
				.toList();
	}

	private List<TestMethod> findMagicMethods(EntrypointContainer<Object> entrypoint) {
		Class<?> testClass = entrypoint.getEntrypoint().getClass();
		List<TestMethod> methods = new ArrayList<>();
		findMagicMethods(entrypoint, testClass, methods);

		if (methods.isEmpty()) {
			LOGGER.warn("No methods with the GameTest annotation were found in {}", testClass.getName());
		}

		return methods;
	}

	// Recursively find all methods with the GameTest annotation
	private void findMagicMethods(EntrypointContainer<Object> entrypoint, Class<?> testClass, List<TestMethod> methods) {
		for (Method method : testClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(GameTest.class)) {
				if (!CustomTestMethodInvoker.class.isAssignableFrom(testClass)) {
					// Only validate the test method when using the default reflection invoker
					validateMethod(method);
				}

				methods.add(new TestMethod(method, method.getAnnotation(GameTest.class), entrypoint));
			}
		}

		if (testClass.getSuperclass() != null) {
			findMagicMethods(entrypoint, testClass.getSuperclass(), methods);
		}
	}

	private void validateMethod(Method method) {
		List<String> issues = new ArrayList<>();

		if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != TestContext.class) {
			issues.add("must have a single parameter of type TestContext");
		}

		if (!Modifier.isPublic(method.getModifiers())) {
			issues.add("must be public");
		}

		if (Modifier.isStatic(method.getModifiers())) {
			issues.add("must not be static");
		}

		if (method.getReturnType() != void.class) {
			issues.add("must return void");
		}

		if (issues.isEmpty()) {
			return;
		}

		String methodName = method.getDeclaringClass().getName() + "#" + method.getName();
		throw new UnsupportedOperationException("Test method (%s) has the following issues: %s".formatted(methodName, String.join(", ", issues)));
	}
}

```
The `testFunction` will be what is invoked by `FunctionTestInstance start`
This will invoke the annotated method's containing class `CustomTestMethodInvoker invokeTestMethod` if it extends the interface
or just invoke the test method.  Both methods receive the `TestContext`.

The `testInstance` method creates the associated `FunctionTestInstance` with the necessary TestData that the
test infrastructure requires - all taken from the `@GameTest` annotation.  ( With the test environment from the registry)
```java
	public record TestMethod(Method method, GameTest gameTest, EntrypointContainer<Object> entrypoint) {
		Identifier identifier() {
			String name = camelToSnake(entrypoint.getEntrypoint().getClass().getSimpleName() + "_" + method.getName());
			return Identifier.of(entrypoint.getProvider().getMetadata().getId(), name);
		}

		Consumer<TestContext> testFunction() {
			return context -> {
				Object instance = entrypoint.getEntrypoint();

				try {
					if (instance instanceof CustomTestMethodInvoker customTestMethodInvoker) {
						customTestMethodInvoker.invokeTestMethod(context, method);
						return;
					}

					method.invoke(instance, context);
				} catch (InvocationTargetException e) {
					// Ensure that any GameTestException are propagated without wrapping
					if (e.getTargetException() instanceof RuntimeException runtimeException) {
						throw runtimeException;
					}

					throw new RuntimeException("Failed to invoke test method", e);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException("Failed to invoke test method", e);
				}
			};
		}

		TestData<RegistryEntry<TestEnvironmentDefinition>> testData(Registry<TestEnvironmentDefinition> testEnvironmentDefinitionRegistry) {
			RegistryEntry<TestEnvironmentDefinition> testEnvironment = testEnvironmentDefinitionRegistry.getOrThrow(RegistryKey.of(RegistryKeys.TEST_ENVIRONMENT, Identifier.of(gameTest.environment())));

			return new TestData<>(
					testEnvironment,
					Identifier.of(gameTest.structure()),
					gameTest.maxTicks(),
					gameTest.setupTicks(),
					gameTest.required(),
					gameTest.rotation(),
					gameTest.manualOnly(),
					gameTest.maxAttempts(),
					gameTest.requiredSuccesses(),
					gameTest.skyAccess()
			);
		}

		TestInstance testInstance(Registry<TestEnvironmentDefinition> testEnvironmentDefinitionRegistry) {
			return new FunctionTestInstance(
					RegistryKey.of(RegistryKeys.TEST_FUNCTION, identifier()),
					testData(testEnvironmentDefinitionRegistry)
			);
		}

		private static String camelToSnake(String input) {
			return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
		}
	}
```

The @GameTest annotation has defaults for everything

There is only one environment registered, the default "minecraft:default"
data / minecraft / test_environment / default.json

This applies no environments.

See the [wiki](https://minecraft.wiki/w/Test_environment_definition) for details.

```json
{
  "type": "minecraft:all_of",
  "definitions": []
}
```

Note the default structure - which is present in the fabric-gametest-api jar
data\fabric-gametest-api-v1\gametest\structure\empty.snbt

As already mentioned this is just 8x8x8 air.

The @GameTest annotation is a function based version of 
data / *namespacename* / [test_instance.json](https://minecraft.wiki/w/Test_instance_definition) as used by block based tests 

# GameTest defaults
```java
public @interface GameTest {
	/**
	 * A namespaced ID of an entry within the {@link net.minecraft.registry.RegistryKeys#TEST_ENVIRONMENT} registry.
	 */
	String environment() default "minecraft:default";

	/**
	 * A namespaced ID pointing to a structure resource in the {@code modid/gametest/structure/} directory.
	 *
	 * <p>Defaults to an 8x8 structure with no blocks.
	 */
	String structure() default "fabric-gametest-api-v1:empty";

	/**
	 * The maximum number of ticks the test is allowed to run for.
	 */
	int maxTicks() default 20;

	/**
	 * The number of ticks to wait before starting the test after placing the structure.
	 */
	int setupTicks() default 0;

	/**
	 * Whether the test is required to pass for the test suite to pass.
	 */
	boolean required() default true;

	/**
	 * The rotation of the structure when placed.
	 */
	BlockRotation rotation() default BlockRotation.NONE;

	/**
	 * When set the test must be run manually.
	 */
	boolean manualOnly() default false;

	/**
	 * The number of times the test should be re attempted if it fails.
	 */
	int maxAttempts() default 1;

	/**
	 * The number of times the test should be successfully ran before it is considered a success.
	 */
	int requiredSuccesses() default 1;

	/**
	 * Whether the test should have sky access. When {@code false} the test will be enclosed by barrier blocks.
	 */
	boolean skyAccess() default false;
}
```

This is how the JSON files in data / *namespacename* / test_environment
get associated with code that runs setup and teardown, with access to the ServerWorld.
If these are not sufficient you could define your own [codec](https://wiki.fabricmc.net/tutorial:codec).

```java
public interface TestEnvironmentDefinition {
	Codec<TestEnvironmentDefinition> CODEC = Registries.TEST_ENVIRONMENT_DEFINITION_TYPE.getCodec().dispatch(TestEnvironmentDefinition::getCodec, codec -> codec);
	Codec<RegistryEntry<TestEnvironmentDefinition>> ENTRY_CODEC = RegistryElementCodec.of(RegistryKeys.TEST_ENVIRONMENT, CODEC);

	static MapCodec<? extends TestEnvironmentDefinition> registerAndGetDefault(Registry<MapCodec<? extends TestEnvironmentDefinition>> registry) {
		Registry.register(registry, "all_of", TestEnvironmentDefinition.AllOf.CODEC);
		Registry.register(registry, "game_rules", TestEnvironmentDefinition.GameRules.CODEC);
		Registry.register(registry, "time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
		Registry.register(registry, "weather", TestEnvironmentDefinition.Weather.CODEC);
		return Registry.register(registry, "function", TestEnvironmentDefinition.Function.CODEC);
	}

	void setup(ServerWorld world);

	default void teardown(ServerWorld world) {
	}

	MapCodec<? extends TestEnvironmentDefinition> getCodec();

    public record AllOf(List<RegistryEntry<TestEnvironmentDefinition>> definitions) implements TestEnvironmentDefinition {...}
    public record Function(Optional<Identifier> setupFunction, Optional<Identifier> teardownFunction) implements TestEnvironmentDefinition {...}
    public record GameRules(
            List<TestEnvironmentDefinition.GameRules.RuleValue<Boolean, net.minecraft.world.GameRules.BooleanRule>> boolRules,
            List<TestEnvironmentDefinition.GameRules.RuleValue<Integer, net.minecraft.world.GameRules.IntRule>> intRules
    ) implements TestEnvironmentDefinition {...}
    public record TimeOfDay(int time) implements TestEnvironmentDefinition {...}
    public record Weather(TestEnvironmentDefinition.Weather.State weather) implements TestEnvironmentDefinition {...}

```








