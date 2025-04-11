package com.example.graphsapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.Serializable

data class SavedGraph(
    val vertices: List<Pair<Float, Float>>,
    val edges: List<Pair<Int, Int>>,
    val weights: Map<Pair<Int, Int>, Int>,
    val thumbnailBase64: String
) : Serializable{
    companion object {
        fun fromGraph(vertices: List<PointF>, edges: List<Pair<Int, Int>>,
                      weights: Map<Pair<Int, Int>, Int>, thumbnail: Bitmap): SavedGraph {
            val byteArrayOutputStream = ByteArrayOutputStream()
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val thumbnailBase64 = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

            return SavedGraph(
                vertices = vertices.map { it.x to it.y },
                edges = edges,
                weights = weights,
                thumbnailBase64 = thumbnailBase64
            )
        }

        fun toBitmap(thumbnailBase64: String): Bitmap {
            val decodedBytes = Base64.decode(thumbnailBase64, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
    }
}