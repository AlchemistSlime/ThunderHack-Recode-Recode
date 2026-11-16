package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.injection.accesors.IClientPlayerEntity;
import thunder.hack.utility.render.Render2DEngine;

import static thunder.hack.features.modules.Module.mc;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer {
    // 1.21.4: entities are rendered through render states, so rotation spoofing is applied to the state
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"), require = 0)
    private void onUpdateState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (Module.fullNullCheck()) return;
        if (mc.player != null && entity == mc.player && mc.player.getControllingVehicle() == null && ClientSettings.renderRotations.getValue() && !ThunderHack.isFuturePresent()) {
            IClientPlayerEntity p = (IClientPlayerEntity) MinecraftClient.getInstance().player;
            state.bodyYaw = Render2DEngine.interpolateFloat(Managers.PLAYER.prevBodyYaw, Managers.PLAYER.bodyYaw, tickDelta);
            float headYaw = Render2DEngine.interpolateFloat(Managers.PLAYER.lastYaw, p.getLastYaw(), tickDelta);
            state.yawDegrees = net.minecraft.util.math.MathHelper.wrapDegrees(headYaw - state.bodyYaw);
            state.pitch = Render2DEngine.interpolateFloat(Managers.PLAYER.lastPitch, p.getLastPitch(), tickDelta);
        }
    }
}
