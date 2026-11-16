package thunder.hack.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
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
 * Перерисовывает кнопки Minecraft (обычные и переключаемые) в стиле ThunderHack.
 * Выключается настройкой ClientSettings -> CustomButtons.
 */
@Mixin(PressableWidget.class)
public abstract class MixinPressableWidget {
    @Unique
    private float thHover;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true, require = 0)
    private void thRenderWidget(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ClientSettings.customButtons.getValue()) return;
        if (((Object) this) instanceof CheckboxWidget) return;
        if (FontRenderers.sf_medium == null) return;

        ClickableWidget w = (ClickableWidget) (Object) this;
        float x = w.getX(), y = w.getY(), width = w.getWidth(), height = w.getHeight();
        float a = 1f;

        boolean hovered = w.isHovered() && w.active;
        thHover = fast(thHover, hovered ? 1f : 0f, 14f);

        UiKit.glass(context.getMatrices(), x, y, width, height, 5f, a);
        if (w.active) {
            UiKit.accentFill(context.getMatrices(), x, y, width, height, 5f, 0.35f * thHover * a);
            float line = (width - 12f) * thHover;
            UiKit.round(context.getMatrices(), x + (width - line) / 2f, y + height - 2f, line, 1f, 0.5f, UiKit.alpha(UiKit.accent(0), 0.9f * a));
        }

        float textAlpha = (w.active ? 0.75f + 0.25f * thHover : 0.35f) * a;
        FontRenderers.sf_medium.drawCenteredString(context.getMatrices(), w.getMessage().getString(), x + width / 2f, y + height / 2f - 3.5f,
                Render2DEngine.applyOpacity(-1, textAlpha));

        ci.cancel();
    }
}
