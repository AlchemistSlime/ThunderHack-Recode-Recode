package thunder.hack.utility.render.shaders.satin.impl;

import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Замена Satin-шейдера для Minecraft 1.21.4: программа берётся из ShaderLoader по ключу
 * (файлы лежат в assets/thunderhack/shaders/core/NAME.json), а uniform-ы привязываются лениво.
 */
public final class ResettableManagedCoreShader implements ManagedCoreShader {
    private final ShaderProgramKey key;
    private final Map<String, ManagedUniform> uniforms = new HashMap<>();
    private final Map<String, ManagedSamplerUniformV1> samplers = new HashMap<>();
    private boolean failedLogged;

    public ResettableManagedCoreShader(Identifier location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
        this.key = new ShaderProgramKey(Identifier.of(location.getNamespace(), "core/" + location.getPath()), vertexFormat, Defines.EMPTY);
        if (initCallback != null) initCallback.accept(this);
    }

    @Override
    public ShaderProgramKey getKey() {
        return key;
    }

    @Override
    public ShaderProgram getProgram() {
        try {
            return MinecraftClient.getInstance().getShaderLoader().getOrCreateProgram(key);
        } catch (RuntimeException e) {
            if (!failedLogged) {
                failedLogged = true;
                LogUtils.getLogger().error("Could not create shader program {}", key.configId(), e);
            }
            return null;
        }
    }

    @Override
    public void release() {
    }

    private ManagedUniform u(String name, int count) {
        return uniforms.computeIfAbsent(name, n -> {
            ManagedUniform m = new ManagedUniform(n, count);
            m.setProvider(this::getProgram);
            return m;
        });
    }

    @Override public Uniform1i findUniform1i(String n) { return u(n, 1); }
    @Override public Uniform2i findUniform2i(String n) { return u(n, 2); }
    @Override public Uniform3i findUniform3i(String n) { return u(n, 3); }
    @Override public Uniform4i findUniform4i(String n) { return u(n, 4); }
    @Override public Uniform1f findUniform1f(String n) { return u(n, 1); }
    @Override public Uniform2f findUniform2f(String n) { return u(n, 2); }
    @Override public Uniform3f findUniform3f(String n) { return u(n, 3); }
    @Override public Uniform4f findUniform4f(String n) { return u(n, 4); }
    @Override public UniformMat4 findUniformMat4(String n) { return u(n, 16); }

    @Override
    public SamplerUniform findSampler(String samplerName) {
        return samplers.computeIfAbsent(samplerName, n -> {
            ManagedSamplerUniformV1 s = new ManagedSamplerUniformV1(n);
            s.setProvider(this::getProgram);
            return s;
        });
    }
}
