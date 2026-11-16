package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ShaderManager;

@Mixin(OutlineVertexConsumerProvider.class)
public class MixinOutlineVertexConsumerProvider {
    // Перед тем как игра рисует свои контуры, рисуем наши (ESP Outline и Shaders для существ)
    @Inject(method = "draw", at = @At("HEAD"), require = 0)
    private void th$beforeVanillaOutlines(CallbackInfo ci) {
        if (ShaderManager.drawingExtra) return;
        if ((Object) this != MinecraftClient.getInstance().getBufferBuilders().getOutlineVertexConsumers()) return;
        Managers.SHADER.endEntities();
    }
}
