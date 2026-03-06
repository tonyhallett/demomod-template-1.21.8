# Resources

[View the source in the browser](https://mcsrc.dev/)
View different versions / snapshots
Package structure with search
Find all references
Go to definition / Peek definition
View inheritance hierarchy

[Vscode nby viewer extension](https://marketplace.visualstudio.com/items?itemName=Misodee.vscode-nbt&ssr=false#review-details) - not looked at yet.

# Testing

To perform testing you can test in game with block based testing or you can test in code.

Block testing and function testing ( code ) are built into the game.
Function based ( server, no UI ) testing is hooked into by Fabric.
Fabric also provides client based testing, this would be necessary if you wanted to test screens, 
take screenshots or do pixel based comparison testing.

## Block or function

### Blocks and entities
Block testing is completely determined by a Minecraft structure whereas Fabric function tests are not.

Function tests will have a default empty 8*8*8 structure if you do not provide one.  
If you are not using a structure then the `TestContext` has methods for adding blocks and entities.

## Creating a structure

Structures are nbt files ( snbt too) - add link
and although it is possible to create the file manually it is simpler to create in game.

There are two methods of creating in game:

1. Using a structure block in save mode.
2. Using the command /test create *testname*

Option 2 is probably the better option, **it does have caveats though**.

### Creating blocks and entities in code

From the `TestContext`

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

For a function test, if you need to perform change then you need to decide when

Your function test method will be invoked once, for code to run later you need to describe the tick or ticks when it needs to be run.

There are two ways of doing this

At a specific tick

`public void runAtTick(long tick, Runnable runnable) {`
`public void waitAndRun(long ticks, Runnable runnable) {`
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
These tasks can be run silently where any thrown GameTestException are swallowed, or reported causing the test to marked as failed.
You can force the task to always fail with `TimedTaskRunner.createAndAddReported`

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

If required there is also `public long getTick() {`



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

## RedstoneBlock in structure does not work the same as placing in game

If state change is triggered by RedstoneBlock then this base test class that finds and replaces can use.
When the class containing @GameTest decorated methods implements CustomTestMethodInvoker the invokeTestMethod will be invoked.
The context can be used prior to invoking the reflected Method.

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
e.g Properties class POWERED boolean property.  ( There are Integer and Enum properties also )
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


## Initial conditions

todo Discuss the json and @GameTest


## Running tests

todo

## The test command

## How to run tests....

1. Block based
2. Code



## The tests for this mod.

Although overkill there are block based as well as both client and server code tests.
By having all 3 understanding is gained on the processes and quirks involved. 

## Block based tests

## Code tests

# The internal code


