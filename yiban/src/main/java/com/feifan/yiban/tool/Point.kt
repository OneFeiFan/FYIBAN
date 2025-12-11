package com.feifan.yiban.tool

import com.alibaba.fastjson.JSONArray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

// 坐标数据类
data class Point(val lng: Double, val lat: Double) {
    override fun toString(): String {
        return "$lng,$lat"
    }
}

// 转换数据为数值数组
fun parseCoordinates(data: JSONArray): List<Point> {
    return data.map {
        it as String
        val parts = it.split(",")
        Point(parts[0].toDouble(), parts[1].toDouble())
    }
}

// 边界数据类
data class Bounds(val minLng: Double, val maxLng: Double, val minLat: Double, val maxLat: Double)

// 计算坐标边界
fun getBounds(coords: List<Point>): Bounds {
    val lngs = coords.map { it.lng }
    val lats = coords.map { it.lat }
    return Bounds(
        lngs.minOrNull()!!,
        lngs.maxOrNull()!!,
        lats.minOrNull()!!,
        lats.maxOrNull()!!
    )
}

// 计算多边形的几何中心
fun calculateCentroid(points: List<Point>): Point {
    var sumX = 0.0
    var sumY = 0.0
    for (point in points) {
        sumX += point.lng
        sumY += point.lat
    }
    return Point(sumX / points.size, sumY / points.size)
}

// 正态分布随机数生成
fun normalRandom(scale: Double = 1.0): Double {
    var u = 0.0
    var v = 0.0
    while (u == 0.0) u = Random.nextDouble()
    while (v == 0.0) v = Random.nextDouble()
    return sqrt(-2.0 * ln(u)) * cos(2.0 * PI * v) * scale
}

// 生成收缩后的多边形
fun createScaledPolygon(
    originalPoints: List<Point>,
    centroid: Point,
    scaleFactor: Double = 0.7
): List<Point> {
    return originalPoints.map { p ->
        val dx = p.lng - centroid.lng
        val dy = p.lat - centroid.lat
        Point(
            centroid.lng + dx * scaleFactor,
            centroid.lat + dy * scaleFactor
        )
    }
}

// 判断点是否在多边形内部
fun isPointInPolygon(point: Point, polygon: List<Point>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]

        val intersect = (pi.lat > point.lat) != (pj.lat > point.lat) &&
                (point.lng < (pj.lng - pi.lng) * (point.lat - pi.lat) / (pj.lat - pi.lat) + pi.lng)

        if (intersect) inside = !inside
        j = i
    }
    return inside
}

// 生成随机点逻辑
fun generateRandomPointInCenter(
    centroid: Point,
    bounds: Bounds,
    scaledPolygon: List<Point>,
    originalPolygon: List<Point>
): Point {
    val stdDev = 0.2
    var point: Point
    var attempts = 0
    val maxAttempts = 100

    do {
        val offsetLng = normalRandom(bounds.maxLng - bounds.minLng) * stdDev
        val offsetLat = normalRandom(bounds.maxLat - bounds.minLat) * stdDev

        point = Point(
            centroid.lng + offsetLng,
            centroid.lat + offsetLat
        )
        attempts++
    } while (attempts < maxAttempts &&
        (!isPointInPolygon(point, scaledPolygon) || !isPointInPolygon(point, originalPolygon))
    )

    return point
}

// 主函数
fun generateRandomPointsInCenter(data: JSONArray): Point {
    val coords = parseCoordinates(data)
    val bounds = getBounds(coords)
    val centroid = calculateCentroid(coords)

    // 创建缩小后的多边形
    val scaledPolygon = createScaledPolygon(coords, centroid, 0.7)

    return generateRandomPointInCenter(
        centroid,
        bounds,
        scaledPolygon,
        coords
    )
}