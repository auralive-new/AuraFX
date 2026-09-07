package com.aurafx.sdk.filter

internal object FilterShaders {
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

    const val FRAG_GRADE = """#version 300 es
precision highp float;
uniform sampler2D uImage;
uniform sampler2D uMask;
uniform sampler3D uLut;
uniform vec2 uTexel;
uniform float uIntensity;
uniform float uUseLut;
uniform float uLutSize;
uniform float uExposure;
uniform float uBrightness;
uniform float uContrast;
uniform float uSaturation;
uniform float uVibrance;
uniform float uTemperature;
uniform float uTint;
uniform float uHighlights;
uniform float uShadows;
uniform float uBlacks;
uniform float uWhites;
uniform float uGamma;
uniform float uLift;
uniform float uGain;
uniform float uBalShadowR;
uniform float uBalShadowB;
uniform float uBalHighR;
uniform float uBalHighB;
uniform float uSelectiveSat;
uniform float uVignette;
uniform float uGrain;
uniform float uBloom;
uniform float uSkinProtect;
uniform float uFeatureProtect;
uniform int uSceneMode;
in vec2 vUv;
out vec4 fragColor;

float luma(vec3 c) {
  return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float smoother(float x, float e0, float e1) {
  float t = clamp((x - e0) / (e1 - e0 + 1e-6), 0.0, 1.0);
  return t * t * (3.0 - 2.0 * t);
}

vec3 sat(vec3 c, float s) {
  float l = luma(c);
  return mix(vec3(l), c, s);
}

vec3 gradeColor(vec3 c) {
  float expv = exp2(uExposure);
  c *= expv;
  c.r *= 1.0 + 0.20 * uTemperature - 0.07 * uTint;
  c.g *= 1.0 + 0.12 * uTint;
  c.b *= 1.0 - 0.20 * uTemperature - 0.07 * uTint;
  c += vec3(uBrightness * 0.12);
  c = (c - 0.5) * (1.0 + uContrast * 0.85) + 0.5;
  float l = luma(c);
  float shadowW = 1.0 - smoother(l, 0.0, 0.45);
  float highW = smoother(l, 0.55, 1.0);
  c += vec3(uShadows * 0.18 * shadowW);
  c += vec3(uHighlights * 0.18 * highW);
  c += vec3(uBlacks * 0.10 * (1.0 - l) + uWhites * 0.10 * l);
  c = max(c + vec3(uLift), 0.0) * (1.0 + uGain);
  c = pow(max(c, 0.0), vec3(1.0 / max(uGamma, 0.001)));
  l = luma(c);
  shadowW = 1.0 - smoother(l, 0.0, 0.45);
  highW = smoother(l, 0.55, 1.0);
  c.r += uBalShadowR * shadowW * 0.12 + uBalHighR * highW * 0.12;
  c.b += uBalShadowB * shadowW * 0.12 + uBalHighB * highW * 0.12;
  c = sat(c, uSaturation);
  float mx = max(c.r, max(c.g, c.b));
  float mn = min(c.r, min(c.g, c.b));
  float s0 = mx > 1e-5 ? (mx - mn) / mx : 0.0;
  c = sat(c, 1.0 + uVibrance * (1.0 - s0));
  c = sat(c, uSelectiveSat);
  return clamp(c, 0.0, 1.25);
}

vec3 sampleLut(vec3 c) {
  float n = max(uLutSize, 2.0);
  vec3 coord = clamp(c, 0.0, 1.0) * ((n - 1.0) / n) + 0.5 / n;
  return texture(uLut, coord).rgb;
}

float hash21(vec2 p) {
  return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
  vec4 src4 = texture(uImage, vUv);
  vec3 src = src4.rgb;
  vec3 graded = gradeColor(src);
  vec3 mapped = mix(graded, sampleLut(src), clamp(uUseLut, 0.0, 1.0));
  vec4 mask = texture(uMask, vUv);
  float skin = mask.r;
  float teeth = mask.g;
  float protect = mask.a;
  vec3 mild = mix(src, mapped, 0.42);
  mapped = mix(mapped, mild, skin * uSkinProtect);
  mapped = mix(mapped, src, protect * uFeatureProtect);
  float toothHue = mapped.b - mapped.r;
  mapped.r = mix(mapped.r, min(mapped.r, src.r + 0.02), teeth * uFeatureProtect);
  mapped.b = mix(mapped.b, max(mapped.b, src.b), teeth * uFeatureProtect * 0.6);
  mapped.g = mix(mapped.g, max(mapped.g, src.g), teeth * uFeatureProtect * 0.35);
  mapped.b = mapped.b + toothHue * 0.0;
  if (uBloom > 0.001) {
    vec3 bloom = vec3(0.0);
    float wsum = 0.0;
    for (int i = -2; i <= 2; i++) {
      if (i == 0) continue;
      vec2 o = vec2(float(i), float(i)) * uTexel * 3.5;
      vec3 s = texture(uImage, vUv + o).rgb;
      float w = max(luma(s) - 0.70, 0.0);
      bloom += s * w;
      wsum += w;
    }
    if (wsum > 0.0) mapped += (bloom / wsum) * uBloom * 0.45 * (1.0 - protect * 0.5);
  }
  float vig = distance(vUv, vec2(0.5));
  mapped *= 1.0 - uVignette * smoother(vig, 0.32, 0.95);
  if (uGrain > 0.001) {
    float n = hash21(vUv * vec2(1920.0, 1080.0));
    mapped += (n - 0.5) * uGrain * 0.07 * (1.0 - skin * 0.55);
  }
  if (uSceneMode == 1) {
    float y = fract(vUv.y * 22.0 + vUv.x * 3.2);
    float streak = smoothstep(0.82, 0.98, hash21(vec2(floor(vUv.x * 48.0), y)));
    mapped = mix(mapped, mapped + vec3(0.10, 0.12, 0.14), streak * (1.0 - skin * 0.7));
    mapped *= vec3(0.93, 0.95, 1.03);
  } else if (uSceneMode == 2) {
    float cloud = smoothstep(0.55, 0.95, hash21(floor(vUv * vec2(9.0, 6.0))));
    vec3 neon = vec3(0.35, 0.55, 1.0) * cloud + vec3(0.85, 0.25, 0.65) * (1.0 - cloud);
    mapped = mix(mapped, mapped * 0.75 + neon * 0.35, 0.45 * (1.0 - skin * 0.55));
  } else if (uSceneMode == 3) {
    vec2 g = abs(fract(vUv * 14.0) - 0.5);
    float grid = 1.0 - smoothstep(0.02, 0.08, min(g.x, g.y));
    mapped = mix(mapped, mapped + vec3(0.15, 0.55, 0.95) * grid, 0.35 * (1.0 - skin * 0.6));
  } else if (uSceneMode == 4) {
    float haze = smoother(vUv.y, 0.15, 0.85);
    mapped = mix(mapped, mapped * vec3(1.12, 0.92, 0.62) + vec3(0.08, 0.04, 0.0), haze * 0.28 * (1.0 - skin * 0.4));
  } else if (uSceneMode == 5) {
    float window = smoother(1.0 - vUv.y, 0.55, 0.95) * smoother(abs(vUv.x - 0.5), 0.0, 0.42);
    mapped *= mix(0.72, 1.08, window);
    mapped = mix(mapped, mapped * vec3(1.08, 0.95, 0.78), 0.22);
  } else if (uSceneMode == 6) {
    float r = distance(vUv, vec2(0.5));
    mapped *= 1.0 - smoother(r, 0.22, 0.72) * 0.45;
    mapped.r += 0.04 * smoother(r, 0.35, 0.8);
    mapped.b += 0.03 * (1.0 - smoother(r, 0.1, 0.5));
  } else if (uSceneMode == 7) {
    mapped = floor(mapped * 5.0 + 0.5) / 5.0;
    mapped = sat(mapped, 1.15);
  } else if (uSceneMode == 8) {
    vec2 p = vUv * vec2(12.0, 9.0);
    float spots = smoothstep(0.35, 0.15, length(fract(p) - 0.5) + 0.15 * hash21(floor(p)));
    mapped = mix(mapped, mapped * vec3(0.55, 0.38, 0.18), spots * 0.55 * (1.0 - skin * 0.75));
  }
  vec3 outc = mix(src, mapped, clamp(uIntensity, 0.0, 1.0));
  fragColor = vec4(clamp(outc, 0.0, 1.0), src4.a);
}
"""

    fun sourcesAvailable(): Boolean =
        FRAG_GRADE.contains("sampler3D") &&
            FRAG_GRADE.contains("uIntensity") &&
            FRAG_GRADE.contains("uLut") &&
            FRAG_GRADE.contains("uSkinProtect") &&
            FRAG_GRADE.contains("uSceneMode") &&
            FRAG_RESOLVE_OES.contains("samplerExternalOES")
}
