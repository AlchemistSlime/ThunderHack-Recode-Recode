#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 resolution;
uniform vec4 color;
uniform vec4 fillColor;
uniform int lineWidth;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    vec2 px = 1.0 / resolution;
    bool inside = texture(DiffuseSampler, uv).a > 0.6;
    float w = float(lineWidth);
    float dmin = 99.0;

    for (int x = -lineWidth; x <= lineWidth; x++) {
        for (int y = -lineWidth; y <= lineWidth; y++) {
            bool other = texture(DiffuseSampler, uv + vec2(x, y) * px).a > 0.6;
            if (other != inside) dmin = min(dmin, length(vec2(x, y)));
        }
    }

    if (inside) {
        // внутри: рамка по краю, дальше заливка
        if (dmin <= max(1.0, w * 0.5)) fragColor = color;
        else fragColor = fillColor;
    } else {
        // снаружи: свечение, затухающее от края
        float a = clamp(1.0 - (dmin - 1.0) / (w + 1.0), 0.0, 1.0);
        if (dmin > 90.0) a = 0.0;
        fragColor = vec4(color.rgb, color.a * a);
    }
}
