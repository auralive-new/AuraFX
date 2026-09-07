package com.aurafx.sdk.scene

import com.aurafx.sdk.beauty.FaceLandmarks
import com.aurafx.sdk.beauty.FaceTopology
import com.aurafx.sdk.beauty.LandmarkSmoother
import com.aurafx.sdk.beauty.allFinite
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procedural strand-ribbon groom. Not a PNG sticker. Roots follow tracked
 * hairline/temple/crown landmarks; length and silhouette come from per-style recipes
 * shipped as JSON under assets/hair/groom/.
 */
data class HairGroomRecipe(
    val id: String,
    val strandCount: Int,
    val segments: Int,
    val length: Float,
    val width: Float,
    val bangs: Float,
    val gather: Float,
    val bun: Float,
    val braid: Float,
    val curtain: Float,
    val layers: Float,
    val volume: Float,
    val asymmetry: Float,
    val pixie: Float,
    val gravity: Float,
) {
    fun assetPath(): String = "hair/groom/$id.json"
}

object HairGroomRecipes {
    fun all(): List<HairGroomRecipe> = HairCatalog.styles.map { forStyle(it.id) }

    fun forStyle(styleId: String): HairGroomRecipe = when (styleId) {
        "hair.style.natural" -> HairGroomRecipe(
            styleId, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
        )
        "hair.style.bob" -> HairGroomRecipe(
            styleId, 56, 10, 0.42f, 0.018f, 0.12f, 0.05f, 0f, 0f, 0.08f, 0.2f, 0.12f, 0f, 0f, 0.85f,
        )
        "hair.style.pixie" -> HairGroomRecipe(
            styleId, 48, 7, 0.18f, 0.014f, 0.35f, 0f, 0f, 0f, 0.05f, 0.15f, 0.28f, 0.08f, 0.9f, 0.4f,
        )
        "hair.style.long_layers" -> HairGroomRecipe(
            styleId, 64, 14, 0.78f, 0.016f, 0.08f, 0.02f, 0f, 0f, 0.12f, 0.7f, 0.18f, 0.04f, 0f, 1f,
        )
        "hair.style.bangs" -> HairGroomRecipe(
            styleId, 52, 10, 0.38f, 0.017f, 0.95f, 0.04f, 0f, 0f, 0.1f, 0.25f, 0.1f, 0f, 0f, 0.7f,
        )
        "hair.style.ponytail" -> HairGroomRecipe(
            styleId, 44, 16, 0.72f, 0.014f, 0.06f, 0.92f, 0f, 0f, 0.05f, 0.15f, 0.2f, 0f, 0f, 1.05f,
        )
        "hair.style.bun" -> HairGroomRecipe(
            styleId, 40, 12, 0.28f, 0.016f, 0.05f, 0.75f, 0.95f, 0f, 0.04f, 0.1f, 0.35f, 0f, 0f, 0.2f,
        )
        "hair.style.braid" -> HairGroomRecipe(
            styleId, 36, 18, 0.7f, 0.02f, 0.04f, 0.88f, 0f, 0.95f, 0.04f, 0.1f, 0.12f, 0f, 0f, 1.0f,
        )
        "hair.style.curtain" -> HairGroomRecipe(
            styleId, 52, 12, 0.55f, 0.016f, 0.55f, 0.02f, 0f, 0f, 0.95f, 0.35f, 0.14f, 0.02f, 0f, 0.8f,
        )
        "hair.style.wolf" -> HairGroomRecipe(
            styleId, 58, 12, 0.62f, 0.015f, 0.28f, 0.08f, 0f, 0f, 0.2f, 0.85f, 0.32f, 0.12f, 0.15f, 0.9f,
        )
        "hair.style.shag" -> HairGroomRecipe(
            styleId, 60, 11, 0.5f, 0.014f, 0.4f, 0.06f, 0f, 0f, 0.18f, 0.75f, 0.22f, 0.18f, 0.2f, 0.75f,
        )
        "hair.style.volume" -> HairGroomRecipe(
            styleId, 50, 11, 0.48f, 0.018f, 0.1f, 0.04f, 0.08f, 0f, 0.08f, 0.3f, 0.95f, 0f, 0.05f, 0.45f,
        )
        "hair.style.asymmetric" -> HairGroomRecipe(
            styleId, 54, 13, 0.58f, 0.016f, 0.22f, 0.05f, 0f, 0f, 0.2f, 0.4f, 0.16f, 0.85f, 0f, 0.9f,
        )
        else -> HairGroomRecipe(
            styleId, 48, 10, 0.4f, 0.016f, 0.1f, 0.05f, 0f, 0f, 0.1f, 0.2f, 0.15f, 0f, 0f, 0.8f,
        )
    }
}

data class HairGroomMesh(
    val vertices: FloatArray,
    val indices: IntArray,
) {
    fun vertexCount(): Int = if (vertices.isEmpty()) 0 else vertices.size / STRIDE
    fun allFinite(): Boolean = vertices.all { it.isFinite() } && indices.all { it >= 0 }

    companion object {
        const val STRIDE = 6
    }
}

object HairGroomMeshBuilder {
    fun toNdcX(u: Float): Float = u * 2f - 1f
    fun toNdcY(v: Float): Float = (1f - v) * 2f - 1f

    fun yaw(lm: FaceLandmarks): Float {
        if (!lm.has(FaceTopology.LEFT_JAW) || !lm.has(FaceTopology.RIGHT_JAW) || !lm.has(FaceTopology.NOSE_TIP)) {
            return 0f
        }
        val mid = 0.5f * (lm.x(FaceTopology.LEFT_JAW) + lm.x(FaceTopology.RIGHT_JAW))
        val scale = LandmarkSmoother.interOcular(lm)
        return ((lm.x(FaceTopology.NOSE_TIP) - mid) / scale).coerceIn(-1.2f, 1.2f)
    }

    fun pitch(lm: FaceLandmarks): Float {
        if (!lm.has(FaceTopology.FOREHEAD) || !lm.has(FaceTopology.CHIN) || !lm.has(FaceTopology.NOSE_TIP)) {
            return 0f
        }
        val midY = 0.5f * (lm.y(FaceTopology.FOREHEAD) + lm.y(FaceTopology.CHIN))
        val scale = LandmarkSmoother.interOcular(lm)
        return ((lm.y(FaceTopology.NOSE_TIP) - midY) / scale).coerceIn(-1.2f, 1.2f)
    }

    fun build(styleId: String, lm: FaceLandmarks): HairGroomMesh {
        val recipe = HairGroomRecipes.forStyle(styleId)
        if (recipe.strandCount <= 0 || recipe.segments < 2) {
            return HairGroomMesh(FloatArray(0), IntArray(0))
        }
        if (lm.count < FaceTopology.LANDMARK_COUNT || !lm.allFinite()) {
            return HairGroomMesh(FloatArray(0), IntArray(0))
        }
        val scale = LandmarkSmoother.interOcular(lm)
        val yaw = yaw(lm)
        val pitch = pitch(lm)
        val forehead = Point(lm.x(FaceTopology.FOREHEAD), lm.y(FaceTopology.FOREHEAD))
        val chin = Point(lm.x(FaceTopology.CHIN), lm.y(FaceTopology.CHIN))
        val left = Point(lm.x(FaceTopology.LEFT_JAW), lm.y(FaceTopology.LEFT_JAW))
        val right = Point(lm.x(FaceTopology.RIGHT_JAW), lm.y(FaceTopology.RIGHT_JAW))
        val up = Point(forehead.x - chin.x, forehead.y - chin.y).normalized()
        val side = Point(right.x - left.x, right.y - left.y).normalized()
        val crown = Point(
            forehead.x + up.x * scale * (0.28f + recipe.volume * 0.22f) - side.x * yaw * scale * 0.12f,
            forehead.y + up.y * scale * (0.28f + recipe.volume * 0.22f) + pitch * scale * 0.08f,
        )
        val gather = Point(
            crown.x - up.x * scale * 0.05f - side.x * yaw * scale * 0.55f,
            crown.y - up.y * scale * 0.02f + 0.04f * scale,
        )
        val verts = ArrayList<Float>(recipe.strandCount * (recipe.segments + 1) * 2 * HairGroomMesh.STRIDE)
        val inds = ArrayList<Int>(recipe.strandCount * recipe.segments * 6)
        var base = 0
        for (s in 0 until recipe.strandCount) {
            val u = s / (recipe.strandCount - 1).toFloat()
            val hairline = hairlineRoot(lm, u, forehead, left, right)
            val path = strandPath(recipe, s, u, hairline, crown, gather, up, side, scale, yaw, pitch)
            if (path.size < 2) continue
            val halfW = recipe.width * (0.7f + 0.5f * (1f - abs(u - 0.5f) * 2f)) * (1f + recipe.volume * 0.25f)
            for (i in path.indices) {
                val p = path[i]
                val tangent = if (i + 1 < path.size) {
                    Point(path[i + 1].x - p.x, path[i + 1].y - p.y)
                } else {
                    Point(p.x - path[i - 1].x, p.y - path[i - 1].y)
                }.normalized()
                val n = Point(-tangent.y, tangent.x)
                val along = i / (path.size - 1).toFloat()
                emit(verts, p.x + n.x * halfW, p.y + n.y * halfW, along, 1f)
                emit(verts, p.x - n.x * halfW, p.y - n.y * halfW, along, -1f)
            }
            val rings = path.size
            for (i in 0 until rings - 1) {
                val i0 = base + i * 2
                inds.add(i0)
                inds.add(i0 + 1)
                inds.add(i0 + 2)
                inds.add(i0 + 1)
                inds.add(i0 + 3)
                inds.add(i0 + 2)
            }
            base += rings * 2
        }
        val mesh = HairGroomMesh(verts.toFloatArray(), inds.toIntArray())
        return if (mesh.allFinite()) mesh else HairGroomMesh(FloatArray(0), IntArray(0))
    }

    private fun hairlineRoot(
        lm: FaceLandmarks,
        u: Float,
        forehead: Point,
        left: Point,
        right: Point,
    ): Point {
        val oval = FaceTopology.FACE_OVAL
        val n = oval.size
        val t = u * (n - 1)
        val i0 = t.toInt().coerceIn(0, n - 2)
        val f = t - i0
        val a = oval[i0]
        val b = oval[i0 + 1]
        val px = lm.x(a) * (1f - f) + lm.x(b) * f
        val py = lm.y(a) * (1f - f) + lm.y(b) * f
        val towardForehead = Point(forehead.x - 0.5f * (left.x + right.x), forehead.y - 0.5f * (left.y + right.y))
        val blend = if (py < forehead.y + 0.08f) 0.65f else 0.2f
        return Point(px * (1f - blend) + forehead.x * blend + towardForehead.x * 0.02f, py * (1f - blend) + forehead.y * blend)
    }

    private fun strandPath(
        recipe: HairGroomRecipe,
        index: Int,
        u: Float,
        root: Point,
        crown: Point,
        gather: Point,
        up: Point,
        side: Point,
        scale: Float,
        yaw: Float,
        pitch: Float,
    ): List<Point> {
        val segs = recipe.segments
        val layerJitter = ((index * 17) % 11) / 10f
        val lengthMul = 1f - recipe.layers * 0.45f * layerJitter + recipe.asymmetry * (u - 0.5f) * 0.7f
        val pixieCut = 1f - recipe.pixie * 0.55f
        val len = recipe.length * scale * 3.4f * lengthMul.coerceIn(0.35f, 1.45f) * pixieCut
        val bangZone = abs(u - 0.5f) < 0.28f
        val bangLen = if (recipe.bangs > 0.01f && bangZone) {
            recipe.bangs * scale * 1.15f * (0.7f + 0.3f * cos((u - 0.5f) * PI.toFloat() * 6f))
        } else {
            0f
        }
        val curtainPull = recipe.curtain * (if (u < 0.5f) -1f else 1f) * (1f - abs(u - 0.5f) * 0.5f)
        val out = ArrayList<Point>(segs + 1)
        for (i in 0..segs) {
            val t = i / segs.toFloat()
            var x = root.x
            var y = root.y
            val growth = Point(
                -up.x * (0.35f + recipe.volume * 0.4f) + side.x * ((u - 0.5f) * 1.6f + yaw * 0.35f + curtainPull),
                -up.y * (0.15f - recipe.volume * 0.35f) + recipe.gravity * (0.55f + t) + pitch * 0.2f,
            )
            if (recipe.gather > 0.4f && t > 0.18f) {
                val g = ((t - 0.18f) / 0.82f).coerceIn(0f, 1f) * recipe.gather
                x = x * (1f - g) + gather.x * g
                y = y * (1f - g) + gather.y * g
            } else if (t < 0.22f) {
                val lift = t / 0.22f
                x = root.x * (1f - lift) + (root.x * 0.45f + crown.x * 0.55f) * lift
                y = root.y * (1f - lift) + (root.y * 0.35f + crown.y * 0.65f) * lift
            }
            val useLen = if (bangLen > 0f && t > 0.12f) {
                bangLen + len * (1f - recipe.bangs) * t
            } else {
                len * t
            }
            x += growth.x * useLen * t
            y += growth.y * useLen * t * 0.85f + t * t * recipe.gravity * scale * 1.2f
            if (recipe.bun > 0.3f && t > 0.35f) {
                val ang = (t - 0.35f) * 6.4f + u * 6.28f + index * 0.2f
                val r = recipe.bun * scale * 0.42f * (1f - (t - 0.35f))
                x = gather.x + cos(ang) * r
                y = gather.y + sin(ang) * r * 0.72f - up.y * scale * 0.04f
            }
            if (recipe.braid > 0.3f && t > 0.28f) {
                val phase = t * 14f + (index % 3) * 2.094f
                x += cos(phase) * scale * 0.07f * recipe.braid
                y += sin(phase * 0.5f) * scale * 0.03f * recipe.braid
            }
            val shag = recipe.layers * sin(index * 1.7f + t * 9f) * scale * 0.04f
            x += shag + side.x * recipe.asymmetry * (u - 0.35f) * t * scale * 0.4f
            y += abs(shag) * 0.4f
            x = x.coerceIn(-0.15f, 1.15f)
            y = y.coerceIn(-0.15f, 1.15f)
            if (!x.isFinite() || !y.isFinite()) continue
            out.add(Point(x, y))
        }
        return out
    }

    private fun emit(verts: ArrayList<Float>, u: Float, v: Float, along: Float, across: Float) {
        val uu = u.coerceIn(-0.2f, 1.2f)
        val vv = v.coerceIn(-0.2f, 1.2f)
        verts.add(toNdcX(uu))
        verts.add(toNdcY(vv))
        verts.add(uu)
        verts.add(vv)
        verts.add(along)
        verts.add(across)
    }

    private data class Point(val x: Float, val y: Float) {
        fun normalized(): Point {
            val l = hypot(x, y)
            return if (l < 1e-5f) Point(0f, -1f) else Point(x / l, y / l)
        }
    }
}
