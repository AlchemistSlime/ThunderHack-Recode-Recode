package thunder.hack.gui.notification;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.UiKit;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.Timer;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.EaseOutBack;

import java.awt.*;

import static thunder.hack.core.manager.client.NotificationManager.isDefault;
import static thunder.hack.features.modules.Module.mc;

public class Notification {
    private final String message, title;
    private final String icon;
    private final int lifeTime;
    public final EaseOutBack animation;
    private float y, width, animationX, height = 25;
    private boolean direction = false;
    private final Timer timer = new Timer();
    private final long created = System.currentTimeMillis();
    private final Color typeColor;

    public Notification(String title, String message, Type type, int time) {
        lifeTime = time;
        typeColor = switch (type) {
            case SUCCESS, ENABLED -> UiKit.SUCCESS;
            case INFO -> UiKit.INFO;
            case WARNING -> UiKit.WARNING;
            case ERROR, DISABLED -> UiKit.DANGER;
        };
        this.title = title;
        this.message = message;

        switch (type) {
            case INFO -> icon = "J";
            case ENABLED -> icon = "K";
            case DISABLED, ERROR -> icon = "I";
            case WARNING -> icon = "L";
            default -> icon = "H";
        }

        width = isDefault() ? FontRenderers.sf_bold_mini.getStringWidth(message) + 38f : FontRenderers.sf_bold_micro.getStringWidth(title + " " + message) + 28f;
        height = isDefault() ? 25 : 13;

        animation = new EaseOutBack(isDefault() ? 10 : 20);

        animationX = width;
        if (isDefault())
            y = mc.getWindow().getScaledHeight() - height;
        else y = mc.getWindow().getScaledHeight() / 2f + 10;
    }

    public void render(MatrixStack matrix, float getY) {
        int animatedAlpha = (int) MathUtility.clamp((1 - animation.getAnimationd()) * 255, 0, 255);
        Color color = new Color(200, 200, 205, animatedAlpha);
        Color accent = Render2DEngine.injectAlpha(typeColor, animatedAlpha);

        float progress = Math.clamp((System.currentTimeMillis() - created) / (float) Math.max(1, lifeTime), 0f, 1f);

        direction = isFinished();
        animationX = (float) (width * animation.getAnimationd());
        y = animate(y, getY);

        if (isDefault()) {
            float x = mc.getWindow().getScaledWidth() - 6 - width + animationX;

            UiKit.round(matrix, x + 4, y + 4, 2f, height - 8, 1f, accent);
            FontRenderers.mid_icons.drawString(matrix, icon, x + 11, y + 7, accent.getRGB());

            FontRenderers.sf_bold_mini.drawString(matrix, title, x + 30, y + 6, HudEditor.textColor.getValue().getColor());
            FontRenderers.sf_bold_mini.drawString(matrix, message, x + 30, y + 15, color.getRGB());

            Render2DEngine.drawRect(matrix, x + 8, y + height - 3f, (width - 16) * (1f - progress), 1f, Render2DEngine.injectAlpha(typeColor, (int) (animatedAlpha * 0.6f)));
        } else {
            float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;

            UiKit.round(matrix, x + 3, y + 3, 1.5f, height - 6, 0.75f, accent);
            FontRenderers.icons.drawString(matrix, icon, x + 9, y + 5.5f, accent.getRGB());
            FontRenderers.sf_bold_micro.drawString(matrix, title + " " + message, x + 22, y + 5, color.getRGB());
        }
    }

    public void onUpdate() {
        animation.update(direction);
    }

    public void renderShaders(MatrixStack matrix, float getY) {
        direction = isFinished();
        animationX = (float) (width * animation.getAnimationd());
        y = animate(y, getY);
        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Blurry)) {
            Render2DEngine.drawHudBase2(matrix, isDefault() ? mc.getWindow().getScaledWidth() - 6 - width + animationX : mc.getWindow().getScaledWidth() / 2f - width / 2f,
                    y, width, height, isDefault() ? 7f : 4f, HudEditor.blurStrength.getValue(), HudEditor.blurOpacity.getValue(), (float) MathUtility.clamp((1 - animation.getAnimationd()), 0f, 1f));
        } else {
            Render2DEngine.drawHudBase(matrix, isDefault() ? mc.getWindow().getScaledWidth() - 6 - width + animationX : mc.getWindow().getScaledWidth() / 2f - width / 2f,
                    y, width, height, isDefault() ? 7f : 4f, (float) MathUtility.clamp((1 - animation.getAnimationd()), 0f, 1f));
        }
    }

    private boolean isFinished() {
        return timer.passedMs(lifeTime);
    }

    public double getHeight() {
        return height;
    }

    public boolean shouldDelete() {
        return isFinished() && animationX >= width - 5;
    }

    public float animate(float value, float target) {
        return value + (target - value) / 8f;
    }

    public enum Type {
        SUCCESS("Success", Formatting.GREEN),
        INFO("Information", Formatting.AQUA),
        WARNING("Warning", Formatting.GOLD),
        ERROR("Error", Formatting.RED),
        ENABLED("Module enabled", Formatting.DARK_GREEN),
        DISABLED("Module disabled", Formatting.DARK_RED);

        final String name;
        final Formatting color;

        Type(String name, Formatting color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public Formatting getColor() {
            return color;
        }
    }
}