package thunder.hack.gui.clickui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.ClickGui;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.ParticleField;
import thunder.hack.gui.UiKit;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.render.animation.EaseOutBack;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static thunder.hack.features.modules.Module.mc;
import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ClickGUI extends Screen {
    public static List<AbstractCategory> windows;
    public static boolean anyHovered;

    private boolean firstOpen;
    private long openedAt = System.currentTimeMillis();
    private final ParticleField particleField = new ParticleField();
    private float scrollY, scrollPending, helpAnim, closeAnimation, prevYaw, prevPitch, closeDirectionX, closeDirectionY;
    public static boolean close = false, imageDirection;

    private long lastFrameNs = System.nanoTime();

    public static String currentDescription = "";
    public EaseOutBack imageAnimation = new EaseOutBack(6);

    public ClickGUI() {
        super(Text.of("NewClickGUI"));
        windows = Lists.newArrayList();
        firstOpen = true;
        this.setInstance();
    }

    private static ClickGUI INSTANCE = new ClickGUI();

    public static ClickGUI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickGUI();
        }

        imageDirection = true;

        return INSTANCE;
    }

    public static ClickGUI getClickGui() {
        windows.forEach(AbstractCategory::init);
        return ClickGUI.getInstance();
    }

    private void setInstance() {
        INSTANCE = this;
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        if (firstOpen) {
            float offset = 0;
            int windowHeight = 18;

            int halfWidth = mc.getWindow().getScaledWidth() / 2;
            int halfWidthCats = (int) ((((float) Module.Category.values().size() - 1) / 2f) * (ModuleManager.clickGui.moduleWidth.getValue() + 4f));

            for (final Module.Category category : Managers.MODULE.getCategories()) {
                if (category == Module.Category.HUD) continue;
                Category window = new Category(category, Managers.MODULE.getModulesByCategory(category), (halfWidth - halfWidthCats) + offset, 20, 100, windowHeight);
                window.setOpen(true);
                windows.add(window);
                offset += ModuleManager.clickGui.moduleWidth.getValue() + 2;
                if (offset > mc.getWindow().getScaledWidth())
                    offset = 0;
            }
            firstOpen = false;
        } else {
            if (windows.getFirst().getX() < 0 || windows.getFirst().getY() < 0) {
                float offset = 0;

                int halfWidth = mc.getWindow().getScaledWidth() / 2;
                int halfWidthCats = (int) (3 * (ModuleManager.clickGui.moduleWidth.getValue() + 4f));

                for (AbstractCategory w : windows) {
                    w.setX((halfWidth - halfWidthCats) + offset);
                    w.setY(20);
                    offset += ModuleManager.clickGui.moduleWidth.getValue() + 2;
                    if (offset > mc.getWindow().getScaledWidth())
                        offset = 0;
                }
            }
        }
        windows.forEach(AbstractCategory::init);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        windows.forEach(AbstractCategory::tick);
        imageAnimation.update(imageDirection);

        if (close) {
            if (mc.player != null) {
                if (mc.player.getPitch() > prevPitch)
                    closeDirectionY = (prevPitch - mc.player.getPitch()) * 300;

                if (mc.player.getPitch() < prevPitch)
                    closeDirectionY = (prevPitch - mc.player.getPitch()) * 300;

                if (mc.player.getYaw() > prevYaw)
                    closeDirectionX = (prevYaw - mc.player.getYaw()) * 300;

                if (mc.player.getYaw() < prevYaw)
                    closeDirectionX = (prevYaw - mc.player.getYaw()) * 300;
            }

            if (closeDirectionX < 1 && closeDirectionY < 1 && closeAnimation > 2)
                closeDirectionY = -3000;

            closeAnimation++;
            if (closeAnimation > 6) {
                close = false;
                windows.forEach(AbstractCategory::restorePos);
                close();
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (ModuleManager.clickGui.blur.getValue())
            applyBlur();

        float open = UiKit.easeOutCubic((System.currentTimeMillis() - openedAt) / 320f);
        UiKit.backdrop(context.getMatrices(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), open);
        if (ModuleManager.clickGui.particles.getValue())
            particleField.render(context.getMatrices(), mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), mouseX, mouseY, open);

        anyHovered = false;

        ClickGui.Image image = ModuleManager.clickGui.image.getValue();

        if (image != ClickGui.Image.None) {
            RenderSystem.setShaderTexture(0, image.file);

            Render2DEngine.renderTexture(context.getMatrices(),

                    mc.getWindow().getScaledWidth() - image.fileWidth * imageAnimation.getAnimationd(),
                    mc.getWindow().getScaledHeight() - image.fileHeight,

                    image.fileWidth,
                    image.fileHeight,


                    0, 0,
                    image.fileWidth, image.fileHeight, image.fileWidth, image.fileHeight);
        }

        if (closeAnimation <= 6) {
            windows.forEach(w -> {
                w.setX((float) (w.getX() + closeDirectionX * AnimationUtility.deltaTime()));
                w.setY((float) (w.getY() + closeDirectionY * AnimationUtility.deltaTime()));
            });
        }


        //   Render2DEngine.drawMainMenuShader(context.getMatrices(), 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());

        // плавная прокрутка: колесо копит «долг», каждый кадр отдаём его часть
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastFrameNs) / 1_000_000_000f);
        lastFrameNs = nowNs;
        float scrollStep = Math.abs(scrollPending) < 0.05f ? scrollPending : scrollPending * Math.min(1f, dt * 14f);
        scrollPending -= scrollStep;
        scrollY = scrollStep;

        if (ModuleManager.clickGui.scrollMode.getValue() == ClickGui.scrollModeEn.Old) {
            for (AbstractCategory window : windows) {
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 264))
                    window.setY(window.getY() + 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 265))
                    window.setY(window.getY() - 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 262))
                    window.setX(window.getX() + 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 263))
                    window.setX(window.getX() - 2);
                if (scrollY != 0)
                    window.setY(window.getY() + scrollY);
            }
        } else for (AbstractCategory window : windows)
            if (scrollY != 0)
                window.setModuleOffset(scrollY, mouseX, mouseY);

        scrollY = 0;
        context.getMatrices().push();
        context.getMatrices().translate(0, (1f - open) * -16f, 0);
        windows.forEach(w -> w.render(context, mouseX, mouseY, delta));
        context.getMatrices().pop();

        if (!Objects.equals(currentDescription, "") && ModuleManager.clickGui.descriptions.getValue()) {
            Render2DEngine.drawHudBase(context.getMatrices(), mouseX + 7, mouseY + 5, FontRenderers.sf_medium.getStringWidth(currentDescription) + 6, 11, 1f, false);
            FontRenderers.sf_medium.drawString(context.getMatrices(), currentDescription, mouseX + 10, mouseY + 8, HudEditor.getColor(0).getRGB());
            currentDescription = "";
        }

        if (ModuleManager.clickGui.tips.getValue() && !close)
            renderHelp(context, mouseX, mouseY);

        if (!HudElement.anyHovered && !ClickGUI.anyHovered)
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
                GLFW.glfwSetCursor(mc.getWindow().getHandle(), GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
            }

    }

    /** Кнопка «?» внизу слева: при наведении показывает подсказки по управлению. */
    private void renderHelp(DrawContext context, int mouseX, int mouseY) {
        float size = 18f, x = 10f, y = mc.getWindow().getScaledHeight() - size - 10f;
        boolean hov = Render2DEngine.isHovered(mouseX, mouseY, x, y, size, size);
        helpAnim = AnimationUtility.fast(helpAnim, hov ? 1f : 0f, 16f);

        UiKit.glow(context.getMatrices(), x, y, size, size, 8, UiKit.alpha(UiKit.accent(0), 0.25f + 0.35f * helpAnim));
        UiKit.glass(context.getMatrices(), x, y, size, size, 9f, 1f);
        if (helpAnim > 0.02f)
            UiKit.accentFill(context.getMatrices(), x, y, size, size, 9f, helpAnim);
        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), "?",
                x + (size - FontRenderers.sf_bold_mini.getStringWidth("?")) / 2f, y + (size - 8f) / 2f + 0.5f, Color.WHITE.getRGB());

        if (helpAnim < 0.03f) return;

        String[] lines = isRu() ? new String[]{
                "ЛКМ по модулю — включить",
                "ПКМ по модулю — открыть настройки",
                "Колёсико по модулю — назначить клавишу",
                "Ctrl + F — поиск",
                "Shift + ЛКМ — показ в Array List",
                "Колёсико по слайдеру — ввод числа",
                "Delete + ЛКМ — сброс настроек модуля",
                "Перетащите конфиг в окно — загрузить"
        } : new String[]{
                "LMB on module — toggle",
                "RMB on module — open settings",
                "Middle click on module — bind key",
                "Ctrl + F — search",
                "Shift + LMB — Array List visibility",
                "Middle click on slider — type value",
                "Delete + LMB — reset module settings",
                "Drop a config into the window — load"
        };

        float w = 0;
        for (String l : lines) w = Math.max(w, FontRenderers.sf_medium.getStringWidth(l));
        w += 20f;
        float h = lines.length * 11f + 12f;
        float px = x, py = y - h - 6f + (1f - helpAnim) * 6f;

        UiKit.glass(context.getMatrices(), px, py, w, h, UiKit.PANEL_RADIUS, helpAnim);
        for (int i = 0; i < lines.length; i++)
            FontRenderers.sf_medium.drawString(context.getMatrices(), lines[i], px + 10f, py + 8f + i * 11f,
                    UiKit.alpha(i == 0 ? UiKit.accent(0) : Color.WHITE, helpAnim * (i == 0 ? 1f : 0.85f)).getRGB());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollPending += (float) (verticalAmount * 15D);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        windows.forEach(w -> {
            w.mouseClicked((int) mouseX, (int) mouseY, button);
            windows.forEach(w1 -> {
                if (w.dragging && w != w1) w1.dragging = false;
            });
        });
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        //   if (!setup && ConfigManager.firstLaunch) return false;
        windows.forEach(w -> w.mouseReleased((int) mouseX, (int) mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char key, int modifier) {
        windows.forEach(w -> w.charTyped(key, modifier));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        windows.forEach(w -> w.keyTyped(keyCode));

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (mc.player == null || !ModuleManager.clickGui.closeAnimation.getValue()) {
                imageDirection = false;
                imageAnimation.reset();
                super.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }

            if (close)
                return true;

            imageDirection = false;

            windows.forEach(AbstractCategory::savePos);

            closeDirectionX = 0;
            closeDirectionY = 0;

            close = true;
            mc.mouse.lockCursor();

            closeAnimation = 0;
            if (mc.player != null) {
                prevYaw = mc.player.getYaw();
                prevPitch = mc.player.getPitch();
            }
            return true;
        }

        return false;
    }
}
