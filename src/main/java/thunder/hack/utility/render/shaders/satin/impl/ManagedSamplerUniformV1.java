package thunder.hack.utility.render.shaders.satin.impl;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.texture.AbstractTexture;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.SamplerUniform;

import java.util.function.Supplier;

public final class ManagedSamplerUniformV1 implements SamplerUniform {
    private final String name;
    private Supplier<ShaderProgram> provider;

    public ManagedSamplerUniformV1(String name) {
        this.name = name;
    }

    public void setProvider(Supplier<ShaderProgram> provider) {
        this.provider = provider;
    }

    @Override
    public void set(AbstractTexture texture) {
        set(texture.getGlId());
    }

    @Override
    public void set(Framebuffer textureFbo) {
        set(textureFbo.getColorAttachment());
    }

    @Override
    public void set(int textureName) {
        if (provider == null) return;
        ShaderProgram p = provider.get();
        if (p != null) p.addSamplerTexture(name, textureName);
    }
}
