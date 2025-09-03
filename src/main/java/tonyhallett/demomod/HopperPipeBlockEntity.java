package tonyhallett.demomod;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import tonyhallett.demomod.common.ProvideBlockEntityType;

public class HopperPipeBlockEntity extends HopperBlockEntity implements ProvideBlockEntityType {
    public HopperPipeBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    protected Text getContainerName(){
        return Text.translatable("container." + Demomod.MOD_ID + ".hopper_pipe");
    }

    @Override
    public BlockEntityType<?> provideBlockEntityType() {
        return ModBlockEntities.HOPPER_PIPE_BLOCK_ENTITY;
    }
}

