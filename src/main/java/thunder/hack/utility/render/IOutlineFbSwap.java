package thunder.hack.utility.render;

import net.minecraft.client.gl.Framebuffer;

/** Реализуется миксином WorldRenderer: временная подмена буфера контуров существ (как у Meteor). */
public interface IOutlineFbSwap {
    void th$pushOutlineFb(Framebuffer framebuffer);

    void th$popOutlineFb();
}
