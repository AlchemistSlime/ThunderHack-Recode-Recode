package thunder.hack.core.manager.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.manager.IManager;
import thunder.hack.features.modules.render.ESP;
import thunder.hack.features.modules.render.Shaders;
import thunder.hack.injection.accesors.IMinecraftClient;
import thunder.hack.utility.render.IOutlineFbSwap;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import thunder.hack.utility.render.shaders.satin.api.managed.ShaderEffectManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Шейдеры существ и рук (схема как у Meteor).
 * Стандартная подсветка Майнкрафта НЕ включается: нужные существа ещё раз рисуются
 * одним цветом в свой буфер, а потом поверх кадра накладывается наш шейдер.
 * Поэтому вода, блоки и рамка выделения в буфер больше не попадают.
 */
public class ShaderManager implements IManager {
    public float time = 0;
    public boolean active;

    /** true, пока мы сами дорисовываем существо или руки (защита от повторного входа). */
    public static boolean drawingExtra;
    /** true, пока руки рисуются в буфер шейдера (глушим оверлеи огня/воды). */
    public static boolean renderingHands;

    private static final ManagedCoreShader OUTLINE = core("outline");
    private static final ManagedCoreShader DEFAULT = core("fx_default");
    private static final ManagedCoreShader SMOKE = core("fx_smoke");
    private static final ManagedCoreShader GRADIENT = core("fx_gradient");
    private static final ManagedCoreShader SNOW = core("fx_snow");
    private static final ManagedCoreShader FADE = core("fx_fade");

    public final EntityBuffer espBuffer = new EntityBuffer();
    public final EntityBuffer shaderBuffer = new EntityBuffer();
    private SimpleFramebuffer handBuffer;

    private boolean frameActive, espActive, shaderActive;
    private final List<RenderTask> tasks = new ArrayList<>();

    private static ManagedCoreShader core(String name) {
        return ShaderEffectManager.getInstance().manageCoreShader(Identifier.of("thunderhack", name), VertexFormats.POSITION);
    }

    // ---------------------------------------------------------------- буфер для существ

    public static class EntityBuffer {
        public OutlineVertexConsumerProvider provider;
        public SimpleFramebuffer fb;
        public boolean used;

        void begin() {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer main = mc.getFramebuffer();
            if (provider == null)
                provider = new OutlineVertexConsumerProvider(mc.getBufferBuilders().getEntityVertexConsumers());
            if (fb == null) {
                fb = new SimpleFramebuffer(main.textureWidth, main.textureHeight, true);
                fb.setClearColor(0f, 0f, 0f, 0f);
            } else if (fb.textureWidth != main.textureWidth || fb.textureHeight != main.textureHeight) {
                fb.resize(main.textureWidth, main.textureHeight);
            }
            fb.clear();
            used = false;
            main.beginWrite(false);
        }
    }

    public boolean isCustom(VertexConsumerProvider vcp) {
        return vcp == espBuffer.provider || vcp == shaderBuffer.provider;
    }

    /** Начало кадра мира (WorldRenderer.render HEAD). */
    public void beginFrame() {
        ESP esp = ModuleManager.esp;
        Shaders sh = ModuleManager.shaders;
        espActive = esp != null && esp.isEnabled() && esp.isOutlineMode();
        shaderActive = sh != null && sh.isEnabled() && sh.hasEntityTargets();
        if (espActive) espBuffer.begin();
        if (shaderActive) shaderBuffer.begin();
        frameActive = espActive || shaderActive;
    }

    public boolean wantEsp(Entity e) {
        return frameActive && espActive && ModuleManager.esp.shouldRender(e);
    }

    public boolean wantShader(Entity e) {
        return frameActive && shaderActive && ModuleManager.shaders.shouldRender(e);
    }

    /** Вызывается прямо перед тем, как игра рисует свои контуры (внутри отрисовки мира). */
    public void endEntities() {
        if (!frameActive) return;
        frameActive = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        IOutlineFbSwap swap = (IOutlineFbSwap) mc.worldRenderer;

        if (espActive && espBuffer.used) flush(espBuffer, swap);
        if (shaderActive && shaderBuffer.used) flush(shaderBuffer, swap);
        mc.getFramebuffer().beginWrite(false);

        if (espActive && espBuffer.used) {
            ESP esp = ModuleManager.esp;
            drawPass(OUTLINE, espBuffer.fb, p -> {
                Framebuffer main = mc.getFramebuffer();
                f(p, "resolution", main.textureWidth, main.textureHeight);
                i(p, "lineWidth", esp.getLineWidth());
                f(p, "color", 1f, 1f, 1f, esp.getGlow() / 100f);
                f(p, "fillColor", 1f, 1f, 1f, esp.getFillAlpha() / 255f);
            });
        }
        if (shaderActive && shaderBuffer.used) {
            Shader mode = ModuleManager.shaders.mode.getValue();
            drawPass(getShader(mode), shaderBuffer.fb, p -> setupShader(mode, p));
        }
    }

    private void flush(EntityBuffer b, IOutlineFbSwap swap) {
        swap.th$pushOutlineFb(b.fb);
        drawingExtra = true;
        try {
            b.provider.draw();
        } finally {
            drawingExtra = false;
            swap.th$popOutlineFb();
        }
    }

    // ---------------------------------------------------------------- руки

    /** Вызывается сразу после того, как игра нарисовала руки. drawHands рисует их ещё раз. */
    public void renderHands(Runnable drawHands) {
        Shaders sh = ModuleManager.shaders;
        boolean hands = sh != null && sh.isEnabled() && sh.hands.getValue();
        if (!hands && tasks.isEmpty()) return;

        if (hands) applyShader(drawHands, sh.handsMode.getValue());
        for (RenderTask t : tasks) applyShader(t.task(), t.shader());
        tasks.clear();
    }

    /** Нарисовать что угодно в отдельный буфер и наложить на кадр шейдер. */
    public void renderShader(Runnable runnable, Shader mode) {
        tasks.add(new RenderTask(runnable, mode));
    }

    private void applyShader(Runnable draw, Shader mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        if (handBuffer == null) {
            handBuffer = new SimpleFramebuffer(main.textureWidth, main.textureHeight, true);
            handBuffer.setClearColor(0f, 0f, 0f, 0f);
        } else if (handBuffer.textureWidth != main.textureWidth || handBuffer.textureHeight != main.textureHeight) {
            handBuffer.resize(main.textureWidth, main.textureHeight);
        }
        handBuffer.clear();

        // на время отрисовки подменяем основной буфер игры нашим — так всё рисуется к нам
        ((IMinecraftClient) mc).th$setFramebuffer(handBuffer);
        handBuffer.beginWrite(false);
        renderingHands = true;
        drawingExtra = true;
        try {
            draw.run();
        } finally {
            drawingExtra = false;
            renderingHands = false;
            ((IMinecraftClient) mc).th$setFramebuffer(main);
            main.beginWrite(false);
        }

        drawPass(getShader(mode), handBuffer, p -> setupShader(mode, p));
    }

    // ---------------------------------------------------------------- общее

    private void drawPass(ManagedCoreShader sh, Framebuffer src, Consumer<ShaderProgram> uniforms) {
        ShaderProgram p = sh.getProgram();
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        main.beginWrite(false);
        if (p == null) return;

        f(p, "InSize", main.textureWidth, main.textureHeight);
        f(p, "resolution", mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
        uniforms.accept(p);
        p.addSamplerTexture("DiffuseSampler", src.getColorAttachment());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        // альфу кадра не трогаем, иначе ломается то, что рисуется после (например рука)
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(sh.getKey());
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        b.vertex(-1f, -1f, 0f);
        b.vertex(1f, -1f, 0f);
        b.vertex(1f, 1f, 0f);
        b.vertex(-1f, 1f, 0f);
        BufferRenderer.drawWithGlobalProgram(b.end());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        main.beginWrite(false);
    }

    public ManagedCoreShader getShader(@NotNull Shader mode) {
        return switch (mode) {
            case Gradient -> GRADIENT;
            case Smoke -> SMOKE;
            case Snow -> SNOW;
            case Fade -> FADE;
            default -> DEFAULT;
        };
    }

    private void setupShader(Shader shader, ShaderProgram p) {
        Shaders s = ModuleManager.shaders;
        float alpha0 = s.glow.getValue() ? -1.0f : s.outlineColor.getValue().getAlpha() / 255.0f;
        time += 0.004f;
        switch (shader) {
            case Gradient -> {
                f(p, "alpha0", alpha0);
                f(p, "alpha1", s.fillAlpha.getValue() / 255f);
                f(p, "alpha2", s.alpha2.getValue() / 255f);
                i(p, "lineWidth", s.lineWidth.getValue());
                i(p, "oct", s.octaves.getValue());
                i(p, "quality", s.quality.getValue());
                f(p, "factor", s.factor.getValue());
                f(p, "moreGradient", s.gradient.getValue());
                f(p, "time", time);
            }
            case Smoke -> {
                f(p, "alpha0", alpha0);
                f(p, "alpha1", s.fillAlpha.getValue() / 255f);
                i(p, "lineWidth", s.lineWidth.getValue());
                i(p, "quality", s.quality.getValue());
                c4(p, "first", s.outlineColor.getValue());
                c3(p, "second", s.outlineColor1.getValue());
                c3(p, "third", s.outlineColor2.getValue());
                c4(p, "ffirst", s.fillColor1.getValue());
                c3(p, "fsecond", s.fillColor2.getValue());
                c3(p, "fthird", s.fillColor3.getValue());
                i(p, "oct", s.octaves.getValue());
                f(p, "time", time);
            }
            case Snow -> {
                c4(p, "color", s.fillColor1.getValue());
                i(p, "quality", s.quality.getValue());
                f(p, "time", time);
            }
            case Fade -> {
                f(p, "alpha0", alpha0);
                f(p, "fillAlpha", s.fillAlpha.getValue() / 255f);
                i(p, "lineWidth", s.lineWidth.getValue());
                i(p, "quality", s.quality.getValue());
                c4(p, "outlinecolor", s.outlineColor.getValue());
                c4(p, "primaryColor", s.fillColor1.getValue());
                c4(p, "secondaryColor", s.fillColor2.getValue());
                f(p, "time", (System.currentTimeMillis() % 100000) / 1000f);
            }
            default -> {
                f(p, "alpha0", alpha0);
                i(p, "lineWidth", s.lineWidth.getValue());
                i(p, "quality", s.quality.getValue());
                c4(p, "color", s.fillColor1.getValue());
                c4(p, "outlinecolor", s.outlineColor.getValue());
            }
        }
    }

    private static void c4(ShaderProgram p, String n, thunder.hack.setting.impl.ColorSetting c) {
        f(p, n, c.getGlRed(), c.getGlGreen(), c.getGlBlue(), c.getGlAlpha());
    }

    private static void c3(ShaderProgram p, String n, thunder.hack.setting.impl.ColorSetting c) {
        f(p, n, c.getGlRed(), c.getGlGreen(), c.getGlBlue());
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

    // ---------------------------------------------------------------- совместимость со старым кодом

    public void renderShaders() {
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

    public record RenderTask(Runnable task, Shader shader) {
    }

    public enum Shader {
        Default,
        Smoke,
        Gradient,
        Snow,
        Fade
    }
}
