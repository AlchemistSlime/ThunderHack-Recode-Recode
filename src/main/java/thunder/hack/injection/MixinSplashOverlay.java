package thunder.hack.injection;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import thunder.hack.features.modules.client.ClientSettings;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

import static thunder.hack.features.modules.Module.mc;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
    @Final @Shadow private boolean reloading;
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime = -1L;
    @Shadow private long reloadStartTime = -1L;
    @Final @Shadow private ResourceReload reload;
    @Final @Shadow private Consumer<Optional<Throwable>> exceptionHandler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(ModuleManager.unHook.isEnabled() || !ClientSettings.customLoadingScreen.getValue())
            return;
        ci.cancel();
        renderCustom(context, mouseX, mouseY, delta);
    }

    public void renderCustom(DrawContext context, int mouseX, int mouseY, float delta) {
        int i = mc.getWindow().getScaledWidth();
        int j = mc.getWindow().getScaledHeight();
        long l = Util.getMeasuringTimeMs();
        if (reloading && reloadStartTime == -1L) {
            reloadStartTime = l;
        }

        float f = reloadCompleteTime > -1L ? (float) (l - reloadCompleteTime) / 1000.0F : -1.0F;
        float g = reloadStartTime > -1L ? (float) (l - reloadStartTime) / 500.0F : -1.0F;
        float h;
        int k;
        if (f >= 1.0F) {
            if (mc.currentScreen != null)
                mc.currentScreen.render(context, 0, 0, delta);

            k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
            thBackdrop(context, i, j, k / 255f, l);
            h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        } else if (reloading) {
            if (mc.currentScreen != null && g < 1.0F)
                mc.currentScreen.render(context, mouseX, mouseY, delta);

            k = MathHelper.ceil(MathHelper.clamp((double) g, 0.15, 1.0) * 255.0);
            thBackdrop(context, i, j, k / 255f, l);
            h = MathHelper.clamp(g, 0.0F, 1.0F);
        } else {
            k = new Color(6, 6, 8).getRGB();
            float m = (float) (k >> 16 & 255) / 255.0F;
            float n = (float) (k >> 8 & 255) / 255.0F;
            float o = (float) (k & 255) / 255.0F;
            GlStateManager._clearColor(m, n, o, 1.0F);
            GlStateManager._clear(16384);
            h = 1.0F;
            thBackdrop(context, i, j, 1f, l);
        }

        k = (int) ((double) context.getScaledWindowWidth() * 0.5);
        int p = (int) ((double) context.getScaledWindowHeight() * 0.5);

        thLogo(context, k, p, h, l);

        float t = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + t * 0.050000012F, 0.0F, 1.0F);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        if (f >= 2.0F) {
            mc.setOverlay(null);
        }

        if (reloadCompleteTime == -1L && reload.isComplete() && (!reloading || g >= 2.0F)) {
            try {
                reload.throwException();
                exceptionHandler.accept(Optional.empty());
            } catch (Throwable var23) {
                exceptionHandler.accept(Optional.of(var23));
            }

            reloadCompleteTime = Util.getMeasuringTimeMs();
            if (mc.currentScreen != null) {
                mc.currentScreen.init(mc, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
            }
        }
    }

    private static Color thAccent(int offset) {
        try {
            return thunder.hack.features.modules.client.HudEditor.getColor(offset);
        } catch (Throwable t) {
            return new Color(255, 0, 0);
        }
    }

    private static int thArgb(Color c, float a) {
        int al = (int) (Math.max(0f, Math.min(1f, a)) * 255f);
        return (al << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    /** Фон: тёмный градиент с оттенком темы, свечение и летающие частицы. */
    private void thBackdrop(DrawContext ctx, int w, int h, float a, long now) {
        float t = now / 1000f;
        Color a0 = thAccent(0), a1 = thAccent(90);
        Color dark = new Color(a0.getRed() / 10, a0.getGreen() / 10, a0.getBlue() / 10);
        ctx.fillGradient(0, 0, w, h, thArgb(new Color(6, 6, 8), a), thArgb(dark, a));

        int cx = w / 2, cy = h / 2 - 12;
        for (int i = 0; i < 14; i++) {
            float r = 20 + i * 11 + (float) Math.sin(t * 1.6f) * 4f;
            ctx.fill((int) (cx - r * 2.2f), (int) (cy - r * 0.55f), (int) (cx + r * 2.2f), (int) (cy + r * 0.55f),
                    thArgb(a0, 0.028f * (1f - i / 14f) * a));
        }

        for (int i = 0; i < 46; i++) {
            float sp = 0.03f + (i % 7) * 0.012f;
            float px = ((i * 97 % 100) / 100f + (float) Math.sin(t * 0.5f + i) * 0.015f) * w;
            float py = h - ((t * sp + (i * 31 % 100) / 100f) % 1f) * h;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 2f + i * 1.7f));
            int s = i % 3 == 0 ? 3 : 2;
            Color pc = i % 2 == 0 ? a0 : a1;
            ctx.fill((int) px - 1, (int) py - 1, (int) px + s + 1, (int) py + s + 1, thArgb(pc, 0.10f * tw * a));
            ctx.fill((int) px, (int) py, (int) px + s, (int) py + s, thArgb(pc, 0.75f * tw * a));
        }
    }

    /** Логотип-картинка (текст в этот момент ещё нельзя рисовать: шрифты не загружены) + полоса прогресса. */
    private void thLogo(DrawContext ctx, int cx, int cy, float a, long now) {
        Color a0 = thAccent(0), a1 = thAccent(90);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, a);
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, TextureStorage.thLogo, cx - 150, cy - 35, (float)(0), (float)(0), 300, 70, 300, 70);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
        Render2DEngine.addWindow(ctx.getMatrices(), cx - 150, cy - 35, cx - 150 + (300 * progress), cy + 35, 1f);
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, TextureStorage.thLogo, cx - 150, cy - 35, (float)(0), (float)(0), 300, 70, 300, 70);
        Render2DEngine.popWindow();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

        int bw = 220, bh = 4, bx = cx - bw / 2, by = cy + 50;
        float p = Math.max(0f, Math.min(1f, progress));
        ctx.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, thArgb(a0, 0.25f * a));
        ctx.fill(bx, by, bx + bw, by + bh, thArgb(new Color(14, 14, 18), 0.95f * a));
        int fw = (int) (bw * p);
        if (fw > 0) {
            ctx.fillGradient(bx, by, bx + fw, by + bh, thArgb(a0, a), thArgb(a1, a));
            ctx.fill(bx + fw - 2, by - 1, bx + fw + 1, by + bh + 1, thArgb(Color.WHITE, 0.55f * a));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }
}
