package thunder.hack.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;

/**
 * Музыка в главном меню: когда мы не в мире и громкость музыки не 0 —
 * ванильная музыка меню отключается (см. MixinMusicTracker) и играет наш трек.
 */
public class MenuMusic {
    private static final Identifier ID = Identifier.of("thunderhack", "menu_music");
    private static SoundInstance instance;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(MenuMusic::tick);
    }

    /** true — мы в меню (игра загружена, мира нет) и музыка включена в настройках. */
    public static boolean shouldPlay(MinecraftClient mc) {
        return mc != null && mc.world == null && mc.getOverlay() == null
                && mc.options.getSoundVolume(SoundCategory.MUSIC) > 0f
                && mc.options.getSoundVolume(SoundCategory.MASTER) > 0f;
    }

    /** Ванильную музыку в меню глушим сразу, даже пока идёт загрузочный экран. */
    public static boolean suppressVanilla(MinecraftClient mc) {
        return mc != null && mc.world == null && mc.options.getSoundVolume(SoundCategory.MUSIC) > 0f;
    }

    private static void tick(MinecraftClient mc) {
        if (suppressVanilla(mc))
            mc.getMusicTracker().stop();
        if (shouldPlay(mc)) {
            if (instance == null || !mc.getSoundManager().isPlaying(instance)) {
                instance = new MenuTrack();
                mc.getSoundManager().play(instance);
            }
        } else if (instance != null) {
            mc.getSoundManager().stop(instance);
            instance = null;
        }
    }

    private static class MenuTrack extends AbstractSoundInstance {
        MenuTrack() {
            super(ID, SoundCategory.MUSIC, SoundInstance.createRandom());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = 0.5f; // тише ванильной музыки; дальше масштабируется ползунком «Музыка»
            this.pitch = 1f;
        }
    }
}
