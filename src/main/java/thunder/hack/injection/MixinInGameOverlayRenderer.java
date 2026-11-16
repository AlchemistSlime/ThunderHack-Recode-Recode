package thunder.hack.injection;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;

@Mixin(InGameOverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    // 1.21.4: handler parameters omitted on purpose, so the hooks don't depend on exact signatures
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void renderFireOverlayHook(CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.fireOverlay.getValue())
            ci.cancel();
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void renderUnderwaterOverlayHook(CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.waterOverlay.getValue())
            ci.cancel();
    }

    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void renderInWallOverlayHook(CallbackInfo ci) {
        if (ModuleManager.noRender.isEnabled() && ModuleManager.noRender.blockOverlay.getValue())
            ci.cancel();
    }
}
