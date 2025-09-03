package tonyhallett.demomod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tonyhallett.demomod.common.ProvideBlockEntityType;

@Mixin(BlockEntity.class)
public abstract class ProvideBlockEntityTypeMixin {
    @Final
    @Mutable
    @Shadow
    private BlockEntityType<?> type;

    @Inject(method = "validateSupports", at = @At("HEAD"))
    private void injectProvideType(BlockState state, CallbackInfo ci) {
        if (this instanceof ProvideBlockEntityType provider) {
            BlockEntityType<?> provided = provider.provideBlockEntityType();
            if (provided != null) {
                this.type = provided;
            }
        }
    }
}

