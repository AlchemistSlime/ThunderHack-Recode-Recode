package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(LivingEntity.class)
public class MixinFullbrightEffect {
    // Fullbright: карта освещения 1.21.4 считается шейдером, поэтому используем её ночное зрение
    @Inject(method = "hasStatusEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void hasStatusEffectHook(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (effect == StatusEffects.NIGHT_VISION
                && ModuleManager.fullbright.isEnabled()
                && (Object) this == MinecraftClient.getInstance().player)
            cir.setReturnValue(true);
    }
}
