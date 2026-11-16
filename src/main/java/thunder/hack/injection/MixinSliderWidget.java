package thunder.hack.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.gui.UiKit;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

import static thunder.hack.utility.render.animation.AnimationUtility.fast;

/**
 * Перерисовывает ползунки Minecraft в стиле ThunderHack.
 * Выключается настройкой ClientSettings -> CustomButtons.
 */
@Mixin(SliderWidget.class)
public abstract class MixinSliderWidget {
    @Shadow
    protected double value;

    @Unique
    private float thHover;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void thRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ClientSettings.customButtons.getValue()) return;
        if (FontRenderers.sf_medium == null) return;

        ClickableWidget w = (ClickableWidget) (Object) this;
        float x = w.getX(), y = w.getY(), width = w.getWidth(), height = w.getHeight();

        boolean hovered = w.isHovered() && w.active;
        thHover = fast(thHover, hovered ? 1f : 0f, 14f);

        float v = (float) Math.clamp(value, 0.0, 1.0);

        UiKit.glass(context.getMatrices(), x, y, width, height, 5f, 1f);

        float inner = width - 4f;
        float fill = Math.max(4f, inner * v);
        UiKit.accentFill(context.getMatrices(), x + 2f, y + 2f, fill, height - 4f, 4f, 0.45f + 0.2f * thHover);
        UiKit.round(context.getMatrices(), x + 2f + inner * v - 1f, y + 2f, 2f, height - 4f, 1f, UiKit.alpha(java.awt.Color.WHITE, 0.55f + 0.4f * thHover));

        float textAlpha = w.active ? 0.8f + 0.2f * thHover : 0.35f;
        FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), w.getMessage().getString(), x + width / 2f, y + height / 2f - 3.5f,
                Render2DEngine.applyOpacity(-1, textAlpha));

        ci.cancel();
    }
}
