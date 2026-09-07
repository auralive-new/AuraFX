package com.aurafx.sdk.beauty

internal object BeautyShaders {
    const val VERT_RESOLVE = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
uniform float uMirror;
out vec2 vUv;
void main() {
  gl_Position = vec4(aPos.x * uMirror, aPos.y, 0.0, 1.0);
  vUv = aUv;
}
"""

    const val FRAG_RESOLVE_OES = """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES uTexture;
uniform mat4 uTexMatrix;
in vec2 vUv;
out vec4 fragColor;
void main() {
  vec4 t = uTexMatrix * vec4(vUv, 0.0, 1.0);
  fragColor = texture(uTexture, t.xy);
}
"""
    const val VERT_BLIT = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
out vec2 vUv;
void main() {
  gl_Position = vec4(aPos, 0.0, 1.0);
  vUv = aUv;
}
"""

    const val FRAG_COPY_2D = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vUv;
out vec4 fragColor;
void main() {
  fragColor = texture(uTexture, vUv);
}
"""

    const val FRAG_SKIN = """#version 300 es
precision mediump float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform vec2 uTexel;
uniform float uFine;
uniform float uSmooth;
uniform float uTextureKeep;
uniform float uBlemish;
uniform float uEvenness;
uniform float uBrightness;
uniform float uWhiten;
uniform float uRuddy;
uniform float uTone;
uniform float uNatural;
uniform float uTooth;
uniform float uCircles;
in vec2 vUv;
out vec4 fragColor;

vec3 rangeBlur(vec2 uv, float radius, vec3 center) {
  vec3 acc = center;
  float wsum = 1.0;
  for (int i = -4; i <= 4; i++) {
    if (i == 0) continue;
    float fi = float(i);
    vec2 offH = vec2(fi, 0.0) * uTexel * radius;
    vec2 offV = vec2(0.0, fi) * uTexel * radius;
    vec3 h = texture(uImage, uv + offH).rgb;
    vec3 v = texture(uImage, uv + offV).rgb;
    float dh = distance(h, center);
    float dv = distance(v, center);
    float wh = exp(-dh * dh * 70.0);
    float wv = exp(-dv * dv * 70.0);
    acc += h * wh + v * wv;
    wsum += wh + wv;
  }
  return acc / max(wsum, 0.001);
}

void main() {
  vec4 orig4 = texture(uImage, vUv);
  vec3 orig = orig4.rgb;
  vec4 m = texture(uMask, vUv);
  float skin = m.r;
  float teeth = m.g;
  float circles = m.b;
  float protect = m.a;
  float skinWork = skin * (1.0 - protect);
  float radius = 1.0 + 7.0 * max(uFine, max(uSmooth, max(uBlemish, uEvenness)));
  vec3 blurred = rangeBlur(vUv, radius, orig);
  vec3 detail = orig - blurred;
  float smoothAmt = clamp(uFine * 0.55 + uSmooth * 0.85 + uBlemish * 0.7 + uEvenness * 0.45, 0.0, 1.0);
  vec3 base = mix(orig, blurred, smoothAmt * skinWork);
  base += detail * uTextureKeep * skinWork * (0.35 + 0.65 * (1.0 - uBlemish));
  float lum = dot(base, vec3(0.299, 0.587, 0.114));
  base += vec3(uBrightness) * 0.18 * skinWork;
  vec3 whiteTarget = mix(base, vec3(lum * 1.02 + 0.04), 0.65);
  base = mix(base, whiteTarget, uWhiten * 0.55 * skinWork);
  vec3 ruddy = base + vec3(0.045, -0.008, -0.018) * uRuddy * skinWork;
  base = mix(base, ruddy, skinWork);
  float coolWarm = (uTone - 0.5) * 2.0;
  base += vec3(0.02, 0.0, -0.02) * coolWarm * skinWork;
  vec3 under = base + vec3(0.07, 0.05, 0.06) * uCircles;
  under = mix(under, vec3(lum + 0.04), 0.15 * uCircles);
  base = mix(base, under, circles * uCircles * (1.0 - protect));
  float toothLum = dot(orig, vec3(0.299, 0.587, 0.114));
  vec3 tooth = orig / max(toothLum, 0.08) * mix(toothLum, min(1.0, toothLum * 1.18 + 0.06), uTooth);
  tooth = mix(orig, clamp(tooth, 0.0, 1.0), uTooth);
  vec3 colored = mix(base, tooth, teeth * uTooth);
  vec3 outc = mix(colored, orig, uNatural * skinWork);
  outc = mix(orig, outc, step(0.001, skinWork + teeth * uTooth + circles * uCircles));
  // Protect eyes/brows/lips from global terms but keep local tooth/circle
  outc = mix(outc, orig, protect * (1.0 - teeth * uTooth));
  fragColor = vec4(clamp(outc, 0.0, 1.0), orig4.a);
}
"""

    const val VERT_WARP = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec2 aDisp;
out vec2 vUv;
void main() {
  vUv = aUv + aDisp;
  gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    const val FRAG_WARP = """#version 300 es
precision mediump float;
uniform sampler2D uImage;
in vec2 vUv;
out vec4 fragColor;
void main() {
  vec2 uv = clamp(vUv, 0.0, 1.0);
  fragColor = texture(uImage, uv);
}
"""

    fun requiredSkinUniforms(): List<String> = listOf(
        "uImage", "uMask", "uTexel", "uFine", "uSmooth", "uTextureKeep",
        "uBlemish", "uEvenness", "uBrightness", "uWhiten", "uRuddy", "uTone",
        "uNatural", "uTooth", "uCircles",
    )
}
