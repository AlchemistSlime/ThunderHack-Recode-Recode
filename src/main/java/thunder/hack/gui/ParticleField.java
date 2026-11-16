package thunder.hack.gui;

import net.minecraft.client.util.math.MatrixStack;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Лёгкие плавающие частицы под цвет темы. Поднимаются снизу вверх, покачиваются
 * и слегка разлетаются от курсора. Размеры дискретные, чтобы кэш свечения не разрастался.
 */
public final class ParticleField {
    private static final float[] SIZES = {1.6f, 2.4f, 3.2f};
    private static final int[] HUES = {0, 90, 180, 270};

    private static final class Particle {
        float x, y, vx, vy, size, phase;
        int hue;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long last = System.nanoTime();

    public void render(MatrixStack ms, float w, float h, float mouseX, float mouseY, float intensity) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1_000_000_000f);
        last = now;
        double t = now / 1_000_000_000.0;

        int target = (int) Math.clamp(w * h / 2600f, 30, 110);
        while (particles.size() < target)
            particles.add(spawn(w, h, true));
        while (particles.size() > target)
            particles.removeLast();

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);

            p.x += (p.vx + (float) Math.sin(t * 0.6 + p.phase) * 6f) * dt;
            p.y += p.vy * dt;

            // мягкое отталкивание от курсора
            float dx = p.x - mouseX, dy = p.y - mouseY;
            float d2 = dx * dx + dy * dy;
            if (d2 > 1f && d2 < 2500f) {
                float d = (float) Math.sqrt(d2);
                float push = (50f - d) * 3f * dt;
                p.x += dx / d * push;
                p.y += dy / d * push;
            }

            if (p.y < -8f || p.x < -12f || p.x > w + 12f) {
                particles.set(i, spawn(w, h, false));
                continue;
            }

            float twinkle = 0.55f + 0.45f * (float) Math.sin(t * 1.5 + p.phase);
            float edge = Math.clamp(Math.min(p.y, h - p.y) / 40f, 0f, 1f);
            float a = twinkle * edge * intensity;
            if (a <= 0.01f) continue;

            Color c = UiKit.accent(p.hue);
            UiKit.glow(ms, p.x - p.size, p.y - p.size, p.size * 2f, p.size * 2f, 5, UiKit.alpha(c, 0.55f * a));
            Color core = Render2DEngine.interpolateColorC(c, Color.WHITE, 0.45f);
            UiKit.round(ms, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size, p.size / 2f, UiKit.alpha(core, 0.9f * a));
        }
    }

    private Particle spawn(float w, float h, boolean anywhere) {
        Particle p = new Particle();
        p.x = random.nextFloat() * w;
        p.y = anywhere ? random.nextFloat() * h : h + 4f + random.nextFloat() * 20f;
        p.size = SIZES[random.nextInt(SIZES.length)];
        p.vx = (random.nextFloat() - 0.5f) * 6f;
        p.vy = -(6f + random.nextFloat() * 16f) * (0.6f + p.size / 3.2f * 0.6f);
        p.phase = random.nextFloat() * 6.2832f;
        p.hue = HUES[random.nextInt(HUES.length)];
        return p;
    }
}
