package com.example.graphsapp

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.*

class Graph {
    val vertices = mutableSetOf<Int>()
    val edges = mutableListOf<Pair<Int, Int>>()
    val weights = mutableMapOf<Pair<Int, Int>, Int>()
    private val vertexPositions = mutableMapOf<Int, Pair<Float, Float>>()
    private var algorithmPath: List<Int>? = null
    private var animationHandler: Handler? = null
    private var animationRunnable: Runnable? = null
    private var animationStep = 0
    private var animationPath: List<Int> = emptyList()


    fun setVertexPosition(vertex: Int, x: Float, y: Float) {
        vertexPositions[vertex] = Pair(x, y)
    }

    fun getVertexPosition(vertex: Int): Pair<Float, Float>? {
        return vertexPositions[vertex]
    }

    fun getAllVertexPositions(): Map<Int, Pair<Float, Float>> {
        return vertexPositions.toMap()
    }


    fun copy(): Graph {
        val newGraph = Graph()
        newGraph.vertices.addAll(vertices)
        newGraph.edges.addAll(edges)
        newGraph.weights.putAll(weights)
        newGraph.vertexPositions.putAll(vertexPositions)
        return newGraph
    }

    fun findShortestPath(start: Int, end: Int): List<Int>? {
        val distances = mutableMapOf<Int, Int>()
        val previous = mutableMapOf<Int, Int?>()
        val priorityQueue = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })

        vertices.forEach {
            distances[it] = Int.MAX_VALUE
            previous[it] = null
        }

        distances[start] = 0
        priorityQueue.add(start to 0)

        while (priorityQueue.isNotEmpty()) {
            val (current, currentDist) = priorityQueue.poll()!!
            if (current == end) break
            if (currentDist > distances[current]!!) continue

            getAdjacencyList()[current]?.forEach { (neighbor, weight) ->
                val distance = currentDist + weight
                if (distance < distances[neighbor]!!) {
                    distances[neighbor] = distance
                    previous[neighbor] = current
                    priorityQueue.add(neighbor to distance)
                }
            }
        }

        if (previous[end] == null && start != end) return null

        val path = mutableListOf<Int>()
        var current: Int? = end
        while (current != null) {
            path.add(current)
            current = previous[current]
        }

        return path.reversed()
    }

    fun animateAlgorithmPath(path: List<Int>, onStep: (Int) -> Unit, onComplete: () -> Unit) {
        try {
            stopAnimation()
            if (animationHandler == null) {
                animationHandler = Handler(Looper.getMainLooper())
            }

            animationStep = 0
            animationPath = path

            animationRunnable = object : Runnable {
                override fun run() {
                    if (animationStep < animationPath.size) {
                        try {
                            onStep(animationPath[animationStep])
                            animationStep++
                            animationHandler?.postDelayed(this, 1000)
                        } catch (e: Exception) {
                            Log.e("Graph", "Animation error", e)
                            onComplete()
                        }
                    } else {
                        onComplete()
                    }
                }
            }

            animationHandler?.post(animationRunnable as Runnable)
        } catch (e: Exception) {
            Log.e("Graph", "Error starting animation", e)
        }
    }


    fun setAnimationHandler(handler: Handler) {
        this.animationHandler = handler
    }

    fun animateAlgorithm(path: List<Int>, onStep: (Int) -> Unit, onComplete: () -> Unit) {
        stopAnimation()
        animationHandler = Handler(Looper.getMainLooper())

        animationStep = 0
        animationPath = path

        animationRunnable = object : Runnable {
            override fun run() {
                if (animationStep < animationPath.size) {
                    onStep(animationPath[animationStep])
                    animationStep++
                    animationHandler?.postDelayed(this, 1000)
                } else {
                    onComplete()
                }
            }
        }
        animationHandler?.post(animationRunnable as Runnable)
    }

    fun stopAnimation() {
        animationHandler?.removeCallbacksAndMessages(null)
        animationRunnable = null
    }

    var isDirected: Boolean = false

    // Добавление ребра с весом (по умолчанию вес = 1)
    fun addEdge(v1: Int, v2: Int, weight: Int = 1) {
        vertices.add(v1)
        vertices.add(v2)
        edges.add(Pair(v1, v2))
        weights[Pair(v1, v2)] = weight
        weights[Pair(v2, v1)] = weight

        if (!isDirected) {
            // В неориентированном режиме добавляем обратное ребро
            weights[Pair(v2, v1)] = weight
        }
    }

    // Удаление ребра
    fun removeEdge(v1: Int, v2: Int) {
        edges.removeIf { (first, second) -> (first == v1 && second == v2) || (first == v2 && second == v1) }
        weights.remove(Pair(v1, v2))
        weights.remove(Pair(v2, v1))

        if (!isDirected) {
            edges.removeIf { (first, second) -> first == v2 && second == v1 }
            weights.remove(Pair(v2, v1))
        }
    }

    // Удаление вершины и всех связанных с ней рёбер
    fun removeVertex(vertex: Int) {
        vertices.remove(vertex)
        edges.removeIf { (first, second) -> first == vertex || second == vertex }
        weights.keys.removeAll { it.first == vertex || it.second == vertex }
    }

    // Получение количества рёбер
    fun getEdgeCount(): Int {
        return edges.size
    }

    // Получение матрицы смежности
    fun getAdjacencyMatrix(): Array<IntArray> {
        val size = vertices.size
        val matrix = Array(size) { IntArray(size) }
        val vertexList = vertices.toList()
        edges.forEach { (v1, v2) ->
            val i = vertexList.indexOf(v1)
            val j = vertexList.indexOf(v2)
            matrix[i][j] = weights[Pair(v1, v2)] ?: 1
            matrix[j][i] = weights[Pair(v2, v1)] ?: 1
        }
        return matrix
    }


    // Получение матрицы инцидентности
    fun getIncidenceMatrix(): Array<IntArray> {
        val vertexList = vertices.toList()
        val matrix = Array(vertexList.size) { IntArray(edges.size) }
        edges.forEachIndexed { edgeIndex, (v1, v2) ->
            val i = vertexList.indexOf(v1)
            val j = vertexList.indexOf(v2)
            matrix[i][edgeIndex] = weights[Pair(v1, v2)] ?: 1
            matrix[j][edgeIndex] = weights[Pair(v2, v1)] ?: 1
        }
        return matrix
    }

    // Алгоритм Флойда-Уоршелла
    fun floydWarshall(): Map<Pair<Int, Int>, Int> {
        val dist = mutableMapOf<Pair<Int, Int>, Int>()

        // Инициализация
        vertices.forEach { i ->
            vertices.forEach { j ->
                if (i == j) {
                    dist[i to j] = 0
                } else {
                    val edge = edges.find { it.first == i && it.second == j }
                    dist[i to j] = edge?.let { weights[it] } ?: Int.MAX_VALUE
                }
            }
        }

        // Основной алгоритм
        vertices.forEach { k ->
            vertices.forEach { i ->
                vertices.forEach { j ->
                    if (dist[i to k] != Int.MAX_VALUE && dist[k to j] != Int.MAX_VALUE) {
                        if (dist[i to j]!! > dist[i to k]!! + dist[k to j]!!) {
                            dist[i to j] = dist[i to k]!! + dist[k to j]!!
                        }
                    }
                }
            }
        }

        return dist
    }

    // Алгоритм Форда-Беллмана
    fun fordBellman(start: Int): Map<Int, Int>? {
        val dist = mutableMapOf<Int, Int>().apply {
            vertices.forEach { put(it, Int.MAX_VALUE) }
            put(start, 0)
        }

        // Релаксация ребер
        repeat(vertices.size - 1) {
            edges.forEach { (u, v) ->
                if (dist[u]!! != Int.MAX_VALUE && dist[v]!! > dist[u]!! + (weights[u to v] ?: 0)) {
                    dist[v] = dist[u]!! + (weights[u to v] ?: 0)
                }
            }
        }

        // Проверка на отрицательные циклы
        edges.forEach { (u, v) ->
            if (dist[u]!! != Int.MAX_VALUE && dist[v]!! > dist[u]!! + (weights[u to v] ?: 0)) {
                return null // Отрицательный цикл обнаружен
            }
        }

        return dist
    }

    // Получение списка смежности
    fun getAdjacencyList(): Map<Int, List<Pair<Int, Int>>> {
        val adjacencyList = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
        edges.forEach { (v1, v2) ->
            // Добавляем ребро из v1 в v2
            adjacencyList.getOrPut(v1) { mutableListOf() }.add(
                Pair(v2, weights[Pair(v1, v2)] ?: 1)
            )

            // Если граф неориентированный, добавляем обратное ребро
            if (!isDirected) {
                adjacencyList.getOrPut(v2) { mutableListOf() }.add(
                    Pair(v1, weights[Pair(v2, v1)] ?: 1)
                )
            }
        }
        return adjacencyList
    }

    fun greedyTSP(startVertex: Int): List<Int>? {
        if (vertices.isEmpty()) return null

        val visited = mutableSetOf<Int>()
        val path = mutableListOf<Int>()
        var current = startVertex
        var totalWeight = 0

        visited.add(current)
        path.add(current)

        while (visited.size < vertices.size) {
            val neighbors = getAdjacencyList()[current]?.filter { it.first !in visited } ?: emptyList()
            if (neighbors.isEmpty()) {
                // Если нет непосещённых соседей, но не все вершины посещены
                return null
            }

            // Выбираем соседа с минимальным весом ребра
            val next = neighbors.minByOrNull { it.second } ?: return null
            path.add(next.first)
            visited.add(next.first)
            totalWeight += next.second
            current = next.first
        }

        // Замыкаем цикл - возвращаемся в стартовую вершину
        val edgeToStart = getAdjacencyList()[current]?.find { it.first == startVertex }
        if (edgeToStart != null) {
            path.add(startVertex)
            totalWeight += edgeToStart.second
        } else {
            // Нет обратного пути в стартовую вершину
            return null
        }

        return path
    }


    fun animateTSPPath(path: List<Int>, onStep: (Int) -> Unit, onComplete: () -> Unit) {
        stopAnimation()
        animationHandler = Handler(Looper.getMainLooper())
        animationStep = 0
        animationPath = path
        animationRunnable = object : Runnable {
            override fun run() {
                if (animationStep < animationPath.size) {
                    onStep(animationPath[animationStep])
                    animationStep++
                    animationHandler?.postDelayed(this, 1000)
                } else {
                    onComplete()
                }
            }
        }
        animationHandler?.post(animationRunnable as Runnable)
    }

    // Алгоритм Дейкстры
    fun dijkstra(startVertex: Int): Map<Int, Int> {
        val distances = mutableMapOf<Int, Int>()
        val priorityQueue = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
        vertices.forEach { distances[it] = Int.MAX_VALUE }
        distances[startVertex] = 0
        priorityQueue.add(Pair(startVertex, 0))

        while (priorityQueue.isNotEmpty()) {
            val (currentVertex, currentDistance) = priorityQueue.poll()!!
            if (currentDistance > distances[currentVertex]!!) continue

            getAdjacencyList()[currentVertex]?.forEach { (neighbor, weight) ->
                val distance = currentDistance + weight
                if (distance < distances[neighbor]!!) {
                    distances[neighbor] = distance
                    priorityQueue.add(Pair(neighbor, distance))
                }
            }
        }

        return distances
    }

    // Поиск всех простых путей между двумя вершинами
    fun findSimplePaths(start: Int, end: Int): List<List<Int>> {
        val paths = mutableListOf<List<Int>>()
        val visited = mutableSetOf<Int>()
        val path = mutableListOf<Int>()

        fun dfs(current: Int) {
            visited.add(current)
            path.add(current)

            if (current == end) {
                paths.add(path.toList())
            } else {
                getAdjacencyList()[current]?.forEach { (neighbor, _) ->
                    if (neighbor !in visited) {
                        dfs(neighbor)
                    }
                }
            }

            path.removeAt(path.size - 1)
            visited.remove(current)
        }

        dfs(start)
        return paths
    }

    // Проверка, является ли граф деревом
    fun isTree(): Boolean {
        return isConnected() && edges.size == vertices.size - 1
    }

    // Проверка, является ли граф лесом
    fun isForest(): Boolean {
        return !isConnected() && edges.size < vertices.size
    }

    // Алгоритм Прима для поиска минимального остовного дерева
    fun prim(): List<Pair<Int, Int>> {
        val mst = mutableListOf<Pair<Int, Int>>()
        if (vertices.isEmpty()) return mst

        val visited = mutableSetOf(vertices.first())
        val edgesQueue = PriorityQueue<Pair<Int, Pair<Int, Int>>>(compareBy { it.first })

        while (visited.size < vertices.size) {
            visited.forEach { vertex ->
                getAdjacencyList()[vertex]?.forEach { (neighbor, weight) ->
                    if (neighbor !in visited) {
                        edgesQueue.add(weight to (vertex to neighbor))
                    }
                }
            }

            if (edgesQueue.isEmpty()) break

            val (_, edge) = edgesQueue.poll()!!
            if (edge.second !in visited) {
                mst.add(edge)
                visited.add(edge.second)
            }
        }

        return mst
    }

    // Алгоритм Краскала для поиска минимального остовного дерева
    fun kruskal(): List<Pair<Int, Int>> {
        val mst = mutableListOf<Pair<Int, Int>>()
        val parent = mutableMapOf<Int, Int>().apply {
            vertices.forEach { put(it, it) }
        }

        fun find(v: Int): Int {
            return if (parent[v] == v) v else find(parent[v]!!).also { parent[v] = it }
        }

        edges.sortedBy { weights[it] ?: 1 }.forEach { (u, v) ->
            val rootU = find(u)
            val rootV = find(v)
            if (rootU != rootV) {
                mst.add(u to v)
                parent[rootU] = rootV
            }
        }

        return mst
    }

    fun layoutTree() {
        if (vertices.isEmpty()) return

        val levels = mutableMapOf<Int, Int>()
        val childrenCount = mutableMapOf<Int, Int>()

        // Вычисляем уровни и количество детей
        fun traverse(root: Int, level: Int) {
            levels[root] = level
            childrenCount[root] = getAdjacencyList()[root]?.size ?: 0

            getAdjacencyList()[root]?.forEach { (child, _) ->
                traverse(child, level + 1)
            }
        }

        // Находим корень (вершину с одним ребром)
        val root = vertices.firstOrNull { getAdjacencyList()[it]?.size == 1 } ?: vertices.first()
        traverse(root, 0)

        val maxLevel = levels.values.maxOrNull() ?: 0
        val screenWidth = 1000f // Примерная ширина
        val levelHeight = 150f

        vertices.forEach { vertex ->
            val level = levels[vertex] ?: 0
            getAdjacencyList()[vertex]?.size ?: 0
            val x = screenWidth * (level + 1) / (maxLevel + 2)
            val y = 100f + level * levelHeight
            setVertexPosition(vertex, x, y)
        }
    }

    // Генерация кода Прюфера для дерева
    fun toPruferCode(): List<Int>? {
        if (!isTree()) {
            Log.d("Prufer", "Граф не является деревом")
            return null
        }

        if (vertices.size <= 2) {
            Log.d("Prufer", "Для дерева с 2 или менее вершинами код Прюфера пуст")
            return emptyList()
        }

        val code = mutableListOf<Int>()
        val degree = mutableMapOf<Int, Int>().apply {
            vertices.forEach { v -> put(v, getDegree(v)) }
        }
        val leaves = PriorityQueue<Int>().apply {
            degree.filter { it.value == 1 }.keys.forEach { add(it) }
        }

        val tempEdges = edges.toMutableList()

        repeat(vertices.size - 2) {
            val leaf = leaves.poll()
            val edge = tempEdges.find { it.first == leaf || it.second == leaf } ?: return null
            val neighbor = if (edge.first == leaf) edge.second else edge.first

            code.add(neighbor)
            tempEdges.remove(edge)

            degree[neighbor] = degree[neighbor]!! - 1
            if (degree[neighbor] == 1) {
                leaves.add(neighbor)
            }
        }

        Log.d("Prufer", "Сгенерированный код: $code")
        return code
    }

    fun setEdgeWeight(v1: Int, v2: Int, weight: Int) {
        edges.find { (first, second) ->
            (first == v1 && second == v2) || (first == v2 && second == v1)
        }?.let {
            weights[Pair(v1, v2)] = weight
            weights[Pair(v2, v1)] = weight
        }
    }


    fun fromPruferCode(code: List<Int>): Graph {
        return try {
            val graph = Graph()
            if (code.isEmpty()) return graph // обработка пустого кода

            val degree = mutableMapOf<Int, Int>()
            val vertexSet = mutableSetOf<Int>()

            code.forEach { v ->
                degree[v] = (degree[v] ?: 0) + 1
                vertexSet.add(v)
            }

            val n = code.size + 2
            (1..n).forEach { vertexSet.add(it) }
            (1..n).forEach { degree.putIfAbsent(it, 1) }

            val leaves = PriorityQueue<Int>().apply {
                degree.filter { it.value == 1 }.keys.forEach { add(it) }
            }

            code.forEach { v ->
                val leaf = leaves.poll() ?: return graph
                graph.addEdge(v, leaf)
                degree[v] = (degree[v] ?: 1) - 1
                degree[leaf] = (degree[leaf] ?: 1) - 1

                if (degree[v] == 1) leaves.add(v)
            }

            val last = leaves.toList()
            if (last.size == 2) {
                graph.addEdge(last[0], last[1])
            }

            // Автоматическое размещение вершин
            graph.vertices.forEachIndexed { index, vertex ->
                graph.setVertexPosition(
                    vertex,
                    100f + index * 200f,
                    300f + (index % 2) * 200f
                )
            }

            graph
        } catch (e: Exception) {
            Log.e("Graph", "Error in fromPruferCode", e)
            Graph() // возвращаем пустой граф в случае ошибки
        }
    }

    // Проверка связности графа
    private fun isConnected(): Boolean {
        if (vertices.isEmpty()) return false

        val visited = mutableSetOf<Int>()
        dfsRecursive(vertices.first(), visited)
        return visited.size == vertices.size
    }

    // Рекурсивный обход в глубину (DFS)
    private fun dfsRecursive(vertex: Int, visited: MutableSet<Int>) {
        visited.add(vertex)
        getAdjacencyList()[vertex]?.forEach { (neighbor, _) ->
            if (neighbor !in visited) {
                dfsRecursive(neighbor, visited)
            }
        }
    }

    // Анимация BFS
    fun animateBFS(startVertex: Int, onStep: (Int) -> Unit) {
        val visited = mutableSetOf<Int>()
        val queue = LinkedList<Int>()
        queue.add(startVertex)

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (queue.isNotEmpty()) {
                    val vertex = queue.poll()
                    if (vertex!! !in visited) {
                        visited.add(vertex)
                        onStep(vertex)
                        getAdjacencyList()[vertex]?.forEach { (neighbor, _) ->
                            queue.add(neighbor)
                        }
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }

    // Анимация DFS
    fun animateDFS(startVertex: Int, onStep: (Int) -> Unit) {
        val visited = mutableListOf<Int>()
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { dfsRecursive(startVertex, visited, onStep, handler) }
        handler.post(runnable)
    }

    // Рекурсивный DFS для анимации
    private fun dfsRecursive(
        vertex: Int,
        visited: MutableList<Int>,
        onStep: (Int) -> Unit,
        handler: Handler
    ) {
        if (vertex !in visited) {
            visited.add(vertex)
            onStep(vertex)
            handler.postDelayed({
                getAdjacencyList()[vertex]?.forEach { (neighbor, _) ->
                    dfsRecursive(neighbor, visited, onStep, handler)
                }
            }, 1000)
        }
    }

    // Поиск Эйлерова цикла
    fun findEulerianCycle(): List<Int>? {
        if (!isConnected() || vertices.any { getDegree(it) % 2 != 0 }) {
            return null
        }

        val tempEdges = edges.toMutableList()
        val stack = ArrayDeque<Int>()
        val cycle = mutableListOf<Int>()
        stack.push(vertices.first())

        while (stack.isNotEmpty()) {
            val current = stack.peek()
            val edge = tempEdges.find { it.first == current || it.second == current }

            if (edge != null) {
                tempEdges.remove(edge)
                val next = if (edge.first == current) edge.second else edge.first
                stack.push(next)
            } else {
                cycle.add(stack.pop())
            }
        }

        return if (cycle.size == edges.size + 1) cycle else null
    }

    // Поиск Гамильтонова цикла
    fun findHamiltonianCycle(): List<Int>? {
        if (vertices.isEmpty()) return null

        val path = mutableListOf<Int>()
        val adjacencyList = getAdjacencyList()
        val vertexList = vertices.toList()

        path.add(vertexList.first())

        fun backtrack(currentVertex: Int): Boolean {
            if (path.size == vertices.size) {
                val firstVertex = path.first()
                if (adjacencyList[currentVertex]?.any { it.first == firstVertex } == true) {
                    path.add(firstVertex)
                    return true
                }
                return false
            }

            adjacencyList[currentVertex]?.forEach { (neighbor, _) ->
                if (neighbor !in path) {
                    path.add(neighbor)
                    if (backtrack(neighbor)) {
                        return true
                    }
                    path.removeAt(path.lastIndex)
                }
            }

            return false
        }

        return if (backtrack(vertices.first())) path else null
    }
    fun getBFSPath(startVertex: Int): List<Int> {
        val visited = mutableSetOf<Int>()
        val queue = LinkedList<Int>()
        queue.add(startVertex)

        while (queue.isNotEmpty()) {
            val vertex = queue.poll()
            if (vertex!! !in visited) {
                visited.add(vertex)
                getAdjacencyList()[vertex]?.forEach { (neighbor, _) ->
                    queue.add(neighbor)
                }
            }
        }
        return visited.toList()
    }

    fun getDFSPath(startVertex: Int): List<Int> {
        val visited = mutableSetOf<Int>()
        dfsRecursive(startVertex, visited)
        val stack = Stack<Int>()
        stack.push(startVertex)

        while (stack.isNotEmpty()) {
            val vertex = stack.pop()
            if (vertex !in visited) {
                visited.add(vertex)
                getAdjacencyList()[vertex]?.forEach { (neighbor, _) ->
                    stack.push(neighbor)
                }
            }
        }
        return visited.toList()
    }

    // Получение степени вершины
    private fun getDegree(vertex: Int): Int {
        return edges.count { it.first == vertex || it.second == vertex }
    }

    // Установка пути для визуализации
    fun setAlgorithmPath(path: List<Int>) {
        this.algorithmPath = path
    }

    fun greedyColoring(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        val adjacencyList = getAdjacencyList()

        vertices.sorted().forEach { u ->
            val usedColors = adjacencyList[u]?.mapNotNull { (v, _) -> result[v] }?.toSet() ?: emptySet()
            var cr = 0
            while (cr in usedColors) {
                cr++
            }
            result[u] = cr
        }
        return result
    }


    private var vertexColors: Map<Int, Int> = emptyMap()

    fun setColoring(coloring: Map<Int, Int>) {
        this.vertexColors = coloring
    }

    fun getColoring(): Map<Int, Int> {
        return vertexColors
    }

    fun sequentialColoring(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        val adjacencyList = getAdjacencyList()

        vertices.sorted().forEach { u ->
            val usedColors = adjacencyList[u]?.mapNotNull { (v, _) -> result[v] }?.toSet() ?: emptySet()
            var cr = 0
            while (cr in usedColors) {
                cr++
            }
            result[u] = cr
        }
        return result
    }

    // Получение пути для визуализации
    fun getAlgorithmPath(): List<Int>? {
        return algorithmPath
    }

    fun toSerializable(): SerializableGraph {
        return SerializableGraph(
            vertices.toList(),
            edges,
            weights.mapKeys { "${it.key.first},${it.key.second}" },
            vertexPositions.mapKeys { it.key.toString() }.mapValues { it.value }
        )
    }

    companion object {
        fun fromSerializable(serializable: SerializableGraph): Graph {
            val graph = Graph()
            graph.vertices.addAll(serializable.vertices)
            graph.edges.addAll(serializable.edges)
            serializable.weights.forEach { (key, value) ->
                val (first, second) = key.split(",").map { it.toInt() }
                graph.weights[Pair(first, second)] = value
            }
            serializable.vertexPositions.forEach { (key, value) ->
                graph.vertexPositions[key.toInt()] = value
            }
            return graph
        }
    }
}

data class SerializableGraph(
    val vertices: List<Int>,
    val edges: List<Pair<Int, Int>>,
    val weights: Map<String, Int>,
    val vertexPositions: Map<String, Pair<Float, Float>> = emptyMap()
)