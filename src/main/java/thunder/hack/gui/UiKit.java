package thunder.hack.gui;

import net.minecraft.client.util.math.MatrixStack;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;

/**
 * Тонкая прослойка между новым интерфейсом и рендер-движком.
 * Весь новый GUI рисует только через эти методы, поэтому при портировании
 * на 1.21.11 достаточно переписать этот файл и Render2DEngine, а не каждое окно.
 */
public final class UiKit {
    private UiKit() {
    }

    public static final float PANEL_RADIUS = 5f;
    public static final float ROW_RADIUS = 3.5f;

    public static final Color SUCCESS = new Color(88, 214, 141);
    public static final Color INFO = new Color(93, 173, 255);
    public static final Color WARNING = new Color(255, 193, 79);
    public static final Color DANGER = new Color(255, 99, 99);

    /** Акцентный цвет темы. Смещение задаёт позицию в градиенте (0, 90, 180, 270). */
    public static Color accent(int offset) {
        return HudEditor.getColor(offset);
    }

    public static Color alpha(Color c, float a) {
        return Render2DEngine.applyOpacity(c, Math.clamp(a, 0f, 1f));
    }

    /** Основа панели: стекло, тонированное цветом темы. */
    public static void panel(MatrixStack ms, float x, float y, float w, float h, float radius) {
        glass(ms, x, y, w, h, radius, 1f);
    }

    /** Тёмный оттенок акцентного цвета (f = яркость 0..1). Меняется вместе с цветом темы. */
    public static Color tint(float f) {
        Color c = accent(0);
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), 255);
    }

    /**
     * Стеклянная плашка под цвет темы. a = 0..1 — прозрачность (для анимации появления).
     * В режиме Blurry — размытие с оттенком акцента, иначе обычная основа HUD.
     */
    public static void glass(MatrixStack ms, float x, float y, float w, float h, float radius, float a) {
        a = Math.clamp(a, 0f, 1f);
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Color c = Render2DEngine.interpolateColorC(Color.BLACK, tint(0.30f), a);
            float opacity = (float) Render2DEngine.interpolate(1f, HudEditor.blurOpacity.getValue(), a);
            Render2DEngine.drawRoundedBlur(ms, x, y, w, h, radius, c, HudEditor.blurStrength.getValue() * a, opacity);
        } else {
            Render2DEngine.drawHudBase(ms, x, y, w, h, radius, a);
        }
    }

    public static void round(MatrixStack ms, float x, float y, float w, float h, float radius, Color c) {
        Render2DEngine.drawRound(ms, x, y, w, h, radius, c);
    }

    public static void glow(MatrixStack ms, float x, float y, float w, float h, int blur, Color c) {
        Render2DEngine.drawBlurredShadow(ms, x, y, w, h, blur, c);
    }

    /** Скруглённая плашка в акцентном цвете (середина градиента темы), прозрачность 0..1. */
    public static void accentFill(MatrixStack ms, float x, float y, float w, float h, float radius, float opacity) {
        Color mid = Render2DEngine.interpolateColorC(accent(270), accent(90), 0.5f);
        Render2DEngine.drawRound(ms, x, y, w, h, radius, alpha(mid, opacity));
    }

    /** Тонкая горизонтальная акцентная линия. */
    public static void accentLine(MatrixStack ms, float x, float y, float w, float thickness, float opacity) {
        Render2DEngine.horizontalGradient(ms, x, y, x + w, y + thickness,
                alpha(accent(270), opacity), alpha(accent(90), opacity));
    }

    public static boolean hovered(double mx, double my, double x, double y, double w, double h) {
        return Render2DEngine.isHovered(mx, my, x, y, w, h);
    }

    /**
     * Фирменный фон ClickGUI: тонированный акцентом градиент, виньетка, сетка и плывущие световые пятна.
     * Нарисован только простыми прямоугольниками и тенями Render2DEngine.
     */
    public static void backdrop(MatrixStack ms, float w, float h) {
        backdrop(ms, w, h, 1f);
    }

    /** f = 0..1, общая «яркость» фона (для плавного появления). */
    public static void backdrop(MatrixStack ms, float w, float h, float f) {
        f = Math.clamp(f, 0f, 1f);
        double t = System.currentTimeMillis() / 1000.0;
        Color base = accent(0);
        Color tint = new Color((int) (base.getRed() * 0.22f), (int) (base.getGreen() * 0.22f), (int) (base.getBlue() * 0.22f), (int) (235 * f));

        // основа: почти чёрный сверху, тёмный акцентный низ
        Render2DEngine.verticalGradient(ms, 0, 0, w, h, new Color(4, 0, 3, (int) (225 * f)), tint);

        // световые пятна
        glow(ms, (float) (w * (0.18 + 0.06 * Math.sin(t * 0.35))), (float) (h * (0.30 + 0.08 * Math.cos(t * 0.27))), 140, 140, 60, alpha(accent(270), 0.55f * f));
        glow(ms, (float) (w * (0.74 + 0.07 * Math.cos(t * 0.31))), (float) (h * (0.58 + 0.07 * Math.sin(t * 0.22))), 170, 170, 70, alpha(accent(90), 0.45f * f));
        glow(ms, (float) (w * (0.46 + 0.10 * Math.sin(t * 0.18))), (float) (h * (0.86 + 0.04 * Math.cos(t * 0.40))), 220, 70, 50, alpha(accent(180), 0.40f * f));

        // сетка
        Color grid = new Color(255, 255, 255, (int) (9 * f));
        for (float gx = 0; gx < w; gx += 28f)
            Render2DEngine.drawRect(ms, gx, 0, 0.5f, h, grid);
        for (float gy = 0; gy < h; gy += 28f)
            Render2DEngine.drawRect(ms, 0, gy, w, 0.5f, grid);

        // виньетка по краям
        Color edge = new Color(0, 0, 0, (int) (190 * f)), none = new Color(0, 0, 0, 0);
        float ex = w * 0.22f, ey = h * 0.22f;
        Render2DEngine.horizontalGradient(ms, 0, 0, ex, h, edge, none);
        Render2DEngine.horizontalGradient(ms, w - ex, 0, w, h, none, edge);
        Render2DEngine.verticalGradient(ms, 0, 0, w, ey, edge, none);
        Render2DEngine.verticalGradient(ms, 0, h - ey, w, h, none, edge);

        // акцентные линии сверху и снизу
        accentLine(ms, 0, 0, w, 1.5f, 0.9f * f);
        accentLine(ms, 0, h - 1.5f, w, 1.5f, 0.9f * f);
    }

    /** Непрозрачный фон для главного меню (когда за ним нет мира). */
    public static void menuBackdrop(MatrixStack ms, float w, float h) {
        Render2DEngine.drawRect(ms, 0, 0, w, h, new Color(5, 0, 4, 255));
        backdrop(ms, w, h, 1f);
    }

    /** Плавное замедление к концу: 0..1 -> 0..1. */
    public static float easeOutCubic(float t) {
        t = Math.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }
}
