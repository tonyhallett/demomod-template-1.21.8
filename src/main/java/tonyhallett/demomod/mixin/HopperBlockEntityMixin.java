package tonyhallett.demomod.mixin;

import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tonyhallett.demomod.HopperPipeBlockEntity;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    @Inject(
            method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onExtract(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (hopper instanceof HopperPipeBlockEntity) {
            cir.setReturnValue(false); // prevent normal extract
        }
    }
}
