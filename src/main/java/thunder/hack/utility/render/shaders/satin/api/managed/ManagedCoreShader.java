package thunder.hack.utility.render.shaders.satin.api.managed;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import org.jetbrains.annotations.Nullable;
import thunder.hack.utility.render.shaders.satin.api.managed.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
    ShaderProgramKey getKey();

    @Nullable
    ShaderProgram getProgram();

    void release();
}
