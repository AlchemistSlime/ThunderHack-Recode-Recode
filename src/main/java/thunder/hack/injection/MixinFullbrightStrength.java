package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.Fullbright;

@Mixin(GameRenderer.class)
public class MixinFullbrightStrength {
    @Inject(method = "getNightVisionStrength", at = @At("HEAD"), cancellable = true, require = 0)
    private static void getNightVisionStrengthHook(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (ModuleManager.fullbright.isEnabled() && entity == MinecraftClient.getInstance().player)
            cir.setReturnValue(Math.max(Fullbright.minBright.getValue(), 0.05f));
    }
}
