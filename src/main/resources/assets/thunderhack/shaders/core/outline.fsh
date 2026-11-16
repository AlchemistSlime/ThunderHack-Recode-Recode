#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 resolution;
uniform vec4 color;       // a = сила свечения
uniform vec4 fillColor;   // a = прозрачность заливки
uniform int lineWidth;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution;
    vec2 px = 1.0 / resolution;
    vec4 c0 = texture(DiffuseSampler, uv);
    bool inside = c0.a > 0.0;
    float w = float(lineWidth);
    float dmin = 99.0;
    vec3 ncol = c0.rgb;

    for (int x = -lineWidth; x <= lineWidth; x++) {
        for (int y = -lineWidth; y <= lineWidth; y++) {
            vec4 t = texture(DiffuseSampler, uv + vec2(x, y) * px);
            bool other = t.a > 0.0;
            if (other != inside) {
                float d = length(vec2(x, y));
                if (d < dmin) {
                    dmin = d;
                    if (!inside) ncol = t.rgb;
                }
            }
        }
    }

    if (inside) {
        // внутри: яркая рамка по краю, дальше заливка
        if (dmin <= max(1.0, w * 0.5)) fragColor = vec4(c0.rgb, 1.0);
        else fragColor = vec4(c0.rgb, fillColor.a);
    } else {
        // снаружи: свечение цвета ближайшего края, затухающее от контура
        if (dmin > 90.0) discard;
        float a = clamp(1.0 - (dmin - 1.0) / (w + 1.0), 0.0, 1.0);
        fragColor = vec4(ncol, a * a * color.a);
    }
}
