package com.aurafx.sdk.makeup

internal object MakeupShaders {
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

    const val VERT = """#version 300 es
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

    const val FRAG_MAKEUP = """#version 300 es
precision mediump float;
uniform sampler2D uImage;
uniform sampler2D uMaskA;
uniform sampler2D uMaskB;
uniform vec2 uTexel;
uniform vec4 uStamps[12];
uniform float uStampKind[12];
uniform int uStampCount;
uniform float uFoundation;
uniform float uCoverage;
uniform vec3 uFoundCol;
uniform float uConcealer;
uniform vec3 uConcCol;
uniform float uBlush;
uniform vec3 uBlushCol;
uniform float uContour;
uniform vec3 uContourCol;
uniform float uHighlight;
uniform vec3 uHighCol;
uniform float uBrow;
uniform vec3 uBrowCol;
uniform float uShadow;
uniform vec3 uLidCol;
uniform vec3 uCreaseCol;
uniform float uLiner;
uniform vec3 uLinerCol;
uniform float uLash;
uniform vec3 uLashCol;
uniform float uLip;
uniform float uLipOpacity;
uniform vec3 uLipCol;
uniform int uLipLook;
uniform float uLipLiner;
uniform vec3 uLipLinerCol;
uniform float uGloss;
uniform float uLens;
uniform vec3 uLensCol;
in vec2 vUv;
out vec4 fragColor;

vec3 overlay(vec3 b, vec3 c) {
  return mix(2.0 * b * c, 1.0 - 2.0 * (1.0 - b) * (1.0 - c), step(0.5, b));
}

float stampW(vec4 s) {
  vec2 d = (vUv - s.xy) / max(s.zw, vec2(0.001));
  float e = dot(d, d);
  return exp(-e * 2.2);
}

void main() {
  vec3 orig = texture(uImage, vUv).rgb;
  vec4 a = texture(uMaskA, vUv);
  vec4 b = texture(uMaskB, vUv);
  float skin = a.r;
  float lips = a.g;
  float lids = a.b;
  float brows = a.a;
  float linerM = b.r;
  float lashM = b.g;
  float irisM = b.b;
  float concM = b.a;
  float protectEye = clamp(irisM * 2.0 + lashM, 0.0, 1.0);
  vec3 col = orig;
  float lum = dot(orig, vec3(0.299, 0.587, 0.114));
  float detail = lum - 0.5;

  float blushS = 0.0;
  float contourS = 0.0;
  float highS = 0.0;
  float concS = concM;
  for (int i = 0; i < 12; i++) {
    if (i >= uStampCount) break;
    float w = stampW(uStamps[i]);
    float k = uStampKind[i];
    blushS += w * step(0.5, 1.0 - abs(k - 0.0));
    contourS += w * step(0.5, 1.0 - abs(k - 1.0));
    highS += w * step(0.5, 1.0 - abs(k - 2.0));
    concS += w * step(0.5, 1.0 - abs(k - 3.0));
  }
  blushS = clamp(blushS, 0.0, 1.0) * skin * (1.0 - lips) * (1.0 - protectEye);
  contourS = clamp(contourS, 0.0, 1.0) * skin * (1.0 - lips);
  highS = clamp(highS, 0.0, 1.0) * skin * (1.0 - lips);
  concS = clamp(concS, 0.0, 1.0) * (1.0 - protectEye) * (1.0 - lips);

  float fAmt = uFoundation * uCoverage * skin * (1.0 - lips) * (1.0 - brows) * (1.0 - lids) * (1.0 - protectEye);
  vec3 found = mix(orig, overlay(orig, uFoundCol), 0.55) + vec3(detail * 0.35);
  col = mix(col, found, fAmt);

  vec3 conc = mix(col, overlay(col, uConcCol), 0.5);
  col = mix(col, conc, uConcealer * concS * 0.75);

  col = mix(col, overlay(col, uBlushCol), uBlush * blushS * 0.65);
  col = mix(col, col * mix(vec3(1.0), uContourCol, 0.55), uContour * contourS * 0.7);
  col = mix(col, col + uHighCol * 0.18, uHighlight * highS * 0.55);

  vec3 browCol = mix(col, overlay(col, uBrowCol), 0.7);
  col = mix(col, browCol, uBrow * brows * 0.8);

  float crease = lids * (1.0 - irisM);
  vec3 sh = mix(uLidCol, uCreaseCol, smoothstep(0.15, 0.85, lids));
  col = mix(col, overlay(col, sh), uShadow * crease * 0.7);

  col = mix(col, uLinerCol, uLiner * linerM * 0.92);
  col = mix(col, uLashCol, uLash * lashM * 0.9);

  vec3 lip = mix(col, overlay(col, uLipCol), uLipOpacity);
  lip = mix(lip, lip + vec3(detail * 0.25), 0.4);
  if (uLipLook == 1) {
    lip = mix(lip, uLipCol, 0.35);
    lip *= 0.92;
  } else if (uLipLook == 2) {
    float om = smoothstep(0.15, 0.85, lips);
    vec3 inner = uLipCol * vec3(0.55, 0.35, 0.40);
    lip = mix(lip, overlay(lip, inner), om * 0.7);
  } else {
    lip = mix(lip, lip + vec3(0.10, 0.06, 0.06), 0.25);
  }
  col = mix(col, lip, uLip * lips);
  float edge = lips * (1.0 - smoothstep(0.2, 0.85, lips));
  col = mix(col, mix(col, uLipLinerCol, 0.8), uLipLiner * edge);

  float spec = pow(clamp(lum * 1.3, 0.0, 1.0), 8.0) * lips;
  col = mix(col, col + vec3(0.18, 0.16, 0.15), uGloss * spec);

  float catchL = smoothstep(0.72, 0.95, lum);
  vec3 lens = mix(orig, mix(orig, uLensCol, 0.55), irisM);
  lens = mix(lens, orig, catchL);
  col = mix(col, lens, uLens * irisM * (1.0 - catchL));

  fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
"""

    fun requiredUniforms(): List<String> = listOf(
        "uFoundation", "uConcealer", "uBlush", "uContour", "uHighlight",
        "uBrow", "uShadow", "uLiner", "uLash", "uLip", "uLipLook", "uLipLiner", "uGloss", "uLens",
        "uMaskA", "uMaskB", "uStamps",
    )
}
