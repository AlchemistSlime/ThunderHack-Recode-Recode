package thunder.hack.core.manager.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL30;
import thunder.hack.core.manager.IManager;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import thunder.hack.utility.render.shaders.satin.api.managed.ShaderEffectManager;

import java.awt.Color;

/**
 * Контур ESP для 1.21.4: сущности рисуются в отдельный буфер,
 * затем поверх экрана рисуется полноэкранный прямоугольник с шейдером контура.
 */
public class ShaderManager implements IManager {
    public float time = 0;
    public boolean active;
    private static class Layer {
        SimpleFramebuffer fb;
        boolean dirty;
        Color color = Color.WHITE;
    }

    private final java.util.Map<Integer, Layer> layers = new java.util.LinkedHashMap<>();
    /** ключ слоя: ESP, по цвету */
    private int activeKey = -1;

    private static final ManagedCoreShader OUTLINE = ShaderEffectManager.getInstance()
            .manageCoreShader(Identifier.of("thunderhack", "outline"), VertexFormats.POSITION);

    public void begin(int key, Color color, boolean depth) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth, h = main.textureHeight;
        Layer l = layers.computeIfAbsent(key, k -> new Layer());
        l.color = color;
        if (l.fb == null) {
            l.fb = new SimpleFramebuffer(w, h, true);
            l.fb.setClearColor(0f, 0f, 0f, 0f);
            l.fb.clear();
        } else if (l.fb.textureWidth != w || l.fb.textureHeight != h) {
            l.fb.resize(w, h);
        }
        if (!l.dirty) {
            if (depth) {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, l.fb.fbo);
                GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
            }
            l.dirty = true;
        }
        l.fb.beginWrite(true);
        activeKey = key;
        active = true;
    }

    public void end() {
        active = false;
        activeKey = -1;
        MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
    }

    private static void f(ShaderProgram p, String n, float... v) {
        GlUniform u = p.getUniform(n);
        if (u == null) return;
        switch (v.length) {
            case 1 -> u.set(v[0]);
            case 2 -> u.set(v[0], v[1]);
            case 3 -> u.set(v[0], v[1], v[2]);
            case 4 -> u.set(v[0], v[1], v[2], v[3]);
        }
    }

    private static void i(ShaderProgram p, String n, int v) {
        GlUniform u = p.getUniform(n);
        if (u != null) u.set(v);
    }

    private static float[] c(Color c, float a) {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, a};
    }

    /** Вызывается раз за кадр после отрисовки мира. */
    public void renderShaders() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        for (java.util.Map.Entry<Integer, Layer> e : layers.entrySet()) {
            Layer l = e.getValue();
            if (!l.dirty || l.fb == null) continue;
            l.dirty = false;
            drawOutline(main, l);
            l.fb.clear();
            main.beginWrite(true);
        }
    }

    private void drawQuad(ManagedCoreShader sh) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(sh.getKey());
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        b.vertex(-1f, -1f, 0f);
        b.vertex(1f, -1f, 0f);
        b.vertex(1f, 1f, 0f);
        b.vertex(-1f, 1f, 0f);
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void drawOutline(Framebuffer main, Layer l) {
        thunder.hack.features.modules.render.ESP esp = ModuleManager.esp;
        ShaderProgram p = OUTLINE.getProgram();
        main.beginWrite(true);
        if (p == null) return;
        f(p, "resolution", main.textureWidth, main.textureHeight);
        i(p, "lineWidth", esp.getLineWidth());
        f(p, "color", c(l.color, 1f));
        f(p, "fillColor", c(l.color, esp.getFillAlpha() / 255f));
        p.addSamplerTexture("DiffuseSampler", l.fb.getColorAttachment());
        drawQuad(OUTLINE);
    }

    public void reloadShaders() {
    }

    public static class ThunderHackFramebuffer extends SimpleFramebuffer {
        public ThunderHackFramebuffer(int width, int height) {
            super(width, height, true);
            setClearColor(0f, 0f, 0f, 0f);
        }
    }

    public boolean fullNullCheck() {
        return true;
    }
}
