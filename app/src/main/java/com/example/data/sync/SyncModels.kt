package com.example.data.sync

import com.example.data.model.BrushType
import com.example.data.model.DrawingPoint
import com.example.data.model.DrawingStroke
import com.example.data.model.PlacedSticker
import com.example.data.model.WallpaperTheme
import org.json.JSONArray
import org.json.JSONObject

sealed class SyncAction {
    data class StrokeBegin(
        val strokeId: String,
        val x: Float,
        val y: Float,
        val colorArgb: Int,
        val strokeWidth: Float,
        val brushType: BrushType,
        val alpha: Float,
        val authorId: String,
        val modifier: com.example.data.model.StrokeModifier = com.example.data.model.StrokeModifier.NONE
    ) : SyncAction()

    data class StrokePoints(
        val strokeId: String,
        val points: List<DrawingPoint>,
        val authorId: String
    ) : SyncAction()

    data class StrokeFinished(
        val stroke: DrawingStroke
    ) : SyncAction()

    data class PlaceSticker(
        val sticker: PlacedSticker
    ) : SyncAction()

    data class UpdateSticker(
        val sticker: PlacedSticker
    ) : SyncAction()

    data class RemoveSticker(
        val stickerId: String,
        val authorId: String
    ) : SyncAction()

    data object ClearAll : SyncAction()

    data class Undo(
        val authorId: String
    ) : SyncAction()

    data class HeartPing(
        val x: Float,
        val y: Float,
        val emoji: String = "💖",
        val sender: String
    ) : SyncAction()

    data class CursorMove(
        val x: Float,
        val y: Float,
        val sender: String
    ) : SyncAction()

    data class ChangeWallpaper(
        val wallpaperTheme: WallpaperTheme,
        val customUri: String? = null
    ) : SyncAction()

    data class RequestSnapshot(
        val requesterId: String
    ) : SyncAction()

    data class UserLayerSnapshot(
        val authorId: String,
        val strokes: List<DrawingStroke>,
        val stickers: List<PlacedSticker>,
        val layerVersion: Long,
        val updatedAt: Long,
        val wallpaperTheme: WallpaperTheme? = null
    ) : SyncAction()

    data class ClearUserLayer(
        val authorId: String
    ) : SyncAction()
}

object SyncActionSerializer {
    fun toJson(action: SyncAction): String {
        val json = JSONObject()
        when (action) {
            is SyncAction.StrokeBegin -> {
                json.put("type", "STROKE_BEGIN")
                json.put("strokeId", action.strokeId)
                json.put("x", action.x.toDouble())
                json.put("y", action.y.toDouble())
                json.put("colorArgb", action.colorArgb)
                json.put("strokeWidth", action.strokeWidth.toDouble())
                json.put("brushType", action.brushType.name)
                json.put("alpha", action.alpha.toDouble())
                json.put("authorId", action.authorId)
                json.put("modifier", action.modifier.name)
            }
            is SyncAction.StrokePoints -> {
                json.put("type", "STROKE_POINTS")
                json.put("strokeId", action.strokeId)
                json.put("authorId", action.authorId)
                val sb = StringBuilder()
                action.points.forEachIndexed { index, p ->
                    if (index > 0) sb.append(';')
                    val ix = (p.x * 1000).toInt()
                    val iy = (p.y * 1000).toInt()
                    val ip = (p.pressure * 100).toInt()
                    sb.append(ix).append(',').append(iy).append(',').append(ip)
                }
                json.put("pts", sb.toString())
            }
            is SyncAction.StrokeFinished -> {
                json.put("type", "STROKE_FINISHED")
                val s = action.stroke
                json.put("strokeId", s.id)
                json.put("colorArgb", s.colorArgb)
                json.put("strokeWidth", s.strokeWidth.toDouble())
                json.put("brushType", s.brushType.name)
                json.put("alpha", s.alpha.toDouble())
                json.put("authorId", s.authorId)
                json.put("modifier", s.modifier.name)
                json.put("createdAt", s.createdAt)
                val sb = StringBuilder()
                s.points.forEachIndexed { index, p ->
                    if (index > 0) sb.append(';')
                    val ix = (p.x * 1000).toInt()
                    val iy = (p.y * 1000).toInt()
                    val ip = (p.pressure * 100).toInt()
                    sb.append(ix).append(',').append(iy).append(',').append(ip)
                }
                json.put("pts", sb.toString())
            }
            is SyncAction.PlaceSticker -> {
                json.put("type", "PLACE_STICKER")
                val st = action.sticker
                json.put("id", st.id)
                json.put("content", st.content)
                json.put("x", st.x.toDouble())
                json.put("y", st.y.toDouble())
                json.put("scale", st.scale.toDouble())
                json.put("rotation", st.rotation.toDouble())
                json.put("authorId", st.authorId)
                json.put("createdAt", st.createdAt)
            }
            is SyncAction.UpdateSticker -> {
                json.put("type", "UPDATE_STICKER")
                val st = action.sticker
                json.put("id", st.id)
                json.put("content", st.content)
                json.put("x", st.x.toDouble())
                json.put("y", st.y.toDouble())
                json.put("scale", st.scale.toDouble())
                json.put("rotation", st.rotation.toDouble())
                json.put("authorId", st.authorId)
                json.put("createdAt", st.createdAt)
            }
            is SyncAction.RemoveSticker -> {
                json.put("type", "REMOVE_STICKER")
                json.put("stickerId", action.stickerId)
                json.put("authorId", action.authorId)
            }
            is SyncAction.ClearAll -> {
                json.put("type", "CLEAR_ALL")
            }
            is SyncAction.Undo -> {
                json.put("type", "UNDO")
                json.put("authorId", action.authorId)
            }
            is SyncAction.HeartPing -> {
                json.put("type", "HEART_PING")
                json.put("x", action.x.toDouble())
                json.put("y", action.y.toDouble())
                json.put("emoji", action.emoji)
                json.put("sender", action.sender)
            }
            is SyncAction.CursorMove -> {
                json.put("type", "CURSOR_MOVE")
                json.put("x", action.x.toDouble())
                json.put("y", action.y.toDouble())
                json.put("sender", action.sender)
            }
            is SyncAction.ChangeWallpaper -> {
                json.put("type", "CHANGE_WALLPAPER")
                json.put("wallpaperTheme", action.wallpaperTheme.name)
                action.customUri?.let { json.put("customUri", it) }
            }
            is SyncAction.RequestSnapshot -> {
                json.put("type", "REQUEST_SNAPSHOT")
                json.put("requesterId", action.requesterId)
            }
            is SyncAction.UserLayerSnapshot -> {
                json.put("type", "USER_LAYER_SNAPSHOT")
                json.put("authorId", action.authorId)
                json.put("layerVersion", action.layerVersion)
                json.put("updatedAt", action.updatedAt)
                action.wallpaperTheme?.let { json.put("wallpaperTheme", it.name) }

                val strokesArr = JSONArray()
                action.strokes.forEach { s ->
                    val sObj = JSONObject()
                    sObj.put("strokeId", s.id)
                    sObj.put("colorArgb", s.colorArgb)
                    sObj.put("strokeWidth", s.strokeWidth.toDouble())
                    sObj.put("brushType", s.brushType.name)
                    sObj.put("alpha", s.alpha.toDouble())
                    sObj.put("authorId", s.authorId)
                    sObj.put("modifier", s.modifier.name)
                    sObj.put("createdAt", s.createdAt)
                    val sb = StringBuilder()
                    s.points.forEachIndexed { index, p ->
                        if (index > 0) sb.append(';')
                        val ix = (p.x * 1000).toInt()
                        val iy = (p.y * 1000).toInt()
                        val ip = (p.pressure * 100).toInt()
                        sb.append(ix).append(',').append(iy).append(',').append(ip)
                    }
                    sObj.put("pts", sb.toString())
                    strokesArr.put(sObj)
                }
                json.put("strokes", strokesArr)

                val stArr = JSONArray()
                action.stickers.forEach { st ->
                    val stObj = JSONObject()
                    stObj.put("id", st.id)
                    stObj.put("content", st.content)
                    stObj.put("x", st.x.toDouble())
                    stObj.put("y", st.y.toDouble())
                    stObj.put("scale", st.scale.toDouble())
                    stObj.put("rotation", st.rotation.toDouble())
                    stObj.put("authorId", st.authorId)
                    stObj.put("createdAt", st.createdAt)
                    stArr.put(stObj)
                }
                json.put("stickers", stArr)
            }
            is SyncAction.ClearUserLayer -> {
                json.put("type", "CLEAR_USER_LAYER")
                json.put("authorId", action.authorId)
            }
        }
        return json.toString()
    }

    private fun parsePoints(json: JSONObject): List<DrawingPoint> {
        val pts = mutableListOf<DrawingPoint>()
        if (json.has("pts")) {
            val raw = json.optString("pts", "")
            if (raw.isNotEmpty()) {
                val tokens = raw.split(';')
                for (token in tokens) {
                    val parts = token.split(',')
                    if (parts.size >= 2) {
                        val x = (parts[0].toFloatOrNull() ?: 0f) / 1000f
                        val y = (parts[1].toFloatOrNull() ?: 0f) / 1000f
                        val p = if (parts.size >= 3) (parts[2].toFloatOrNull() ?: 100f) / 100f else 1.0f
                        pts.add(DrawingPoint(x, y, p))
                    }
                }
            }
        } else if (json.has("points")) {
            val ptsArr = json.optJSONArray("points") ?: JSONArray()
            for (j in 0 until ptsArr.length()) {
                val pObj = ptsArr.getJSONObject(j)
                pts.add(
                    DrawingPoint(
                        x = pObj.getDouble("x").toFloat(),
                        y = pObj.getDouble("y").toFloat(),
                        pressure = pObj.optDouble("pressure", 1.0).toFloat()
                    )
                )
            }
        }
        return pts
    }

    fun fromJson(jsonString: String): SyncAction? {
        return try {
            val json = JSONObject(jsonString)
            when (json.optString("type")) {
                "REQUEST_SNAPSHOT" -> {
                    SyncAction.RequestSnapshot(
                        requesterId = json.optString("requesterId", "")
                    )
                }
                "USER_LAYER_SNAPSHOT", "FULL_SNAPSHOT" -> {
                    val themeName = if (json.has("wallpaperTheme")) json.optString("wallpaperTheme") else null
                    val theme = themeName?.let {
                        try { WallpaperTheme.valueOf(it) } catch (e: Exception) { null }
                    }
                    val strokesArr = json.optJSONArray("strokes") ?: JSONArray()
                    val strokesList = mutableListOf<DrawingStroke>()
                    for (i in 0 until strokesArr.length()) {
                        val sObj = strokesArr.getJSONObject(i)
                        val pts = parsePoints(sObj)
                        val mod = try { com.example.data.model.StrokeModifier.valueOf(sObj.optString("modifier", "NONE")) } catch (e: Exception) { com.example.data.model.StrokeModifier.NONE }
                        strokesList.add(
                            DrawingStroke(
                                id = sObj.getString("strokeId"),
                                points = pts,
                                colorArgb = sObj.getInt("colorArgb"),
                                strokeWidth = sObj.getDouble("strokeWidth").toFloat(),
                                brushType = try { BrushType.valueOf(sObj.optString("brushType", "PEN")) } catch (e: Exception) { BrushType.PEN },
                                authorId = sObj.optString("authorId", "partner"),
                                alpha = sObj.optDouble("alpha", 1.0).toFloat(),
                                modifier = mod,
                                createdAt = sObj.optLong("createdAt", sObj.optLong("ts", System.currentTimeMillis()))
                            )
                        )
                    }

                    val stArr = json.optJSONArray("stickers") ?: JSONArray()
                    val stickersList = mutableListOf<PlacedSticker>()
                    for (i in 0 until stArr.length()) {
                        val stObj = stArr.getJSONObject(i)
                        stickersList.add(
                            PlacedSticker(
                                id = stObj.getString("id"),
                                content = stObj.getString("content"),
                                x = stObj.getDouble("x").toFloat(),
                                y = stObj.getDouble("y").toFloat(),
                                scale = stObj.optDouble("scale", 1.0).toFloat(),
                                rotation = stObj.optDouble("rotation", 0.0).toFloat(),
                                authorId = stObj.optString("authorId", "partner"),
                                createdAt = stObj.optLong("createdAt", stObj.optLong("ts", System.currentTimeMillis()))
                            )
                        )
                    }

                    SyncAction.UserLayerSnapshot(
                        authorId = json.optString("authorId", "partner"),
                        strokes = strokesList,
                        stickers = stickersList,
                        layerVersion = json.optLong("layerVersion", 1L),
                        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                        wallpaperTheme = theme
                    )
                }
                "CLEAR_USER_LAYER" -> {
                    SyncAction.ClearUserLayer(
                        authorId = json.optString("authorId", "partner")
                    )
                }
                "STROKE_BEGIN" -> {
                    val mod = try { com.example.data.model.StrokeModifier.valueOf(json.optString("modifier", "NONE")) } catch (e: Exception) { com.example.data.model.StrokeModifier.NONE }
                    SyncAction.StrokeBegin(
                        strokeId = json.getString("strokeId"),
                        x = json.getDouble("x").toFloat(),
                        y = json.getDouble("y").toFloat(),
                        colorArgb = json.getInt("colorArgb"),
                        strokeWidth = json.getDouble("strokeWidth").toFloat(),
                        brushType = BrushType.valueOf(json.optString("brushType", "PEN")),
                        alpha = json.optDouble("alpha", 1.0).toFloat(),
                        authorId = json.optString("authorId", "partner"),
                        modifier = mod
                    )
                }
                "STROKE_POINTS" -> {
                    val pts = parsePoints(json)
                    SyncAction.StrokePoints(
                        strokeId = json.getString("strokeId"),
                        points = pts,
                        authorId = json.optString("authorId", "partner")
                    )
                }
                "STROKE_FINISHED" -> {
                    val pts = parsePoints(json)
                    val mod = try { com.example.data.model.StrokeModifier.valueOf(json.optString("modifier", "NONE")) } catch (e: Exception) { com.example.data.model.StrokeModifier.NONE }
                    val stroke = DrawingStroke(
                        id = json.getString("strokeId"),
                        points = pts,
                        colorArgb = json.getInt("colorArgb"),
                        strokeWidth = json.getDouble("strokeWidth").toFloat(),
                        brushType = BrushType.valueOf(json.optString("brushType", "PEN")),
                        authorId = json.optString("authorId", "partner"),
                        alpha = json.optDouble("alpha", 1.0).toFloat(),
                        modifier = mod,
                        createdAt = json.optLong("createdAt", json.optLong("ts", System.currentTimeMillis()))
                    )
                    SyncAction.StrokeFinished(stroke)
                }
                "PLACE_STICKER" -> {
                    val sticker = PlacedSticker(
                        id = json.getString("id"),
                        content = json.getString("content"),
                        x = json.getDouble("x").toFloat(),
                        y = json.getDouble("y").toFloat(),
                        scale = json.optDouble("scale", 1.0).toFloat(),
                        rotation = json.optDouble("rotation", 0.0).toFloat(),
                        authorId = json.optString("authorId", "partner"),
                        createdAt = json.optLong("createdAt", json.optLong("ts", System.currentTimeMillis()))
                    )
                    SyncAction.PlaceSticker(sticker)
                }
                "UPDATE_STICKER" -> {
                    val sticker = PlacedSticker(
                        id = json.getString("id"),
                        content = json.getString("content"),
                        x = json.getDouble("x").toFloat(),
                        y = json.getDouble("y").toFloat(),
                        scale = json.optDouble("scale", 1.0).toFloat(),
                        rotation = json.optDouble("rotation", 0.0).toFloat(),
                        authorId = json.optString("authorId", "partner"),
                        createdAt = json.optLong("createdAt", json.optLong("ts", System.currentTimeMillis()))
                    )
                    SyncAction.UpdateSticker(sticker)
                }
                "REMOVE_STICKER" -> {
                    SyncAction.RemoveSticker(
                        stickerId = json.getString("stickerId"),
                        authorId = json.optString("authorId", "partner")
                    )
                }
                "CLEAR_ALL" -> SyncAction.ClearAll
                "UNDO" -> SyncAction.Undo(authorId = json.optString("authorId", "partner"))
                "HEART_PING" -> {
                    SyncAction.HeartPing(
                        x = json.getDouble("x").toFloat(),
                        y = json.getDouble("y").toFloat(),
                        emoji = json.optString("emoji", "💖"),
                        sender = json.optString("sender", "Partner")
                    )
                }
                "CURSOR_MOVE" -> {
                    SyncAction.CursorMove(
                        x = json.getDouble("x").toFloat(),
                        y = json.getDouble("y").toFloat(),
                        sender = json.optString("sender", "Partner")
                    )
                }
                "CHANGE_WALLPAPER" -> {
                    SyncAction.ChangeWallpaper(
                        wallpaperTheme = WallpaperTheme.valueOf(json.optString("wallpaperTheme", "DEEP_PURPLE")),
                        customUri = if (json.has("customUri")) json.getString("customUri") else null
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
