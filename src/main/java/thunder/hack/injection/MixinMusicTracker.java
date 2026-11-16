package thunder.hack.injection;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.MenuMusic;

@Mixin(MusicTracker.class)
public class MixinMusicTracker {
    // в главном меню ванильная музыка не играет — вместо неё наш трек
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void tickHook(CallbackInfo ci) {
        if (MenuMusic.suppressVanilla(MinecraftClient.getInstance()))
            ci.cancel();
    }
}
