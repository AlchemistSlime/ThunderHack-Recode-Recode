package thunder.hack.injection;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.core.manager.client.ShaderManager;
import thunder.hack.utility.render.IOutlineFbSwap;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IOutlineFbSwap {
    @Shadow
    private Framebuffer entityOutlineFramebuffer;

    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Unique
    private final List<Framebuffer> th$fbStack = new ArrayList<>();

    @Unique
    private final List<Handle<Framebuffer>> th$handleStack = new ArrayList<>();

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

    // ---- ESP Outline и Shaders для существ (схема Meteor)

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void th$renderHead(CallbackInfo ci) {
        Managers.SHADER.beginFrame();
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), require = 0)
    private void th$renderEntityHead(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        ShaderManager sm = Managers.SHADER;
        if (ShaderManager.drawingExtra || sm.isCustom(vertexConsumers)) return;

        if (sm.wantEsp(entity)) {
            java.awt.Color c = ModuleManager.esp.getEntityColor(entity);
            th$drawExtra(sm.espBuffer, c.getRed(), c.getGreen(), c.getBlue(), entity, cameraX, cameraY, cameraZ, tickDelta, matrices);
        }
        if (sm.wantShader(entity))
            th$drawExtra(sm.shaderBuffer, 255, 255, 255, entity, cameraX, cameraY, cameraZ, tickDelta, matrices);
    }

    @Unique
    private void th$drawExtra(ShaderManager.EntityBuffer buf, int r, int g, int b, Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices) {
        if (buf.provider == null || buf.fb == null) return;
        th$pushOutlineFb(buf.fb);
        ShaderManager.drawingExtra = true;
        try {
            buf.provider.setColor(r, g, b, 255);
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, buf.provider);
            buf.used = true;
        } finally {
            ShaderManager.drawingExtra = false;
            th$popOutlineFb();
        }
    }

    @Override
    public void th$pushOutlineFb(Framebuffer framebuffer) {
        th$fbStack.add(this.entityOutlineFramebuffer);
        this.entityOutlineFramebuffer = framebuffer;
        th$handleStack.add(this.framebufferSet.entityOutlineFramebuffer);
        this.framebufferSet.entityOutlineFramebuffer = () -> framebuffer;
    }

    @Override
    public void th$popOutlineFb() {
        if (th$fbStack.isEmpty()) return;
        this.entityOutlineFramebuffer = th$fbStack.remove(th$fbStack.size() - 1);
        this.framebufferSet.entityOutlineFramebuffer = th$handleStack.remove(th$handleStack.size() - 1);
    }
}
