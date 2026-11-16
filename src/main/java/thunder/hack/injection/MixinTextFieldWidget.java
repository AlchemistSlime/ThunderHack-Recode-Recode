package thunder.hack.injection;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.hack.features.modules.client.ClientSettings;
import thunder.hack.gui.UiKit;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;

/** Красит рамку текстовых полей (с фоном) в цвет темы. */
@Mixin(TextFieldWidget.class)
public abstract class MixinTextFieldWidget {
    @Shadow
    private boolean drawsBackground;

    @Inject(method = "renderWidget", at = @At("RETURN"), require = 0)
    private void thFrame(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ClientSettings.customButtons.getValue() || !drawsBackground) return;

        ClickableWidget w = (ClickableWidget) (Object) this;
        float x = w.getX(), y = w.getY(), width = w.getWidth(), height = w.getHeight();
        Color c = UiKit.alpha(UiKit.accent(0), w.isFocused() ? 1f : 0.65f);

        Render2DEngine.drawRect(context.getMatrices(), x, y, width, 1f, c);
        Render2DEngine.drawRect(context.getMatrices(), x, y + height - 1f, width, 1f, c);
        Render2DEngine.drawRect(context.getMatrices(), x, y, 1f, height, c);
        Render2DEngine.drawRect(context.getMatrices(), x + width - 1f, y, 1f, height, c);
    }
}
