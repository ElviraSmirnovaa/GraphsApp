package com.example.graphsapp

import android.annotation.SuppressLint
import android.app.Activity
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
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
    var isDirected = false


    // Стили отрисовки
    private val vertexPaint = Paint().apply {
        color = Color.parseColor("#03A9F4")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun toggleDirectedMode() {
        isDirected = !isDirected
        // При переключении режима очистим текущие ребра и пересоздадим их
        val tempEdges = edges.toList()
        val tempWeights = edgeWeights.toMap()
        edges.clear()
        edgeWeights.clear()

        (context as? Activity)?.runOnUiThread {
            (context as? MainActivity)?.updateToggleButtonIcon(isDirected)
        }

        tempEdges.forEach { edge ->
            if (isDirected) {
                // В ориентированном режиме сохраняем только одно направление
                edges.add(edge)
                edgeWeights[edge] = tempWeights[edge] ?: 1
            } else {
                // В неориентированном режиме добавляем оба направления
                edges.add(edge)
                edges.add(edge.second to edge.first)
                edgeWeights[edge] = tempWeights[edge] ?: 1
                edgeWeights[edge.second to edge.first] = tempWeights[edge] ?: 1
            }
        }
        invalidate()
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

        // Оптимизированные параметры стрелок
        val ARROW_SIZE = 50f  // Размер "крыльев" (уменьшен с 60f)
        val ARROW_LENGTH = 40f  // Длина стрелки (уменьшен с 45f)
        val VERTEX_RADIUS = 45f
        val EDGE_WIDTH = 8f
        val ARROW_ANGLE = Math.PI/6  // Угол раскрытия стрелки (уже чем PI/6)

        // 1. Рисуем постоянные ребра
        edges.forEach { (v1, v2) ->
            if (v1 in vertices.indices && v2 in vertices.indices) {
                val start = vertices[v1]
                val end = vertices[v2]

                val dx = end.x - start.x
                val dy = end.y - start.y
                val length = sqrt(dx * dx + dy * dy)

                if (length > VERTEX_RADIUS * 2) {  // Минимальная длина ребра
                    val unitDx = dx / length
                    val unitDy = dy / length

                    // Корректируем точки для идеального соединения
                    val lineStart = PointF(
                        start.x + unitDx * VERTEX_RADIUS,
                        start.y + unitDy * VERTEX_RADIUS
                    )
                    val lineEnd = PointF(
                        end.x - unitDx * VERTEX_RADIUS,
                        end.y - unitDy * VERTEX_RADIUS
                    )

                    // Стиль ребра
                    val paint = when {
                        highlightedEdges.contains(v1 to v2) -> edgeHighlightPaint
                        !isDirected && highlightedEdges.contains(v2 to v1) -> edgeHighlightPaint
                        else -> edgePaint
                    }.apply {
                        strokeWidth = if (this == edgeHighlightPaint) 12f else EDGE_WIDTH
                    }

                    // Рисуем идеально подогнанную линию
                    canvas.drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y, paint)

                    // Ориентированные стрелки
                    if (isDirected) {
                        // Точка прикрепления стрелки (ровно на конце линии)
                        val arrowTip = PointF(lineEnd.x, lineEnd.y)

                        // Основание стрелки (смещено назад)
                        val arrowBase = PointF(
                            arrowTip.x - unitDx * ARROW_LENGTH,
                            arrowTip.y - unitDy * ARROW_LENGTH
                        )

                        // Боковые точки стрелки
                        val angle = atan2(dy, dx)
                        val leftWing = PointF(
                            arrowBase.x - ARROW_SIZE * cos(angle - ARROW_ANGLE).toFloat(),
                            arrowBase.y - ARROW_SIZE * sin(angle - ARROW_ANGLE).toFloat()
                        )
                        val rightWing = PointF(
                            arrowBase.x - ARROW_SIZE * cos(angle + ARROW_ANGLE).toFloat(),
                            arrowBase.y - ARROW_SIZE * sin(angle + ARROW_ANGLE).toFloat()
                        )

                        // Рисуем идеальную стрелку
                        val path = Path().apply {
                            moveTo(arrowTip.x, arrowTip.y)
                            lineTo(leftWing.x, leftWing.y)
                            lineTo(rightWing.x, rightWing.y)
                            close()
                        }

                        // Заливка с обводкой для четкости
                        paint.style = Paint.Style.FILL
                        canvas.drawPath(path, paint)

                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1.5f
                        canvas.drawPath(path, paint)
                    }

                    // Вес ребра
                    val weight = edgeWeights[v1 to v2] ?: 1
                    val midX = (start.x + end.x) / 2
                    val midY = (start.y + end.y) / 2
                    canvas.drawText(weight.toString(), midX, midY, weightPaint)
                }
            }
        }

        // 2. Временное ребро (при перетаскивании)
        currentEdge?.let { (start, end) ->
            tempEdgePaint.alpha = 180
            tempEdgePaint.strokeWidth = 10f

            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = sqrt(dx * dx + dy * dy)

            if (length > VERTEX_RADIUS) {
                val unitDx = dx / length
                val unitDy = dy / length

                val lineEnd = PointF(
                    end.x - unitDx * VERTEX_RADIUS,
                    end.y - unitDy * VERTEX_RADIUS
                )

                // Линия
                canvas.drawLine(
                    start.x + unitDx * VERTEX_RADIUS,
                    start.y + unitDy * VERTEX_RADIUS,
                    lineEnd.x,
                    lineEnd.y,
                    tempEdgePaint
                )

                // Временная стрелка
                if (isDirected) {
                    tempEdgePaint.alpha = 220
                    tempEdgePaint.style = Paint.Style.FILL

                    val angle = atan2(dy, dx)
                    val arrowBase = PointF(
                        lineEnd.x - unitDx * ARROW_LENGTH * 0.7f,
                        lineEnd.y - unitDy * ARROW_LENGTH * 0.7f
                    )

                    val path = Path().apply {
                        moveTo(lineEnd.x, lineEnd.y)
                        lineTo(
                            arrowBase.x - ARROW_SIZE * 0.8f * cos(angle - ARROW_ANGLE).toFloat(),
                            arrowBase.y - ARROW_SIZE * 0.8f * sin(angle - ARROW_ANGLE).toFloat()
                        )
                        lineTo(
                            arrowBase.x - ARROW_SIZE * 0.8f * cos(angle + ARROW_ANGLE).toFloat(),
                            arrowBase.y - ARROW_SIZE * 0.8f * sin(angle + ARROW_ANGLE).toFloat()
                        )
                        close()
                    }

                    canvas.drawPath(path, tempEdgePaint)
                }
            }
        }

        // 3. Вершины (без изменений)
        vertices.forEachIndexed { index, vertex ->
            val color = vertexColors[index] ?:
            if (highlightedVertices.contains(index)) vertexHighlightPaint.color
            else vertexPaint.color

            val paint = if (highlightedVertices.contains(index)) {
                vertexHighlightPaint.apply { this.color = color }
            } else {
                vertexPaint.apply { this.color = color }
            }

            canvas.drawCircle(vertex.x, vertex.y, VERTEX_RADIUS, paint)
            canvas.drawText(index.toString(), vertex.x, vertex.y + 15, textPaint)
        }
    }

    private fun drawArrow(canvas: Canvas, start: PointF, end: PointF, paint: Paint) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)
        if (length < 5f) return // Не рисуем стрелки для очень коротких ребер

        val angle = atan2(dy, dx)
        val arrowSize = 20f
        val arrowLength = 15f // Длина стержня стрелки

        // Точка начала стрелки (с отступом от конечной вершины)
        val arrowStartX = end.x - (dx / length) * arrowLength
        val arrowStartY = end.y - (dy / length) * arrowLength

        // Создаем путь для стрелки
        val path = Path().apply {
            moveTo(end.x, end.y)
            lineTo(
                arrowStartX - arrowSize * cos(angle - PI / 6).toFloat(),
                arrowStartY - arrowSize * sin(angle - PI / 6).toFloat()
            )
            lineTo(
                arrowStartX - arrowSize * cos(angle + PI / 6).toFloat(),
                arrowStartY - arrowSize * sin(angle + PI / 6).toFloat()
            )
            close()
        }

        // Рисуем стрелку
        canvas.drawPath(path, paint)
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
            if (isDirected) {
                // В ориентированном режиме добавляем только одно направление
                if (!edges.contains(v1 to v2)) {
                    edges.add(v1 to v2)
                    edgeWeights[v1 to v2] = weight
                }
            } else {
                // В неориентированном режиме добавляем оба направления
                if (!edges.contains(v1 to v2) && !edges.contains(v2 to v1)) {
                    edges.add(v1 to v2)
                    edges.add(v2 to v1)
                    edgeWeights[v1 to v2] = weight
                    edgeWeights[v2 to v1] = weight
                }
            }
            invalidate()
        }
    }

    fun removeEdge(v1: Int, v2: Int) {
        if (isDirected) {
            edges.removeAll { (first, second) -> first == v1 && second == v2 }
            edgeWeights.remove(v1 to v2)
        } else {
            edges.removeAll { (first, second) ->
                (first == v1 && second == v2) || (first == v2 && second == v1)
            }
            edgeWeights.remove(v1 to v2)
            edgeWeights.remove(v2 to v1)
        }
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

        // Сортируем вершины по значению для правильного порядка
        val sortedVertices = graph.vertices.sorted()

        // Добавляем вершины с их позициями
        sortedVertices.forEach { vertex ->
            graph.getVertexPosition(vertex)?.let { (x, y) ->
                vertices.add(PointF(x, y))
            } ?: run {
                // Если позиция не задана, размещаем случайно (не должно происходить для дерева)
                vertices.add(PointF(
                    (100 + (0..1000).random()).toFloat(),
                    (100 + (0..500).random()).toFloat()
                ))
            }
        }

        // Добавляем ребра
        graph.edges.forEach { (v1, v2) ->
            val index1 = sortedVertices.indexOf(v1)
            val index2 = sortedVertices.indexOf(v2)
            if (index1 != -1 && index2 != -1) {
                edges.add(index1 to index2)
                edgeWeights[index1 to index2] = graph.weights[v1 to v2] ?: 1
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