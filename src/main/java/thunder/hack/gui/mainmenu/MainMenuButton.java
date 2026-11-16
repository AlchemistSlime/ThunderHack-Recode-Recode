package thunder.hack.gui.mainmenu;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.gui.UiKit;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.utility.render.animation.AnimationUtility.fast;

public class MainMenuButton {
    private final float posX, posY, width, height;
    private final String name;
    private final Runnable action;
    private float hover;

    public MainMenuButton(float posX, float posY, float width, float height, @NotNull String name, Runnable action) {
        this.name = name;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.action = action;
    }

    public MainMenuButton(float posX, float posY, @NotNull String name, Runnable action, boolean isExit) {
        this(posX, posY, isExit ? 222f : 107f, 38f, name, action);
    }

    public MainMenuButton(float posX, float posY, @NotNull String name, Runnable action) {
        this(posX, posY, name, action, false);
    }

    public void onRender(DrawContext context, float mouseX, float mouseY) {
        onRender(context, mouseX, mouseY, 1f);
    }

    /** appear: 0..1 — прогресс появления (плавный выезд снизу с проявлением). */
    public void onRender(DrawContext context, float mouseX, float mouseY, float appear) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;

        float x = cx + posX;
        float y = cy + posY;

        boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, x, y, width, height);
        hover = fast(hover, hovered ? 1f : 0f, 14f);

        float drawY = y + (1f - appear) * 12f;
        float textAlpha = (0.65f + 0.35f * hover) * appear;

        UiKit.glass(context.getMatrices(), x, drawY, width, height, 8f, appear);
        UiKit.accentFill(context.getMatrices(), x, drawY, width, height, 8f, 0.38f * hover * appear);

        // акцентная полоска слева растёт при наведении
        float barH = 8f + 8f * hover;
        UiKit.round(context.getMatrices(), x + 7f, drawY + (height - barH) / 2f, 2f, barH, 1f, UiKit.alpha(UiKit.accent(0), (0.35f + 0.65f * hover) * appear));

        FontRenderers.monsterrat.drawString(context.getMatrices(), name, x + 17f + 4f * hover, drawY + height / 2f - 3f,
                Render2DEngine.applyOpacity(-1, textAlpha));

        // стрелка справа появляется при наведении
        FontRenderers.sf_bold.drawString(context.getMatrices(), ">", x + width - 15f - 3f * (1f - hover), drawY + height / 2f - 3f,
                Render2DEngine.applyOpacity(-1, 0.8f * hover * appear));
    }

    public void onClick(int mouseX, int mouseY) {
        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;
        if (Render2DEngine.isHovered(mouseX, mouseY, cx + posX, cy + posY, width, height))
            action.run();
    }
}
