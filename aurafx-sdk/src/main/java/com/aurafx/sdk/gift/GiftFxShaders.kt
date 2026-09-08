package com.aurafx.sdk.gift

internal object GiftFxShaders {
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
uniform sampler2D uFreeze;
uniform int uHasFreeze;
uniform int uGiftType;
uniform int uPhase;
uniform int uLayer;
uniform int uOcclusion;
uniform float uTime;
uniform float uNorm;
uniform float uEnvelope;
uniform float uGravity;
uniform vec2 uSubject;
uniform vec2 uClone;
uniform float uIod;
uniform vec2 uResolution;
uniform float uPortraitAspect;
in vec2 vUv;
out vec4 fragColor;

float hash(vec2 p) {
  return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float noise(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  float a = hash(i);
  float b = hash(i + vec2(1.0, 0.0));
  float c = hash(i + vec2(0.0, 1.0));
  float d = hash(i + vec2(1.0, 1.0));
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}
float fbm(vec2 p) {
  float v = 0.0;
  float a = 0.5;
  for (int i = 0; i < 5; i++) {
    v += a * noise(p);
    p = p * 2.03 + 17.1;
    a *= 0.5;
  }
  return v;
}
vec3 hsv(float h, float s, float v) {
  vec3 c = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
  return v * mix(vec3(1.0), c, s);
}
float sdCircle(vec2 p, float r) { return length(p) - r; }
float sdEllipse(vec2 p, vec2 r) {
  vec2 n = p / max(r, vec2(1e-4));
  return length(n) - 1.0;
}
float personAt(vec2 uv) { return texture(uMask, clamp(uv, 0.0, 1.0)).r; }
float hairAt(vec2 uv) { return texture(uMask, clamp(uv, 0.0, 1.0)).g; }
float occKeep(vec2 uv, float a) {
  float p = personAt(uv);
  float h = hairAt(uv);
  if (uOcclusion == 1) a *= 1.0 - smoothstep(0.25, 0.75, p);
  else if (uOcclusion == 2) a *= 1.0 - smoothstep(0.25, 0.75, h);
  else if (uOcclusion == 3) a *= 1.0 - smoothstep(0.2, 0.7, max(p, h));
  return a;
}
vec2 portrait(vec2 uv) {
  float aspect = uResolution.x / max(uResolution.y, 1.0);
  if (aspect > uPortraitAspect) {
    float vis = uPortraitAspect / aspect;
    float x0 = (1.0 - vis) * 0.5;
    return vec2((uv.x - x0) / max(vis, 1e-4), uv.y);
  } else {
    float vis = aspect / uPortraitAspect;
    float y0 = (1.0 - vis) * 0.5;
    return vec2(uv.x, (uv.y - y0) / max(vis, 1e-4));
  }
}
vec4 timeFreeze(vec4 src, vec2 uv) {
  vec2 d = uv - uSubject;
  float r = length(d * vec2(1.0, 1.6));
  float wave = sin(r * 42.0 - uTime * 9.0);
  float ring = smoothstep(0.08, 0.0, abs(wave) * 0.12 + abs(r - fract(uTime * 0.35) * 0.9));
  float freezeAmt = smoothstep(0.08, 0.02, abs(r - 0.22 - 0.12 * sin(uTime))) * 0.55;
  if (uPhase >= 2 && uPhase <= 3) freezeAmt = mix(0.85, 0.15, uNorm);
  if (uPhase >= 4) freezeAmt *= uEnvelope;
  vec3 frozen = src.rgb;
  if (uHasFreeze > 0) frozen = texture(uFreeze, uv).rgb;
  frozen = mix(frozen, vec3(dot(frozen, vec3(0.3, 0.59, 0.11))), 0.55);
  vec3 col = mix(src.rgb, frozen, freezeAmt * uEnvelope);
  col += vec3(0.45, 0.75, 1.0) * ring * 0.65 * uEnvelope;
  return vec4(col, 1.0);
}
vec4 portalDoor(vec4 src, vec2 uv) {
  vec2 p = (uv - uSubject) * vec2(1.0, 1.55);
  p.y += 0.04;
  float open = smoothstep(0.0, 0.35, uNorm) * (1.0 - smoothstep(0.82, 1.0, uNorm));
  float ang = atan(p.y, p.x) + uTime * 0.7;
  float rad = length(p);
  float ring = abs(rad - (0.22 + 0.06 * sin(ang * 6.0 + uTime))) - 0.018;
  float portal = 1.0 - smoothstep(0.0, 0.04, ring);
  float interior = 1.0 - smoothstep(0.0, 0.20 * open, rad);
  vec3 energy = hsv(fract(uTime * 0.08 + fbm(p * 4.0 + uTime)), 0.65, 0.9);
  energy *= 0.45 + 0.55 * fbm(p * 7.0 - uTime * 0.6);
  vec3 rim = vec3(0.35, 0.8, 1.0) * portal;
  float a = max(portal, interior * 0.85) * open * uEnvelope;
  a = occKeep(uv, a);
  vec3 col = mix(src.rgb, energy + rim, clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 meteorCreature(vec4 src, vec2 uv) {
  float t = clamp(uNorm, 0.0, 1.0);
  vec2 head = vec2(0.18 + 0.64 * t + 0.08 * sin(t * 9.0), mix(-0.06, 1.12, t));
  vec2 p = uv - head;
  p.x *= 1.15;
  float body = 1.0 - smoothstep(0.0, 0.055, sdEllipse(p, vec2(0.07, 0.045)));
  float wing = 1.0 - smoothstep(0.0, 0.03, abs(p.y + 0.01) - (0.02 + 0.03 * sin(uTime * 14.0 + p.x * 40.0)));
  wing *= 1.0 - smoothstep(0.12, 0.18, abs(p.x));
  float eye = 1.0 - smoothstep(0.0, 0.012, length(p - vec2(0.025, 0.0)));
  float trail = exp(-abs(uv.x - head.x) * 18.0) * smoothstep(head.y, head.y - 0.35, uv.y) * 0.45;
  float impact = exp(-length(uv - vec2(0.55, 0.78)) * 18.0) * smoothstep(0.45, 0.62, t) * (1.0 - smoothstep(0.72, 0.9, t));
  vec3 col = src.rgb;
  vec3 creature = mix(vec3(1.0, 0.45, 0.12), vec3(1.0, 0.9, 0.4), eye);
  float a = (body + wing * 0.7 + trail + impact) * uEnvelope;
  col = mix(col, creature + vec3(1.0, 0.5, 0.15) * impact, clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 hologramClone(vec4 src, vec2 uv) {
  vec2 delta = uClone - uSubject;
  vec2 sampleUv = uv - delta;
  float d = length((uv - uClone) / max(uIod, 0.08));
  float mask = 1.0 - smoothstep(1.05, 1.55, d);
  mask *= uEnvelope;
  vec3 holo = texture(uImage, clamp(sampleUv, 0.0, 1.0)).rgb;
  float scan = 0.55 + 0.45 * sin(uv.y * 220.0 + uTime * 8.0);
  holo.r = texture(uImage, clamp(sampleUv + vec2(0.004, 0.0), 0.0, 1.0)).r;
  holo.b = texture(uImage, clamp(sampleUv - vec2(0.004, 0.0), 0.0, 1.0)).b;
  holo *= vec3(0.35, 0.95, 1.0) * scan;
  float dissolve = 1.0 - smoothstep(0.78, 1.0, uNorm);
  mask *= dissolve;
  float core = 1.0 - smoothstep(0.55, 1.1, length((uv - uSubject) / max(uIod, 0.08)));
  mask *= 1.0 - core * 0.85;
  vec3 col = mix(src.rgb, holo + vec3(0.1, 0.4, 0.5) * 0.2, clamp(mask, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 magicPaint(vec4 src, vec2 uv) {
  vec2 p = uv - uSubject;
  float ang = atan(p.y, p.x);
  float rad = length(p * vec2(1.0, 1.45));
  float ribbon = sin(ang * 5.0 + uTime * 1.4 + fbm(uv * 6.0) * 6.0);
  float stroke = smoothstep(0.12, 0.02, abs(rad - 0.22 - 0.08 * ribbon));
  stroke += smoothstep(0.1, 0.02, abs(rad - 0.34 + 0.06 * sin(ang * 3.0 - uTime)));
  float flow = fbm(uv * 8.0 + vec2(uTime * 0.25, -uTime * 0.2));
  vec3 paint = hsv(fract(0.08 + ang * 0.08 + flow * 0.2), 0.75, 0.95);
  float a = (stroke * 0.9 + flow * 0.18) * uEnvelope;
  a *= 1.0 - smoothstep(0.88, 1.0, uNorm);
  a = occKeep(uv, a);
  vec3 col = mix(src.rgb, mix(src.rgb, paint, 0.85), clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 giantShadow(vec4 src, vec2 uv) {
  float grow = smoothstep(0.0, 0.32, uNorm) * (1.0 - smoothstep(0.78, 0.98, uNorm));
  vec2 off = vec2(-0.07 + 0.03 * sin(uTime * 1.1), -0.05);
  vec2 suv = (uv + off - uSubject) / (1.0 + 1.15 * grow) + uSubject;
  float sil = personAt(suv);
  sil = max(sil, personAt(suv + vec2(0.01, 0.0)));
  sil = max(sil, personAt(suv + vec2(-0.01, 0.02)));
  float a = smoothstep(0.12, 0.55, sil) * grow * uEnvelope * 0.85;
  a = occKeep(uv, a);
  vec3 shade = vec3(0.02, 0.03, 0.07);
  vec3 col = mix(src.rgb, shade, clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 miniWorld(vec4 src, vec2 uv) {
  vec2 p = uv - uSubject + vec2(0.0, 0.16);
  float island = 1.0 - smoothstep(0.0, 0.09, sdEllipse(p, vec2(0.22, 0.07)));
  float hills = fbm(p * 10.0 + 3.0) * island;
  float water = 1.0 - smoothstep(0.05, 0.12, abs(p.y - 0.02)) * step(length(p.x), 0.24);
  float cloud = fbm(uv * 5.0 + vec2(uTime * 0.07, 0.0));
  cloud *= 1.0 - smoothstep(0.18, 0.32, length(p - vec2(0.08, -0.12)));
  float moon = 1.0 - smoothstep(0.0, 0.03, length(p - vec2(-0.16, -0.18)));
  vec3 land = mix(vec3(0.15, 0.42, 0.18), vec3(0.45, 0.32, 0.12), hills);
  vec3 colW = mix(vec3(0.15, 0.35, 0.7), vec3(0.4, 0.7, 0.9), 0.5 + 0.5 * sin(uTime + p.x * 30.0));
  float a = (island * 0.9 + water * 0.35 + cloud * 0.4 + moon * 0.7) * uEnvelope;
  a = occKeep(uv, a);
  vec3 world = mix(land, colW, water * 0.6) + vec3(1.0) * moon * 0.4 + vec3(0.9) * cloud * 0.25;
  vec3 col = mix(src.rgb, world, clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
vec4 gravityFlip(vec4 src, vec2 uv) {
  float gy = uGravity;
  float field = fbm(uv * 9.0 + vec2(0.0, uTime * 0.4 * gy));
  float streaks = abs(sin((uv.y * 40.0 - uTime * 6.0 * gy) + field * 4.0));
  streaks = 1.0 - smoothstep(0.15, 0.55, streaks);
  streaks *= 0.22 + 0.2 * (1.0 - personAt(uv));
  vec3 dust = mix(vec3(0.7, 0.85, 1.0), vec3(1.0, 0.7, 0.3), 0.5 - 0.5 * gy);
  vec3 col = src.rgb + dust * streaks * uEnvelope;
  float band = smoothstep(0.22, 0.32, uNorm) * (1.0 - smoothstep(0.7, 0.84, uNorm));
  col += vec3(0.4, 0.7, 1.0) * band * 0.12 * (1.0 - personAt(uv));
  return vec4(col, 1.0);
}
vec4 mirrorDimension(vec4 src, vec2 uv) {
  float open = smoothstep(0.04, 0.28, uNorm) * (1.0 - smoothstep(0.82, 0.98, uNorm));
  float left = 1.0 - smoothstep(0.018, 0.05, abs(uv.x - (uSubject.x - 0.28)));
  float right = 1.0 - smoothstep(0.018, 0.05, abs(uv.x - (uSubject.x + 0.28)));
  left *= 1.0 - smoothstep(0.42, 0.72, abs(uv.y - uSubject.y));
  right *= 1.0 - smoothstep(0.42, 0.72, abs(uv.y - uSubject.y));
  vec2 warpL = uv + vec2(0.04 * sin(uv.y * 28.0 + uTime), 0.01 * cos(uTime * 2.0));
  vec2 warpR = uv + vec2(-0.04 * sin(uv.y * 26.0 - uTime), 0.01 * sin(uTime * 1.7));
  vec3 refL = texture(uImage, clamp(vec2(uSubject.x * 2.0 - warpL.x, warpL.y), 0.0, 1.0)).rgb;
  vec3 refR = texture(uImage, clamp(vec2(uSubject.x * 2.0 - warpR.x, warpR.y), 0.0, 1.0)).rgb;
  refL = mix(refL, vec3(0.4, 0.85, 1.0), 0.25);
  refR = mix(refR, vec3(1.0, 0.45, 0.85), 0.25);
  float aL = left * open * uEnvelope;
  float aR = right * open * uEnvelope;
  float center = 1.0 - smoothstep(0.16, 0.34, abs(uv.x - uSubject.x));
  aL *= 1.0 - center * 0.85;
  aR *= 1.0 - center * 0.85;
  vec3 col = mix(src.rgb, refL, clamp(aL, 0.0, 1.0));
  col = mix(col, refR, clamp(aR, 0.0, 1.0));
  col += vec3(0.5, 0.8, 1.0) * (left + right) * 0.15 * open * uEnvelope;
  return vec4(col, 1.0);
}
vec4 inkUniverse(vec4 src, vec2 uv) {
  vec2 p = uv * 3.5;
  float n = fbm(p + vec2(uTime * 0.15, -uTime * 0.12));
  vec2 flow = vec2(n, fbm(p.yx + uTime * 0.1)) - 0.5;
  float ink = fbm((uv + flow * 0.25 - uSubject) * 7.0);
  float creature = 1.0 - smoothstep(0.42, 0.62, abs(ink - 0.5) + 0.15 * length(uv - uSubject));
  creature *= 0.7 + 0.3 * sin(atan(uv.y - uSubject.y, uv.x - uSubject.x) * 5.0 + uTime);
  float tendril = smoothstep(0.15, 0.55, ink) * (1.0 - smoothstep(0.55, 0.85, ink));
  float collapse = 1.0 - smoothstep(0.82, 1.0, uNorm);
  float a = (creature * 0.75 + tendril * 0.45) * uEnvelope * collapse;
  a = occKeep(uv, a * 0.9 + creature * 0.1);
  vec3 inkC = mix(vec3(0.05, 0.02, 0.12), vec3(0.45, 0.1, 0.55), ink);
  inkC += vec3(0.2, 0.05, 0.4) * creature;
  vec3 col = mix(src.rgb, inkC, clamp(a, 0.0, 1.0));
  return vec4(col, 1.0);
}
void main() {
  vec4 src = texture(uImage, vUv);
  vec2 pu = portrait(vUv);
  vec4 outc = src;
  if (uGiftType == 0) outc = timeFreeze(src, vUv);
  else if (uGiftType == 1) outc = portalDoor(src, vUv);
  else if (uGiftType == 2) outc = meteorCreature(src, vUv);
  else if (uGiftType == 3) outc = hologramClone(src, vUv);
  else if (uGiftType == 4) outc = magicPaint(src, vUv);
  else if (uGiftType == 5) outc = giantShadow(src, vUv);
  else if (uGiftType == 6) outc = miniWorld(src, vUv);
  else if (uGiftType == 7) outc = gravityFlip(src, vUv);
  else if (uGiftType == 8) outc = mirrorDimension(src, vUv);
  else if (uGiftType == 9) outc = inkUniverse(src, vUv);
  float letter = 1.0;
  if (pu.x < 0.0 || pu.x > 1.0 || pu.y < 0.0 || pu.y > 1.0) letter = 0.35;
  fragColor = mix(src, outc, letter);
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
precision mediump float;
uniform sampler2D uMask;
uniform vec3 uColor;
uniform float uAlpha;
uniform int uKind;
uniform float uOcc;
in vec2 vLocal;
in vec2 vScr;
out vec4 fragColor;
void main() {
  vec2 p = vLocal * 2.0 - 1.0;
  float d = length(p);
  float a = 1.0 - smoothstep(0.2, 1.0, d);
  if (uKind == 2) {
    a = 1.0 - smoothstep(0.1, 0.85, abs(p.x) + abs(p.y) * 0.35);
  } else if (uKind == 4) {
    a *= 0.7 + 0.3 * sin(p.x * 12.0);
  } else if (uKind == 9) {
    a = 1.0 - smoothstep(0.05, 0.9, d * (0.7 + 0.4 * abs(p.y)));
  }
  float person = texture(uMask, clamp(vScr, 0.0, 1.0)).r;
  a *= uAlpha * (1.0 - person * uOcc);
  if (a < 0.02) discard;
  fragColor = vec4(uColor, a);
}
"""

    fun sourcesOk(): Boolean =
        FRAG_COMPOSE.contains("uGiftType") &&
            FRAG_COMPOSE.contains("timeFreeze") &&
            FRAG_COMPOSE.contains("portalDoor") &&
            FRAG_COMPOSE.contains("meteorCreature") &&
            FRAG_COMPOSE.contains("hologramClone") &&
            FRAG_COMPOSE.contains("magicPaint") &&
            FRAG_COMPOSE.contains("giantShadow") &&
            FRAG_COMPOSE.contains("miniWorld") &&
            FRAG_COMPOSE.contains("gravityFlip") &&
            FRAG_COMPOSE.contains("mirrorDimension") &&
            FRAG_COMPOSE.contains("inkUniverse") &&
            FRAG_COMPOSE.contains("uPortraitAspect") &&
            VERT_SPRITE.contains("uCenter") &&
            FRAG_SPRITE.contains("uKind")
}
