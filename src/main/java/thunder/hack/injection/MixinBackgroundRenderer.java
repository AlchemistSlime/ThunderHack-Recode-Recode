package thunder.hack.injection;

import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.Entity;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.modules.render.WorldTweaks;

@Mixin(BackgroundRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, Vector4f color, float viewDistance, boolean thickFog, float tickDelta, CallbackInfoReturnable<Fog> cir) {
        Fog fog = cir.getReturnValue();
        if (fog == null) return;

        float start = fog.start();
        float end = fog.end();
        float r = fog.red();
        float g = fog.green();
        float b = fog.blue();
        boolean changed = false;

        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fog.getValue()) {
            if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN) {
                start = viewDistance * 4;
                end = viewDistance * 4.25f;
                changed = true;
            }
        }

        if (ModuleManager.worldTweaks.isEnabled() && WorldTweaks.fogModify.getValue().isEnabled()) {
            start = WorldTweaks.fogStart.getValue();
            end = WorldTweaks.fogEnd.getValue();
            r = WorldTweaks.fogColor.getValue().getGlRed();
            g = WorldTweaks.fogColor.getValue().getGlGreen();
            b = WorldTweaks.fogColor.getValue().getGlBlue();
            changed = true;
        }

        if (changed)
            cir.setReturnValue(new Fog(start, end, fog.shape(), r, g, b, fog.alpha()));
    }

    @Inject(method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;", at = @At("HEAD"), cancellable = true)
    private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blindness.getValue()) info.setReturnValue(null);
    }
}
