package tonyhallett.demomod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCollisionHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MinecartCollisionKillerBlock extends Block {
    public MinecartCollisionKillerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Make a smaller block like a cactus (14x14 horizontal, full height)
        return Block.createCuboidShape(1, 0, 1, 15, 16, 15);
    }

    private static final RegistryKey<DamageType> damageType = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of(Demomod.MOD_ID,"minecart_killer")
    );

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, EntityCollisionHandler handler) {
        if (entity instanceof MinecartEntity && world instanceof ServerWorld serverWorld){
            var damageSource = new DamageSource(
                    world.getRegistryManager()
                            .getOrThrow(RegistryKeys.DAMAGE_TYPE)
                            .getEntry(damageType.getValue()).get()
            );

            entity.damage(serverWorld, damageSource, 10F);
        }
    }
}

