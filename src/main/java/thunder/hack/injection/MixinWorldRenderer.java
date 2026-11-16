package thunder.hack.injection;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;

import java.awt.Color;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    protected abstract void renderEntity(Entity entity, double cx, double cy, double cz, float td, MatrixStack matrices, VertexConsumerProvider vcp);

    @Unique
    private boolean thunderhack$again;

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3, require = 0)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return ModuleManager.freeCam.isEnabled() || spectator;
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void renderWeatherHook(CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.noWeather.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEntity", at = @At("RETURN"), require = 0)
    private void shaderEntityReturn(Entity entity, double cx, double cy, double cz, float td, MatrixStack matrices, VertexConsumerProvider vcp, CallbackInfo ci) {
        if (thunderhack$again) return;

        // ESP Outline: сущность остаётся в мире и дополнительно рисуется в буфер своего цвета
        if (ModuleManager.esp.isEnabled() && ModuleManager.esp.isOutlineMode() && ModuleManager.esp.shouldRender(entity)
                && vcp instanceof VertexConsumerProvider.Immediate imm) {
            Color col = ModuleManager.esp.getEntityColor(entity);
            imm.draw();
            Managers.SHADER.begin(col.getRGB() & 0xFFFFFF | 0x1000000, col, !ModuleManager.esp.isThroughWalls());
            thunderhack$again = true;
            try {
                renderEntity(entity, cx, cy, cz, td, matrices, vcp);
                imm.draw();
            } finally {
                thunderhack$again = false;
                Managers.SHADER.end();
            }
        }
    }
}
