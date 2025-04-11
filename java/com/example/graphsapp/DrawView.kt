package com.example.graphsapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import kotlin.math.pow
import kotlin.math.sqrt

class DrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Основные данные графа
    val vertices = mutableListOf<PointF>()
    private var edges = mutableListOf<Pair<Int, Int>>()
    private var edgeWeights = mutableMapOf<Pair<Int, Int>, Int>()
    private val highlightedEdges = mutableListOf<Pair<Int, Int>>()
    private val highlightedVertices = mutableListOf<Int>()
    private var startVertexIndex: Int? = null
    private var currentEdge: Pair<PointF, PointF>? = null
    private var isDrawingEnabled = true
    private var isLongPressHandled = false
    private var vertexColors = mutableMapOf<Int, Int>()
    private var lastAlgorithmPath: List<Int>? = null
    private var lastAlgorithmType: String? = null
    private var lastColoring: Map<Int, Int>? = null
    private var animationRunning = false

    // Стили отрисовки
    private val vertexPaint = Paint().apply {
        color = Color.parseColor("#03A9F4")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val edgePaint = Paint().apply {
        color = Color.parseColor("#00FFFF")
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val vertexHighlightPaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val edgeHighlightPaint = Paint().apply {
        color = Color.parseColor("#FF5722")
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val weightPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val tempEdgePaint = Paint().apply {
        color = Color.parseColor("#03A9F4")
        strokeWidth = 8f
        alpha = 150
        isAntiAlias = true
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            isLongPressHandled = false
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            isLongPressHandled = true
            findVertexAt(e.x, e.y)?.let { index ->
                removeVertex(index)
                onRemoveVertexListener?.invoke(index)
            }

            findEdgeAt(e.x, e.y)?.let { edge ->
                onEdgeClickListener?.invoke(edge)
            }
        }
    })

    // Callback-функции
    var onDrawListener: ((Int, Int) -> Unit)? = null
    var onRemoveVertexListener: ((Int) -> Unit)? = null
    var onRemoveEdgeListener: ((Int, Int) -> Unit)? = null
    var onEdgeClickListener: ((Pair<Int, Int>) -> Unit)? = null

    fun enableDrawing(enable: Boolean) {
        isDrawingEnabled = enable
    }

    fun setLastAlgorithmData(path: List<Int>?, algorithmType: String?, coloring: Map<Int, Int>? = null) {
        this.lastAlgorithmPath = path
        this.lastAlgorithmType = algorithmType
        this.lastColoring = coloring
        invalidate()
    }

    fun animateAlgorithm(handler: Handler = Handler(Looper.getMainLooper()), onComplete: () -> Unit = {}) {
        if (animationRunning) return

        animationRunning = true
        highlightPath(emptyList())

        when (lastAlgorithmType) {
            "GREEDY_COLORING", "SEQUENTIAL_COLORING" -> {
                lastColoring?.let { coloring ->
                    animateColoring(coloring.keys.toList(), coloring, handler, {
                        animationRunning = false
                        onComplete()
                    })
                } ?: run {
                    animationRunning = false
                    onComplete()
                }
            }
            else -> {
                lastAlgorithmPath?.let { path ->
                    animatePath(path, handler, {
                        animationRunning = false
                        onComplete()
                    })
                } ?: run {
                    animationRunning = false
                    onComplete()
                }
            }
        }
    }

    private fun animatePath(path: List<Int>, handler: Handler, onComplete: () -> Unit) {
        var currentStep = 0
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep < path.size) {
                    highlightPath(path.take(currentStep + 1))
                    invalidate()
                    currentStep++
                    handler.postDelayed(this, 1000)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(runnable)
    }

    private fun animateColoring(path: List<Int>, coloring: Map<Int, Int>, handler: Handler, onComplete: () -> Unit) {
        var currentStep = 0
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep < path.size) {
                    vertexColors[path[currentStep]] = coloring[path[currentStep]] ?: Color.BLUE
                    highlightPath(listOf(path[currentStep]))
                    invalidate()
                    currentStep++
                    handler.postDelayed(this, 1000)
                } else {
                    onComplete()
                }
            }
        }
        handler.post(runnable)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Рисуем ребра - всегда фиксированным цветом
        edges.forEach { (v1, v2) ->
            if (v1 in vertices.indices && v2 in vertices.indices) {
                val paint = if (highlightedEdges.contains(v1 to v2) || highlightedEdges.contains(v2 to v1)) {
                    edgeHighlightPaint
                } else {
                    edgePaint
                }
                canvas.drawLine(vertices[v1].x, vertices[v1].y, vertices[v2].x, vertices[v2].y, paint)

                // Вес ребра
                val weight = edgeWeights[v1 to v2] ?: 1
                val midX = (vertices[v1].x + vertices[v2].x) / 2
                val midY = (vertices[v1].y + vertices[v2].y) / 2
                canvas.drawText(weight.toString(), midX, midY, weightPaint)
            }
        }

        // Текущее перетаскиваемое ребро
        currentEdge?.let { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, tempEdgePaint)
        }

        // Рисуем вершины
        vertices.forEachIndexed { index, vertex ->
            val color = vertexColors[index] ?: if (highlightedVertices.contains(index)) {
                vertexHighlightPaint.color
            } else {
                vertexPaint.color
            }

            val paint = if (highlightedVertices.contains(index)) {
                vertexHighlightPaint.apply { this.color = color }
            } else {
                vertexPaint.apply { this.color = color }
            }

            canvas.drawCircle(vertex.x, vertex.y, 45f, paint)
            canvas.drawText(index.toString(), vertex.x, vertex.y + 15, textPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) return false

        gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchDown(event.x, event.y)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> handleTouchUp(event.x, event.y)
        }
        return true
    }

    private fun handleTouchDown(x: Float, y: Float) {
        findVertexAt(x, y)?.let { index ->
            startVertexIndex = index
            currentEdge = vertices[index] to PointF(x, y)
        } ?: run {
            if (!isLongPressHandled) {
                addVertex(x, y)
            }
        }
        invalidate()
    }

    private fun handleTouchMove(x: Float, y: Float) {
        startVertexIndex?.let { index ->
            currentEdge = vertices[index] to PointF(x, y)
            invalidate()
        }
    }

    private fun handleTouchUp(x: Float, y: Float) {
        startVertexIndex?.let { startIndex ->
            findVertexAt(x, y)?.let { endIndex ->
                if (startIndex != endIndex) {
                    addEdge(startIndex, endIndex)
                    onDrawListener?.invoke(startIndex, endIndex)
                }
            }
            startVertexIndex = null
            currentEdge = null
            invalidate()
        }
    }

    fun addVertex(x: Float, y: Float) {
        vertices.add(PointF(x, y))
        invalidate()
    }

    fun addEdge(v1: Int, v2: Int, weight: Int = 1) {
        if (v1 in vertices.indices && v2 in vertices.indices) {
            if (!edges.contains(v1 to v2) && !edges.contains(v2 to v1)) {
                edges.add(v1 to v2)
                edgeWeights[v1 to v2] = weight
                edgeWeights[v2 to v1] = weight
                invalidate()
            }
        }
    }

    fun removeEdge(v1: Int, v2: Int) {
        edges.removeAll { (first, second) ->
            (first == v1 && second == v2) || (first == v2 && second == v1)
        }
        edgeWeights.remove(v1 to v2)
        edgeWeights.remove(v2 to v1)
        onRemoveEdgeListener?.invoke(v1, v2)
        invalidate()
    }

    fun setEdgeWeight(v1: Int, v2: Int, weight: Int) {
        if (edges.contains(v1 to v2) || edges.contains(v2 to v1)) {
            edgeWeights[v1 to v2] = weight
            edgeWeights[v2 to v1] = weight
            invalidate()
        }
    }

    fun highlightPath(path: List<Int>) {
        highlightedVertices.clear()
        highlightedEdges.clear()

        if (path.isNotEmpty()) {
            highlightedVertices.addAll(path.filter { it in vertices.indices })

            for (i in 0 until path.size - 1) {
                if (path[i] in vertices.indices && path[i + 1] in vertices.indices) {
                    highlightedEdges.add(path[i] to path[i + 1])
                }
            }
        }
        invalidate()
    }

    fun loadGraph(graph: Graph) {
        clear()

        graph.vertices.forEach { vertex ->
            graph.getVertexPosition(vertex)?.let { (x, y) ->
                vertices.add(PointF(x, y))
            } ?: run {
                vertices.add(PointF(
                    (100 + (0..1000).random()).toFloat(),
                    (100 + (0..500).random()).toFloat()
                ))
            }
        }

        graph.edges.forEach { (v1, v2) ->
            if (v1 in vertices.indices && v2 in vertices.indices) {
                edges.add(v1 to v2)
                edgeWeights[v1 to v2] = graph.weights[v1 to v2] ?: 1
                edgeWeights[v2 to v1] = graph.weights[v2 to v1] ?: 1
            }
        }

        invalidate()
    }

    fun clear() {
        vertices.clear()
        edges.clear()
        edgeWeights.clear()
        highlightedEdges.clear()
        highlightedVertices.clear()
        vertexColors.clear()
        lastAlgorithmPath = null
        lastAlgorithmType = null
        lastColoring = null
        invalidate()
    }

    private fun findVertexAt(x: Float, y: Float): Int? {
        vertices.forEachIndexed { index, vertex ->
            if (sqrt((vertex.x - x).pow(2) + (vertex.y - y).pow(2)) <= 45f) {
                return index
            }
        }
        return null
    }

    private fun findEdgeAt(x: Float, y: Float): Pair<Int, Int>? {
        edges.forEach { (v1, v2) ->
            if (v1 in vertices.indices && v2 in vertices.indices) {
                if (pointToLineDistance(x, y, vertices[v1].x, vertices[v1].y, vertices[v2].x, vertices[v2].y) <= 20f) {
                    return v1 to v2
                }
            }
        }
        return null
    }

    private fun pointToLineDistance(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val lineLength = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
        if (lineLength == 0f) return sqrt((px - x1).pow(2) + (py - y1).pow(2))

        val t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / (lineLength.pow(2))
        val tClamped = t.coerceIn(0f, 1f)
        val projectionX = x1 + tClamped * (x2 - x1)
        val projectionY = y1 + tClamped * (y2 - y1)

        return sqrt((px - projectionX).pow(2) + (py - projectionY).pow(2))
    }

    fun removeVertex(index: Int) {
        if (index !in vertices.indices) return

        edges.removeAll { (v1, v2) -> v1 == index || v2 == index }
        edgeWeights.keys.removeAll { (v1, v2) -> v1 == index || v2 == index }
        vertices.removeAt(index)

        edges = edges.map { (v1, v2) ->
            (if (v1 > index) v1 - 1 else v1) to (if (v2 > index) v2 - 1 else v2)
        }.toMutableList()

        val newWeights = mutableMapOf<Pair<Int, Int>, Int>()
        edgeWeights.forEach { (edge, weight) ->
            val newEdge = (if (edge.first > index) edge.first - 1 else edge.first) to
                    (if (edge.second > index) edge.second - 1 else edge.second)
            if (newEdge.first >= 0 && newEdge.second >= 0) {
                newWeights[newEdge] = weight
            }
        }
        edgeWeights = newWeights

        invalidate()
    }

    fun createThumbnail(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return Bitmap.createScaledBitmap(bitmap, 200, 200, true)
    }

    fun setColoring(coloring: Map<Int, Int>) {
        vertexColors.clear()
        vertexColors.putAll(coloring)
        invalidate()
    }

    fun resetEdgeColors() {
        edgePaint.color = Color.parseColor("#00FFFF")
        edgeHighlightPaint.color = Color.parseColor("#FF5722")
        invalidate()
    }

    fun stopAnimation() {
        animationRunning = false
    }
}