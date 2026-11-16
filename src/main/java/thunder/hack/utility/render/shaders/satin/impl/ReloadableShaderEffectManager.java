package thunder.hack.utility.render.shaders.satin.impl;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedCoreShader;
import thunder.hack.utility.render.shaders.satin.api.managed.ManagedShaderEffect;
import thunder.hack.utility.render.shaders.satin.api.managed.ShaderEffectManager;

import java.util.function.Consumer;

public final class ReloadableShaderEffectManager implements ShaderEffectManager {
    public static final ReloadableShaderEffectManager INSTANCE = new ReloadableShaderEffectManager();

    @Override
    public ManagedShaderEffect manage(Identifier location) {
        return null; // пост-эффекты (Shaders) в 1.21.4 пока не поддерживаются
    }

    @Override
    public ManagedShaderEffect manage(Identifier location, Consumer<ManagedShaderEffect> initCallback) {
        return null;
    }

    @Override
    public ManagedCoreShader manageCoreShader(Identifier location) {
        return manageCoreShader(location, VertexFormats.POSITION);
    }

    @Override
    public ManagedCoreShader manageCoreShader(Identifier location, VertexFormat vertexFormat) {
        return manageCoreShader(location, vertexFormat, s -> {});
    }

    @Override
    public ManagedCoreShader manageCoreShader(Identifier location, VertexFormat vertexFormat, Consumer<ManagedCoreShader> initCallback) {
        return new ResettableManagedCoreShader(location, vertexFormat, initCallback);
    }
}
