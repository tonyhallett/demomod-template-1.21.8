package tonyhallett.demomod;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModDamageSources{
    private static DamageSource minecartKillerDamageSource;
    private static boolean checkedForMinecartKiller;
    public static DamageSource getMinecartKillerDamageSource(DamageSources damageSources){
        if (!checkedForMinecartKiller){

            minecartKillerDamageSource = damageSources.create(
                    RegistryKey.of(
                        RegistryKeys.DAMAGE_TYPE,
                        Identifier.of("demomod:minecart_killer")
                    )
            );
            checkedForMinecartKiller = true;
        }
        return minecartKillerDamageSource;
    }
}
