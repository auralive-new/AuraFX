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
float openAmt() { return uEnvelope * (1.0 - smoothstep(0.90, 1.0, uNorm)); }
vec4 finishLook(vec4 src, vec2 uv, vec3 fx, float a) {
  a = occKeep(uv, clamp(a, 0.0, 1.0) * openAmt());
  return vec4(mix(src.rgb, fx, clamp(a, 0.0, 1.0)), 1.0);
}
vec4 extraA(vec4 src, vec2 uv) {
  vec2 p = uv - uSubject;
  int t = uGiftType;
  vec3 fx = src.rgb;
  float a = 0.0;
  if (t == 10) { // LOOK_10 neon train
    float path = uv.y - (uSubject.y + 0.08) - 0.08 * sin(uv.x * 8.0 + uTime);
    float rail = 1.0 - smoothstep(0.0, 0.014, abs(path));
    float tx = mix(-0.15, 1.18, fract(uTime * 0.12 + uNorm * 0.35));
    float train = (1.0 - smoothstep(0.0, 0.045, abs(uv.x - tx))) * (1.0 - smoothstep(0.02, 0.07, abs(path)));
    float cars = step(0.0, sin((uv.x - tx) * 48.0));
    float tunnel = exp(-18.0 * length(uv - vec2(0.94, uSubject.y + 0.08)));
    a = rail * 0.45 + train * (0.55 + 0.45 * cars) + tunnel * 0.8;
    fx = mix(vec3(0.05, 0.9, 1.0), vec3(1.0, 0.15, 0.75), train);
  } else if (t == 11) { // LOOK_11 floating castle
    vec2 c = uSubject + vec2(0.0, -0.22 - 0.06 * uNorm);
    float keep = 1.0 - smoothstep(0.0, 0.09, sdEllipse(uv - c, vec2(0.16, 0.07)));
    float tower = 1.0 - smoothstep(0.0, 0.03, abs(uv.x - c.x) - 0.03) * step(c.y - 0.16, uv.y) * step(uv.y, c.y);
    float flag = 1.0 - smoothstep(0.0, 0.02, abs(uv.y - (c.y - 0.14 + 0.01 * sin(uTime * 6.0)))) * step(c.x, uv.x) * step(uv.x, c.x + 0.06);
    float cloud = fbm(uv * 6.0 + vec2(uTime * 0.05, 0.0)) * (1.0 - smoothstep(0.18, 0.32, length(uv - c)));
    a = keep * 0.85 + tower + flag * 0.7 + cloud * 0.35;
    fx = mix(vec3(0.55, 0.45, 0.7), vec3(1.0, 0.85, 0.55), tower + flag);
  } else if (t == 12) { // LOOK_12 dragon
    float ang = uTime * 1.4;
    vec2 head = uSubject + 0.22 * vec2(cos(ang), sin(ang * 0.7) * 0.6);
    float body = 0.0;
    for (int i = 0; i < 6; i++) {
      float fi = float(i);
      vec2 bp = uSubject + 0.22 * vec2(cos(ang - fi * 0.35), sin((ang - fi * 0.35) * 0.7) * 0.6);
      body = max(body, 1.0 - smoothstep(0.0, 0.028 - fi * 0.003, length(uv - bp)));
    }
    float wing = 1.0 - smoothstep(0.0, 0.02, abs(dot(uv - head, vec2(-sin(ang), cos(ang)))) - (0.02 + 0.05 * abs(sin(uTime * 10.0))));
    a = body + wing * 0.45;
    fx = mix(vec3(0.15, 0.7, 0.35), vec3(1.0, 0.85, 0.2), body);
  } else if (t == 13) { // LOOK_13 ocean
    float w = 0.55 + 0.12 * sin(uv.x * 18.0 - uTime * 3.0) + 0.08 * fbm(uv * 8.0);
    float wave = smoothstep(w, w - 0.08, uv.y) * (1.0 - smoothstep(w + 0.12, w + 0.28, uv.y));
    float foam = 1.0 - smoothstep(0.0, 0.02, abs(uv.y - w));
    a = wave * 0.7 + foam * 0.5;
    fx = mix(vec3(0.05, 0.2, 0.45), vec3(0.45, 0.85, 1.0), foam + uv.y);
  } else if (t == 14) { // LOOK_14 volcano
    vec2 c = uSubject + vec2(0.0, 0.18);
    float cone = 1.0 - smoothstep(0.02, 0.12, abs(uv.x - c.x) * 1.8 + (c.y - uv.y) * 0.35);
    cone *= step(uv.y, c.y + 0.02) * step(c.y - 0.28, uv.y);
    float lava = fbm(uv * 10.0 + vec2(0.0, -uTime * 0.4)) * cone;
    float erupt = exp(-20.0 * length(uv - (c + vec2(0.0, -0.22)))) * smoothstep(0.28, 0.5, uNorm);
    a = cone * 0.75 + lava * 0.5 + erupt;
    fx = mix(vec3(0.15, 0.05, 0.04), vec3(1.0, 0.35, 0.05), lava + erupt);
  } else if (t == 15) { // LOOK_15 sakura
    float branch = 1.0 - smoothstep(0.0, 0.012, abs(p.x * 0.4 + p.y * 0.2 - 0.04 * sin(p.y * 20.0)));
    branch *= 1.0 - smoothstep(0.28, 0.42, length(p));
    float petal = 0.0;
    for (int i = 0; i < 5; i++) {
      vec2 pp = vec2(hash(vec2(float(i), 2.1)), hash(vec2(float(i), 7.3)));
      pp.y = fract(pp.y + uTime * 0.08);
      petal = max(petal, 1.0 - smoothstep(0.0, 0.025, length(uv - pp)));
    }
    a = branch * 0.55 + petal * 0.8 + 0.12 * fbm(uv * 9.0);
    fx = mix(vec3(0.35, 0.12, 0.18), vec3(1.0, 0.7, 0.82), petal);
  } else if (t == 16) { // LOOK_16 samurai portal
    float gate = abs(sdEllipse(p * vec2(1.0, 1.4), vec2(0.16, 0.22)));
    float ring = 1.0 - smoothstep(0.0, 0.02, gate);
    float arc = 1.0 - smoothstep(0.0, 0.015, abs(p.x) - 0.02 * sin(p.y * 30.0 + uTime * 8.0));
    arc *= 1.0 - smoothstep(0.18, 0.28, abs(p.y));
    float sil = 1.0 - smoothstep(0.0, 0.04, sdEllipse(p + vec2(0.0, 0.02), vec2(0.05, 0.12)));
    a = ring + arc * 0.7 + sil * 0.55;
    fx = mix(vec3(0.9, 0.15, 0.12), vec3(1.0, 0.85, 0.35), ring);
    fx = mix(fx, vec3(0.05), sil);
  } else if (t == 17) { // LOOK_17 robot
    float tx = mix(-0.2, 1.15, smoothstep(0.05, 0.75, uNorm));
    vec2 c = vec2(tx, uSubject.y + 0.05);
    float body = 1.0 - smoothstep(0.0, 0.05, sdEllipse(uv - c, vec2(0.07, 0.12)));
    float eye = 1.0 - smoothstep(0.0, 0.015, length(uv - (c + vec2(0.02, -0.04))));
    float gear = 1.0 - smoothstep(0.035, 0.05, abs(length(uv - c) - 0.09 - 0.01 * sin(atan(uv.y - c.y, uv.x - c.x) * 8.0 + uTime * 4.0)));
    a = body + eye + gear * 0.6;
    fx = mix(vec3(0.45, 0.5, 0.55), vec3(0.2, 1.0, 0.85), eye);
  } else if (t == 18) { // LOOK_18 station
    float r = length(p * vec2(1.0, 1.3));
    float ring = 1.0 - smoothstep(0.0, 0.012, abs(r - 0.18));
    float spoke = 1.0 - smoothstep(0.0, 0.01, abs(sin(atan(p.y, p.x) * 3.0 + uTime * 0.4)) - 0.08);
    spoke *= 1.0 - smoothstep(0.22, 0.28, r);
    float sat = 1.0 - smoothstep(0.0, 0.02, length(p - 0.2 * vec2(cos(uTime), sin(uTime))));
    a = ring + spoke * 0.4 + sat;
    fx = mix(vec3(0.55, 0.65, 0.8), vec3(1.0, 0.9, 0.4), sat + ring);
  } else if (t == 19) { // LOOK_19 black hole
    float r = length(p);
    float pull = 0.12 * openAmt() / (r + 0.06);
    vec3 warped = texture(uImage, clamp(uv - p * pull, 0.0, 1.0)).rgb;
    float disk = 1.0 - smoothstep(0.06, 0.22, r);
    float hole = 1.0 - smoothstep(0.0, 0.05, r);
    fx = mix(warped, vec3(1.0, 0.55, 0.15), disk * 0.45);
    fx = mix(fx, vec3(0.0), hole);
    a = clamp(disk + 0.35, 0.0, 1.0);
    return finishLook(src, uv, fx, a);
  } else if (t == 20) { // LOOK_20 crystal
    float xt = abs(fract(uv.x * 7.0 + 0.1 * sin(uv.y * 20.0)) - 0.5);
    float grow = mix(0.02, 0.22, smoothstep(0.1, 0.55, uNorm));
    float cry = (1.0 - smoothstep(0.0, 0.03, xt - 0.04)) * step(1.0 - grow, uv.y) * fbm(uv * 8.0);
    float pulse = 0.5 + 0.5 * sin(uTime * 5.0 + uv.x * 20.0);
    a = cry * (0.5 + 0.5 * pulse);
    fx = mix(vec3(0.15, 0.4, 0.9), vec3(0.85, 0.95, 1.0), pulse);
  } else if (t == 21) { // LOOK_21 moon
    vec2 c = uSubject + vec2(0.0, -0.18);
    float moon = 1.0 - smoothstep(0.0, 0.16, length(uv - c));
    float shadow = 1.0 - smoothstep(0.0, 0.14, length(uv - (c + vec2(0.07 + 0.02 * sin(uTime * 0.4), 0.0))));
    float glow = exp(-10.0 * max(0.0, length(uv - c) - 0.16));
    a = moon * 0.75 * (1.0 - shadow * 0.85) + glow * 0.55;
    fx = mix(vec3(0.05, 0.06, 0.12), vec3(0.95, 0.92, 0.75), moon * (1.0 - shadow));
  } else if (t == 22) { // LOOK_22 cloud city
    float base = uSubject.y - 0.28;
    float isles = 0.0;
    for (int i = 0; i < 4; i++) {
      vec2 ic = vec2(0.2 + 0.2 * float(i), base + 0.03 * sin(uTime + float(i)));
      isles = max(isles, 1.0 - smoothstep(0.0, 0.05, sdEllipse(uv - ic, vec2(0.09, 0.025))));
    }
    float ship = 1.0 - smoothstep(0.0, 0.02, length(uv - vec2(fract(uTime * 0.07), base - 0.08)));
    a = isles * 0.8 + ship + 0.25 * fbm(uv * 5.0 + vec2(uTime * 0.04, 0.0));
    fx = mix(vec3(0.7, 0.78, 0.9), vec3(1.0, 0.85, 0.4), ship);
  } else if (t == 23) { // LOOK_23 thunder god
    float sil = 1.0 - smoothstep(0.0, 0.07, sdEllipse(p + vec2(0.0, -0.02), vec2(0.1, 0.22)));
    float bolt = 1.0 - smoothstep(0.0, 0.012, abs(p.x - 0.04 * sin(p.y * 40.0 + uTime * 18.0)));
    bolt *= 1.0 - smoothstep(0.35, 0.55, abs(p.y));
    float strike = step(0.72, uNorm) * (1.0 - smoothstep(0.72, 0.88, uNorm)) * exp(-8.0 * abs(p.x));
    a = sil * 0.55 + bolt + strike;
    fx = mix(vec3(0.05, 0.08, 0.2), vec3(0.7, 0.85, 1.0), bolt + strike);
  } else if (t == 24) { // LOOK_24 aurora
    float curtain = fbm(vec2(uv.x * 3.0, uv.y * 1.5 + uTime * 0.15));
    float band = smoothstep(0.15, 0.0, abs(uv.y - 0.28 - 0.1 * sin(uv.x * 4.0 + uTime)));
    a = curtain * band * 0.85 + 0.2 * (1.0 - personAt(uv));
    fx = hsv(fract(0.35 + uv.x * 0.2 + curtain * 0.15), 0.65, 0.9);
  } else if (t == 25) { // LOOK_25 lava rift
    float crack = 1.0 - smoothstep(0.0, 0.018, abs(uv.y - 0.62 - 0.04 * sin(uv.x * 12.0)) - 0.01 * fbm(uv * 20.0));
    float heat = fbm(uv * 9.0 + vec2(0.0, -uTime * 0.3)) * crack;
    a = crack * 0.7 + heat * 0.5;
    fx = mix(vec3(0.08, 0.02, 0.01), vec3(1.0, 0.4, 0.05), heat + crack);
  } else if (t == 26) { // LOOK_26 fairy
    float plant = 1.0 - smoothstep(0.0, 0.02, abs(sin(uv.x * 30.0) * 0.08 + 0.72 - uv.y));
    plant *= step(0.65, uv.y);
    float dust = 0.0;
    for (int i = 0; i < 6; i++) {
      vec2 fp = uSubject + 0.16 * vec2(sin(uTime * 1.3 + float(i)), cos(uTime * 1.1 + float(i) * 1.7));
      dust = max(dust, 1.0 - smoothstep(0.0, 0.02, length(uv - fp)));
    }
    a = plant * 0.45 + dust;
    fx = mix(vec3(0.2, 0.55, 0.15), vec3(1.0, 0.95, 0.55), dust);
  } else if (t == 27) { // LOOK_27 steam
    float gear = 1.0 - smoothstep(0.07, 0.1, abs(length(p) - 0.16 - 0.015 * sin(atan(p.y, p.x) * 10.0 + uTime * 2.0)));
    float piston = 1.0 - smoothstep(0.0, 0.02, abs(p.x) - 0.02) * (1.0 - smoothstep(0.05, 0.16, abs(p.y - 0.04 * sin(uTime * 8.0))));
    float steam = fbm(uv * 7.0 + vec2(0.0, -uTime * 0.3)) * (1.0 - smoothstep(0.22, 0.38, length(p)));
    a = gear + piston * 0.6 + steam * 0.4;
    fx = mix(vec3(0.35, 0.32, 0.28), vec3(0.85, 0.88, 0.9), steam);
  } else if (t == 28) { // LOOK_28 paper
    vec2 q = floor(uv * mix(8.0, 18.0, uNorm));
    float fold = hash(q);
    float sheet = step(0.35, fold) * (1.0 - smoothstep(0.7, 1.0, uNorm));
    float fly = 1.0 - smoothstep(0.0, 0.04, abs(uv.y - fract(fold + uTime * 0.1)));
    a = sheet * 0.55 + fly * 0.35;
    fx = mix(src.rgb, vec3(0.95, 0.9, 0.78), 0.7);
    fx *= 0.85 + 0.15 * fold;
  } else if (t == 29) { // LOOK_29 magnetic
    float ang = atan(p.y, p.x) + uTime * 1.2;
    float orbit = 1.0 - smoothstep(0.0, 0.02, abs(length(p) - 0.2 - 0.04 * sin(ang * 5.0)));
    float shard = 1.0 - smoothstep(0.0, 0.018, length(p - 0.2 * vec2(cos(uTime * 2.0), sin(uTime * 2.0))));
    a = orbit * 0.7 + shard;
    fx = mix(vec3(0.4, 0.7, 1.0), vec3(0.9, 0.95, 1.0), shard);
  }
  return finishLook(src, uv, fx, a);
}
vec4 extraB(vec4 src, vec2 uv) {
  vec2 p = uv - uSubject;
  int t = uGiftType;
  vec3 fx = src.rgb;
  float a = 0.0;
  if (t == 30) { // LOOK_30 laser
    float beams = 0.0;
    for (int i = 0; i < 5; i++) {
      float ang = float(i) * 0.7 + uTime * 0.8;
      vec2 dir = vec2(cos(ang), sin(ang));
      float d = abs(dot(p, vec2(-dir.y, dir.x)));
      beams = max(beams, (1.0 - smoothstep(0.0, 0.008, d)) * (1.0 - smoothstep(0.35, 0.02, dot(p, dir))));
    }
    a = beams;
    fx = mix(vec3(1.0, 0.1, 0.4), vec3(0.2, 1.0, 0.9), fract(uTime * 0.3));
  } else if (t == 31) { // LOOK_31 digital rain
    float drop = fract(uv.y * 18.0 - uTime * 0.9 - hash(vec2(floor(uv.x * 28.0), 1.0)) * 4.0);
    float glyph = step(0.55, hash(floor(uv * vec2(28.0, 18.0) + uTime)));
    float react = 1.0 - smoothstep(0.12, 0.32, abs(uv.x - uSubject.x));
    a = (1.0 - drop) * glyph * (0.35 + 0.65 * react) * 0.85;
    fx = vec3(0.15, 1.0, 0.4);
  } else if (t == 32) { // LOOK_32 pixel
    float cells = mix(220.0, 16.0, sin(clamp((uNorm - 0.12) * 3.4, 0.0, 3.14159)));
    vec2 q = (floor(uv * cells) + 0.5) / cells;
    fx = texture(uImage, q).rgb;
    float grid = 1.0 - smoothstep(0.0, 0.08, abs(fract(uv.x * cells) - 0.5) * abs(fract(uv.y * cells) - 0.5) * 8.0);
    a = 0.85 * openAmt();
    fx = mix(fx, fx * vec3(0.4, 0.9, 1.0), grid * 0.25);
    return finishLook(src, uv, fx, a);
  } else if (t == 33) { // LOOK_33 underwater
    float caust = fbm(uv * 8.0 + vec2(uTime * 0.2, -uTime * 0.15));
    float ray = pow(max(0.0, 1.0 - abs(uv.x - 0.5 - 0.1 * sin(uTime)) * 2.2), 4.0) * (1.0 - uv.y);
    float fish = 1.0 - smoothstep(0.0, 0.03, length((uv - vec2(fract(uTime * 0.08 + 0.2), 0.7 + 0.05 * sin(uTime * 2.0))) * vec2(0.6, 1.4)));
    a = 0.25 + 0.35 * caust + ray * 0.4 + fish * 0.7;
    fx = mix(vec3(0.02, 0.18, 0.35), vec3(0.3, 0.75, 0.9), caust + ray);
  } else if (t == 34) { // LOOK_34 desert
    float dune = 0.18 * fbm(vec2(uv.x * 4.0 + uTime * 0.1, 1.0));
    float sand = smoothstep(0.55 + dune, 0.72, uv.y);
    float vortex = 1.0 - smoothstep(0.0, 0.12, abs(length(p) - 0.25 - 0.05 * sin(atan(p.y, p.x) * 6.0 + uTime)));
    a = sand * 0.55 + vortex * 0.45;
    fx = mix(vec3(0.62, 0.45, 0.22), vec3(0.95, 0.8, 0.45), vortex);
  } else if (t == 35) { // LOOK_35 ice palace
    vec2 c = uSubject + vec2(0.0, 0.12);
    float hall = 1.0 - smoothstep(0.0, 0.08, sdEllipse(uv - c, vec2(0.2, 0.14)));
    float spire = 1.0 - smoothstep(0.0, 0.02, abs(uv.x - c.x) - 0.02) * step(c.y - 0.22, uv.y) * step(uv.y, c.y);
    float spark = fbm(uv * 14.0 + uTime) * hall;
    a = hall * 0.7 + spire + spark * 0.4;
    fx = mix(vec3(0.55, 0.75, 0.95), vec3(1.0), spark + spire);
  } else if (t == 36) { // LOOK_36 flower
    vec2 c = vec2(0.5, mix(1.15, 0.72, smoothstep(0.0, 0.4, uNorm)));
    float openF = mix(0.04, 0.22, smoothstep(0.25, 0.7, uNorm));
    float petal = 0.0;
    for (int i = 0; i < 6; i++) {
      float ang = float(i) * 1.047 + uTime * 0.2;
      vec2 pr = (uv - c) * vec2(cos(ang), sin(ang));
      petal = max(petal, 1.0 - smoothstep(0.0, 0.05, sdEllipse(pr, vec2(openF, openF * 0.4))));
    }
    float pollen = fbm(uv * 20.0) * petal * smoothstep(0.55, 0.8, uNorm);
    a = petal * 0.85 + pollen * 0.4;
    fx = mix(vec3(0.7, 0.1, 0.35), vec3(1.0, 0.85, 0.2), pollen);
  } else if (t == 37) { // LOOK_37 jungle
    float vine = 1.0 - smoothstep(0.0, 0.015, abs(uv.x - 0.08 - 0.04 * sin(uv.y * 14.0 + uTime)));
    vine += 1.0 - smoothstep(0.0, 0.015, abs(uv.x - 0.92 + 0.04 * sin(uv.y * 12.0 - uTime)));
    float leaf = fbm(uv * 9.0) * (smoothstep(0.0, 0.15, uv.x) + smoothstep(1.0, 0.85, uv.x));
    float bug = 1.0 - smoothstep(0.0, 0.012, length(uv - (uSubject + 0.18 * vec2(sin(uTime * 2.0), cos(uTime * 1.6)))));
    a = clamp(vine * 0.5 + leaf * 0.45 + bug, 0.0, 1.0);
    fx = mix(vec3(0.05, 0.25, 0.08), vec3(0.45, 0.85, 0.2), leaf);
    fx = mix(fx, vec3(1.0, 0.95, 0.4), bug);
  } else if (t == 38) { // LOOK_38 phantom train
    float ty = uSubject.y + 0.06;
    float tx = mix(-0.3, 1.3, fract(uTime * 0.08 + 0.15));
    float fog = fbm(uv * 4.0 + vec2(uTime * 0.2, 0.0));
    float train = (1.0 - smoothstep(0.0, 0.06, abs(uv.y - ty))) * (1.0 - smoothstep(0.0, 0.22, abs(uv.x - tx)));
    a = train * 0.55 + fog * 0.25;
    fx = mix(src.rgb, vec3(0.55, 0.75, 1.0), 0.5);
    fx += vec3(0.4, 0.7, 1.0) * train * 0.4;
  } else if (t == 39) { // LOOK_39 clockwork
    float r = length(p);
    float gear = 1.0 - smoothstep(0.0, 0.015, abs(r - 0.2 - 0.02 * sin(atan(p.y, p.x) * 12.0 + uTime)));
    float hand = 1.0 - smoothstep(0.0, 0.01, abs(p.x * sin(uTime) - p.y * cos(uTime))) * (1.0 - step(0.18, r));
    a = gear + hand * 0.7;
    fx = mix(vec3(0.45, 0.32, 0.18), vec3(1.0, 0.85, 0.4), hand);
  } else if (t == 40) { // LOOK_40 rocket
    float ry = mix(1.1, -0.1, smoothstep(0.12, 0.85, uNorm));
    vec2 c = vec2(0.5, ry);
    float body = 1.0 - smoothstep(0.0, 0.04, sdEllipse(uv - c, vec2(0.035, 0.09)));
    float exhaust = exp(-18.0 * length((uv - (c + vec2(0.0, 0.1))) * vec2(3.0, 1.0))) * (1.0 - step(c.y, uv.y));
    a = body + exhaust;
    fx = mix(vec3(0.7, 0.75, 0.8), vec3(1.0, 0.5, 0.1), exhaust);
  } else if (t == 41) { // LOOK_41 meteor shower
    float mets = 0.0;
    for (int i = 0; i < 7; i++) {
      float fi = float(i);
      vec2 m = vec2(fract(fi * 0.17 + uTime * 0.12), fract(0.2 + fi * 0.11 - uTime * 0.2));
      vec2 d = uv - m;
      mets = max(mets, (1.0 - smoothstep(0.0, 0.02, abs(d.x + d.y))) * (1.0 - smoothstep(0.0, 0.12, length(d))));
    }
    a = mets;
    fx = mix(vec3(0.3, 0.45, 1.0), vec3(1.0, 0.8, 0.4), mets);
  } else if (t == 42) { // LOOK_42 temple
    float rise = mix(0.22, 0.0, smoothstep(0.05, 0.4, uNorm));
    vec2 c = uSubject + vec2(0.0, 0.1 + rise);
    float base = 1.0 - smoothstep(0.0, 0.06, sdEllipse(uv - c, vec2(0.18, 0.05)));
    float coln = 1.0 - smoothstep(0.0, 0.02, abs(abs(uv.x - c.x) - 0.08)) * step(c.y - 0.16, uv.y) * step(uv.y, c.y);
    float stone = 1.0 - smoothstep(0.0, 0.02, length(uv - (c + vec2(0.16 * sin(uTime), -0.18))));
    float sig = 1.0 - smoothstep(0.0, 0.015, abs(sin(uv.x * 40.0) * 0.02 + c.y - 0.12 - uv.y));
    a = base + coln * 0.7 + stone * 0.6 + sig * 0.4;
    fx = mix(vec3(0.45, 0.32, 0.18), vec3(1.0, 0.85, 0.4), sig + stone);
  } else if (t == 43) { // LOOK_43 storm eye
    float r = length(p * vec2(1.0, 1.3));
    float swirl = fbm(vec2(atan(p.y, p.x) * 1.5 + uTime, r * 6.0));
    float wall = smoothstep(0.08, 0.16, r) * (1.0 - smoothstep(0.32, 0.48, r));
    float bolt = 1.0 - smoothstep(0.0, 0.01, abs(p.x - 0.05 * sin(p.y * 50.0 + uTime * 12.0)));
    a = wall * swirl * 0.9 + bolt * wall * 0.5;
    fx = mix(vec3(0.15, 0.18, 0.28), vec3(0.8, 0.9, 1.0), bolt);
  } else if (t == 44) { // LOOK_44 islands
    float isles = 0.0;
    for (int i = 0; i < 4; i++) {
      vec2 ic = uSubject + vec2(-0.22 + 0.14 * float(i), -0.18 + 0.04 * sin(uTime + float(i)));
      isles = max(isles, 1.0 - smoothstep(0.0, 0.045, sdEllipse(uv - ic, vec2(0.08, 0.03))));
      float fall = (1.0 - smoothstep(0.0, 0.01, abs(uv.x - ic.x))) * step(ic.y, uv.y) * step(uv.y, ic.y + 0.08);
      isles = max(isles, fall * 0.5);
    }
    a = isles;
    fx = mix(vec3(0.2, 0.45, 0.2), vec3(0.4, 0.75, 1.0), 0.4);
  } else if (t == 45) { // LOOK_45 alien
    vec2 c = uSubject + vec2(0.0, -0.26);
    float saucer = 1.0 - smoothstep(0.0, 0.05, sdEllipse(uv - c, vec2(0.16, 0.045)));
    float dome = 1.0 - smoothstep(0.0, 0.03, length(uv - (c + vec2(0.0, -0.03))));
    float beam = (1.0 - smoothstep(0.0, 0.08, abs(uv.x - uSubject.x))) * step(c.y, uv.y) * step(uv.y, uSubject.y + 0.05);
    a = saucer + dome * 0.8 + beam * 0.45;
    fx = mix(vec3(0.4, 1.0, 0.55), vec3(0.9, 1.0, 0.5), beam);
  } else if (t == 46) { // LOOK_46 golden
    float rib = 0.0;
    for (int i = 0; i < 4; i++) {
      float off = float(i) * 0.7;
      rib = max(rib, 1.0 - smoothstep(0.0, 0.02, abs(p.y - 0.08 * sin(p.x * 12.0 + uTime + off))));
    }
    rib *= 1.0 - smoothstep(0.32, 0.48, length(p));
    a = rib * 0.75 + 0.2 * fbm(uv * 8.0);
    fx = mix(vec3(0.45, 0.3, 0.08), vec3(1.0, 0.85, 0.35), rib);
  } else if (t == 47) { // LOOK_47 music
    float beat = 0.5 + 0.5 * sin(uTime * 6.283);
    float wave = 0.0;
    for (int i = 0; i < 4; i++) {
      float fi = float(i + 1);
      wave = max(wave, 1.0 - smoothstep(0.0, 0.015, abs(p.y - 0.05 * fi * beat * sin(p.x * (10.0 + fi) + uTime * fi))));
    }
    a = wave * (0.5 + 0.5 * beat);
    fx = hsv(fract(0.75 + beat * 0.1), 0.7, 1.0);
  } else if (t == 48) { // LOOK_48 butterfly
    float ang = uTime * 0.9;
    vec2 c = uSubject + 0.2 * vec2(cos(ang), sin(ang * 0.8) * 0.5);
    vec2 q = uv - c;
    q.x = abs(q.x);
    float wing = 1.0 - smoothstep(0.0, 0.04, sdEllipse(q - vec2(0.05, 0.0), vec2(0.08 + 0.03 * sin(uTime * 12.0), 0.1)));
    float body = 1.0 - smoothstep(0.0, 0.015, sdEllipse(q, vec2(0.015, 0.06)));
    a = wing * 0.8 + body;
    fx = mix(vec3(0.45, 0.2, 0.9), vec3(1.0, 0.8, 0.3), wing);
  } else if (t == 49) { // LOOK_49 final dimension
    float r = length(p * vec2(1.0, 1.4));
    float rings = 0.0;
    for (int i = 0; i < 4; i++) {
      float rr = 0.1 + 0.07 * float(i) + 0.02 * sin(uTime + float(i));
      rings = max(rings, 1.0 - smoothstep(0.0, 0.012, abs(r - rr)));
    }
    float trail = exp(-8.0 * abs(p.y)) * (0.4 + 0.6 * fbm(uv * 10.0 + uTime));
    float close = smoothstep(0.82, 1.0, uNorm);
    a = (rings + trail * 0.5) * (1.0 - close * 0.7) + close * 0.15;
    fx = mix(hsv(fract(0.55 + r + uTime * 0.05), 0.6, 1.0), vec3(1.0), close);
  }
  return finishLook(src, uv, fx, a);
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
  else if (uGiftType < 30) outc = extraA(src, vUv);
  else outc = extraB(src, vUv);
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

    fun sourcesOk(): Boolean {
        if (!FRAG_COMPOSE.contains("uGiftType")) return false
        if (!FRAG_COMPOSE.contains("timeFreeze") || !FRAG_COMPOSE.contains("inkUniverse")) return false
        if (!FRAG_COMPOSE.contains("extraA") || !FRAG_COMPOSE.contains("extraB")) return false
        if (!FRAG_COMPOSE.contains("uPortraitAspect") || !VERT_SPRITE.contains("uCenter")) return false
        if (!FRAG_SPRITE.contains("uKind")) return false
        for (i in 10 until 50) {
            if (!FRAG_COMPOSE.contains("LOOK_$i")) return false
        }
        return true
    }

    fun gpuLookCount(): Int = 50
}
