package com.aurafx.sdk.ar

internal object ARShaders {
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
    const val FRAG_COMPOSE = """#version 300 es
precision highp float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform int uLook;
uniform int uFamily;
uniform vec3 uPaint;
uniform float uIntensity;
uniform float uTime;
uniform float uSmile;
uniform float uMouth;
uniform float uBlinkL;
uniform float uBlinkR;
uniform float uBrow;
uniform vec2 uEyeL;
uniform vec2 uEyeR;
uniform vec2 uIrisL;
uniform vec2 uIrisR;
uniform vec2 uCheekL;
uniform vec2 uCheekR;
uniform vec2 uForehead;
uniform vec2 uMouthP;
uniform float uIod;
in vec2 vUv;
out vec4 fragColor;

float gauss(vec2 a, vec2 b, float r) {
  vec2 d = a - b;
  return exp(-dot(d, d) / max(r * r, 1e-5));
}
vec3 hsv(float h, float s, float v) {
  vec3 c = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
  return v * mix(vec3(1.0), c, s);
}
void main() {
  vec4 src4 = texture(uImage, vUv);
  vec3 src = src4.rgb;
  vec4 m = texture(uMask, vUv);
  float face = smoothstep(0.15, 0.65, m.a);
  float person = smoothstep(0.15, 0.7, m.r);
  float hair = smoothstep(0.2, 0.7, m.g);
  vec3 outc = src;
  float I = clamp(uIntensity, 0.0, 1.0);
  float uniq = fract(float(uLook) * 0.073 + 0.13);
  if (uFamily == 0) {
    float sun = gauss(vUv, uForehead + vec2(0.12 + uniq * 0.1, -0.12), 0.32 + uniq * 0.08);
    outc += uPaint * sun * I * (1.0 - hair * 0.5);
    outc = mix(outc, outc * (vec3(1.0) + uPaint * 0.12), 0.25 * I * person);
  } else if (uFamily == 1) {
    float nose = gauss(vUv, uMouthP + vec2(0.0, -uIod * 0.55), uIod * 0.18);
    outc = mix(outc, mix(outc, uPaint, 0.45), nose * I * (1.0 - uMouth * 0.2));
    float eg = (1.0 - uBlinkL) * gauss(vUv, uIrisL, uIod * 0.16) + (1.0 - uBlinkR) * gauss(vUv, uIrisR, uIod * 0.16);
    outc += uPaint * eg * 0.4 * I;
  } else if (uFamily == 2) {
    outc = mix(outc, outc + uPaint * 0.12, 0.22 * I * person);
  } else if (uFamily == 3) {
    float ch = gauss(vUv, uCheekL, uIod * 0.28) + gauss(vUv, uCheekR, uIod * 0.28) + gauss(vUv, uForehead, uIod * 0.32);
    vec3 rain = hsv(fract(vUv.x * 1.6 + uTime * 0.07 + uniq), 0.7, 1.0);
    vec3 tone = mix(uPaint, rain, 0.35);
    outc = mix(outc, mix(outc, tone, 0.55), ch * face * I * (0.45 + 0.55 * uSmile));
  } else if (uFamily == 4) {
    outc = mix(outc, outc * vec3(0.92, 0.95, 1.08) + uPaint * 0.08, 0.4 * I * person);
    float eg = gauss(vUv, uIrisL, uIod * 0.18) + gauss(vUv, uIrisR, uIod * 0.18);
    outc += uPaint * eg * 0.25 * I * (1.0 - 0.5 * (uBlinkL + uBlinkR));
  } else if (uFamily == 5) {
    float glow = gauss(vUv, uCheekL, uIod * 0.12) + gauss(vUv, uCheekR, uIod * 0.12);
    outc += uPaint * glow * 0.35 * I;
  } else if (uFamily == 6) {
    float mark = gauss(vUv, uForehead, uIod * 0.2);
    outc = mix(outc, uPaint, mark * 0.4 * I * face);
  } else if (uFamily == 7) {
    float wash = gauss(vUv, uCheekL, uIod * 0.3) + gauss(vUv, uCheekR, uIod * 0.3);
    outc = mix(outc, mix(outc, uPaint, 0.4), wash * I * face);
  } else if (uFamily == 8) {
    float cap = gauss(vUv, uForehead + vec2(0.0, -uIod * 0.2), uIod * 0.42);
    outc = mix(outc, mix(outc, uPaint, 0.5), cap * I * (1.0 - hair * 0.35));
  } else if (uFamily == 9) {
    float eg = (1.0 - uBlinkL) * gauss(vUv, uIrisL, uIod * 0.14) + (1.0 - uBlinkR) * gauss(vUv, uIrisR, uIod * 0.14);
    outc += uPaint * eg * 0.55 * I;
  }
  fragColor = vec4(clamp(outc, 0.0, 1.0), src4.a);
}
"""
    const val VERT_SPRITE = """#version 300 es
layout(location = 0) in vec2 aCorner;
layout(location = 1) in vec2 aUvLocal;
uniform vec2 uCenter;
uniform vec2 uSize;
uniform float uAngle;
out vec2 vLocal;
out vec2 vScr;
void main() {
  float c = cos(uAngle);
  float s = sin(uAngle);
  vec2 p = vec2(aCorner.x * uSize.x, aCorner.y * uSize.y);
  vec2 r = vec2(p.x * c - p.y * s, p.x * s + p.y * c);
  vec2 uv = uCenter + r;
  vLocal = aUvLocal;
  vScr = uv;
  gl_Position = vec4(uv.x * 2.0 - 1.0, (1.0 - uv.y) * 2.0 - 1.0, 0.0, 1.0);
}
"""
    const val FRAG_SPRITE = """#version 300 es
precision highp float;
uniform sampler2D uMask;
uniform vec3 uColor;
uniform int uKind;
uniform float uOcc;
uniform float uAlpha;
in vec2 vLocal;
in vec2 vScr;
out vec4 fragColor;
float sdHeart(vec2 p) {
  p.y -= 0.1;
  float a = p.x * p.x + p.y * p.y - 0.28;
  return a * a * a - p.x * p.x * p.y * p.y * p.y;
}
void main() {
  vec2 p = vLocal * 2.0 - 1.0;
  float a = 0.0;
  if (uKind == 0) {
    vec2 q = p;
    q.y += 0.2;
    float tri = max(-q.y, abs(q.x) + q.y * 0.55);
    a = 1.0 - smoothstep(0.02, 0.12, tri);
  } else if (uKind == 1) {
    float lens = min(length(p - vec2(-0.42, 0.0)), length(p - vec2(0.42, 0.0)));
    a = 1.0 - smoothstep(0.28, 0.36, lens);
    a *= smoothstep(0.08, 0.16, lens);
    float bridge = 1.0 - smoothstep(0.06, 0.12, abs(p.y)) * step(abs(p.x), 0.22);
    a = max(a, bridge * 0.9);
  } else if (uKind == 2) {
    float d = length(vec2(p.x, p.y * 1.8));
    a = 1.0 - smoothstep(0.72, 0.95, d);
    a *= smoothstep(0.45, 0.62, d);
  } else if (uKind == 3) {
    float d = length(p);
    a = 1.0 - smoothstep(0.78, 0.95, d);
    float cut = length(p - vec2(0.18, -0.05));
    a *= smoothstep(0.55, 0.72, cut);
  } else if (uKind == 4) {
    a = 1.0 - smoothstep(0.15, 0.55, abs(p.y));
    a *= 1.0 - smoothstep(0.85, 1.0, abs(p.x));
  } else if (uKind == 5) {
    a = 1.0 - smoothstep(-0.02, 0.04, sdHeart(p * 0.9));
  } else if (uKind == 6) {
    float an = atan(p.y, p.x);
    float r = length(p);
    float star = 0.45 + 0.2 * cos(an * 5.0);
    a = 1.0 - smoothstep(star, star + 0.08, r);
  } else if (uKind == 7) {
    a = 1.0 - smoothstep(0.35, 0.7, abs(p.x) + abs(p.y) * 0.4);
  } else {
    float d = length(p);
    a = 1.0 - smoothstep(0.15, 0.7, d);
  }
  float hair = texture(uMask, clamp(vScr, 0.0, 1.0)).g;
  a *= uAlpha * (1.0 - hair * uOcc);
  if (a < 0.02) discard;
  fragColor = vec4(uColor, a);
}
"""

    fun sourcesOk(): Boolean =
        FRAG_COMPOSE.contains("uLook") && FRAG_COMPOSE.contains("uFamily") &&
            FRAG_SPRITE.contains("uKind") && FRAG_SPRITE.contains("uOcc")
}
