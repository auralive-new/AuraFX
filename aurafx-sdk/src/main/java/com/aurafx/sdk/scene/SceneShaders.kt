package com.aurafx.sdk.scene

internal object SceneShaders {
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
    const val FRAG_OES = """#version 300 es
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
    const val FRAG_COPY = """#version 300 es
precision mediump float;
uniform sampler2D uTexture;
in vec2 vUv;
out vec4 fragColor;
void main() { fragColor = texture(uTexture, vUv); }
"""
    const val FRAG_BACKGROUND = """#version 300 es
precision highp float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform vec2 uTexel;
uniform float uIntensity;
uniform int uMode;
uniform vec3 uColorA;
uniform vec3 uColorB;
in vec2 vUv;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

vec3 procedural(vec2 uv) {
  if (uMode <= 1) {
    vec3 acc = vec3(0.0);
    float wsum = 0.0;
    float rad = uMode == 0 ? 2.2 : 4.4;
    for (int i = -3; i <= 3; i++) {
      for (int j = -3; j <= 3; j++) {
        vec2 o = vec2(float(i), float(j)) * uTexel * rad;
        float wp = 1.0 - texture(uMask, uv + o).r;
        acc += texture(uImage, uv + o).rgb * wp;
        wsum += wp;
      }
    }
    return acc / max(wsum, 0.08);
  }
  if (uMode == 2) return uColorA;
  if (uMode == 3) return mix(uColorB, uColorA, clamp(1.0 - uv.y, 0.0, 1.0));
  if (uMode == 4) {
    float floorl = smoothstep(0.62, 0.85, uv.y);
    return mix(uColorA, uColorB, floorl);
  }
  if (uMode == 5) {
    vec3 base = mix(uColorB, uColorA * 0.35, uv.y);
    float n = hash(floor(uv * vec2(18.0, 12.0)));
    float orb = smoothstep(0.72, 0.98, n) * (0.35 + 0.65 * hash(uv * 40.0));
    return base + uColorA * orb;
  }
  if (uMode == 6) {
    vec3 sky = mix(uColorB, uColorA, pow(1.0 - uv.y, 1.4));
    float sun = smoothstep(0.18, 0.0, distance(uv, vec2(0.78, 0.22)));
    return sky + vec3(1.0, 0.85, 0.55) * sun * 0.45;
  }
  if (uMode == 7) {
    float pane = abs(fract(uv.x * 3.0) - 0.5);
    float bar = smoothstep(0.46, 0.5, pane);
    vec3 win = mix(uColorA, vec3(0.75, 0.82, 0.95), 0.4 + 0.3 * uv.y);
    return mix(win, uColorB, bar);
  }
  if (uMode == 8) {
    vec3 night = uColorB;
    float w = hash(floor(uv * vec2(24.0, 16.0)));
    float light = smoothstep(0.86, 0.99, w);
    return night + uColorA * light * 0.8;
  }
  float r = distance(uv, vec2(0.5, 0.42));
  return mix(uColorA, uColorB, smoothstep(0.05, 0.72, r));
}

void main() {
  vec4 src4 = texture(uImage, vUv);
  vec4 m = texture(uMask, vUv);
  float person = smoothstep(0.18, 0.62, m.r);
  vec3 bg = procedural(vUv);
  vec3 replaced = mix(bg, src4.rgb, person);
  vec3 outc = mix(src4.rgb, replaced, clamp(uIntensity, 0.0, 1.0));
  fragColor = vec4(outc, src4.a);
}
"""
    const val FRAG_HAIR = """#version 300 es
precision highp float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform vec3 uHairCol;
uniform float uIntensity;
in vec2 vUv;
out vec4 fragColor;
void main() {
  vec4 src4 = texture(uImage, vUv);
  vec3 src = src4.rgb;
  vec4 m = texture(uMask, vUv);
  float hair = smoothstep(0.22, 0.72, m.g);
  float face = smoothstep(0.25, 0.7, m.a);
  float protect = face;
  float lum = dot(src, vec3(0.299, 0.587, 0.114));
  float clum = max(dot(uHairCol, vec3(0.299, 0.587, 0.114)), 0.06);
  vec3 dyed = uHairCol * (lum / clum);
  dyed = mix(dyed, src, smoothstep(0.52, 0.92, lum) * 0.5);
  dyed = mix(dyed, src * uHairCol / clum, 0.25);
  float amt = uIntensity * hair * (1.0 - protect);
  vec3 outc = mix(src, clamp(dyed, 0.0, 1.0), clamp(amt, 0.0, 1.0));
  fragColor = vec4(outc, src4.a);
}
"""
    const val VERT_WARP = """#version 300 es
layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUv;
layout(location = 2) in vec2 aDisp;
out vec2 vUv;
out vec2 vId;
void main() {
  vId = aUv;
  vUv = aUv + aDisp;
  gl_Position = vec4(aPos, 0.0, 1.0);
}
"""
    const val FRAG_BODY = """#version 300 es
precision mediump float;
uniform sampler2D uImage;
uniform sampler2D uMask;
in vec2 vUv;
in vec2 vId;
out vec4 fragColor;
void main() {
  vec4 orig = texture(uImage, clamp(vId, 0.0, 1.0));
  vec4 warped = texture(uImage, clamp(vUv, 0.0, 1.0));
  vec4 m = texture(uMask, clamp(vId, 0.0, 1.0));
  float body = smoothstep(0.22, 0.68, m.b);
  float face = smoothstep(0.18, 0.62, m.a);
  float hair = smoothstep(0.18, 0.62, m.g);
  float person = smoothstep(0.18, 0.62, m.r);
  float allow = body * (1.0 - face) * (1.0 - hair * 0.8) * person;
  fragColor = mix(orig, warped, allow);
}
"""
    const val FRAG_LIGHT = """#version 300 es
precision highp float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform float uIntensity;
uniform int uMode;
uniform float uBrightness;
uniform float uWarmth;
uniform float uShadowLift;
uniform float uHighlight;
uniform float uAzimuth;
in vec2 vUv;
out vec4 fragColor;
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
void main() {
  vec4 src4 = texture(uImage, vUv);
  vec3 src = src4.rgb;
  vec4 m = texture(uMask, vUv);
  float subject = smoothstep(0.15, 0.7, m.r);
  float face = smoothstep(0.2, 0.7, m.a);
  float l = luma(src);
  vec3 lit = src;
  lit += vec3(uBrightness * 0.22) * (0.55 + 0.45 * face);
  float shadows = 1.0 - smoothstep(0.15, 0.55, l);
  lit += vec3(uShadowLift * 0.18) * shadows;
  float hi = smoothstep(0.62, 0.95, l);
  lit = mix(lit, src, hi * uHighlight * 0.65);
  float warm = uWarmth;
  if (uMode == 2) warm += 0.45;
  if (uMode == 3) warm -= 0.45;
  lit.r *= 1.0 + 0.12 * warm;
  lit.b *= 1.0 - 0.12 * warm;
  if (uMode == 1) {
    float dir = clamp(0.5 + 0.5 * (vUv.x - 0.5) * uAzimuth * 2.0 + (0.5 - vUv.y) * 0.25, 0.0, 1.0);
    lit += vec3(0.10, 0.09, 0.08) * dir;
  }
  if (uMode == 0) {
    lit = mix(lit, lit + vec3(0.04), 0.35);
  }
  if (uMode == 4) {
    lit = mix(src, lit, 0.7);
  }
  vec3 outc = mix(src, clamp(lit, 0.0, 1.0), clamp(uIntensity, 0.0, 1.0) * subject);
  fragColor = vec4(outc, src4.a);
}
"""

    fun lightingOk() = FRAG_LIGHT.contains("uShadowLift") && FRAG_LIGHT.contains("uIntensity")
    fun backgroundOk() = FRAG_BACKGROUND.contains("uMask") && FRAG_BACKGROUND.contains("procedural")
    fun hairOk() = FRAG_HAIR.contains("uHairCol")
}
