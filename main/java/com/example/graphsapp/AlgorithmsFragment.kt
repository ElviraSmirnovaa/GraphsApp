package com.example.graphsapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class AlgorithmsFragment : Fragment() {

    private lateinit var outputText: TextView
    private lateinit var startVertexInput: EditText
    private lateinit var endVertexInput: EditText
    private var currentPathIndex = 0
    private var simplePaths = listOf<List<Int>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_algorithms, container, false)

        outputText = view.findViewById(R.id.output_text)
        startVertexInput = view.findViewById(R.id.start_vertex_input)
        endVertexInput = view.findViewById(R.id.end_vertex_input)

        setupAlgorithmButtons(view)
        return view
    }

    private fun setupAlgorithmButtons(view: View) {
        view.findViewById<Button>(R.id.bfs_button).setOnClickListener { runBFS() }
        view.findViewById<Button>(R.id.dfs_button).setOnClickListener { runDFS() }
        view.findViewById<Button>(R.id.dijkstra_button).setOnClickListener { runDijkstra() }
        view.findViewById<Button>(R.id.simple_paths_button).setOnClickListener { findSimplePaths() }
        view.findViewById<Button>(R.id.next_path_button).setOnClickListener { showNextPath() }
        view.findViewById<Button>(R.id.prim_button).setOnClickListener { runPrim() }
        view.findViewById<Button>(R.id.kruskal_button).setOnClickListener { runKruskal() }
        view.findViewById<Button>(R.id.prufer_button).setOnClickListener { showPruferCode() }
        view.findViewById<Button>(R.id.coloring_greedy_button).setOnClickListener { runGreedyColoring() }
        view.findViewById<Button>(R.id.coloring_sequential_button).setOnClickListener { runSequentialColoring() }
        view.findViewById<Button>(R.id.tsp_button).setOnClickListener { runGreedyTSP() }
        view.findViewById<Button>(R.id.floyd_button).setOnClickListener { runFloyd() }
        view.findViewById<Button>(R.id.ford_button).setOnClickListener { runFord() }
    }

    fun updateButtonsAvailability(isDirected: Boolean) {
        view?.let {
            // Кнопки для неориентированных графов
            it.findViewById<Button>(R.id.bfs_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.dfs_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.dijkstra_button)?.isEnabled = true
            it.findViewById<Button>(R.id.simple_paths_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.prim_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.kruskal_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.prufer_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.coloring_greedy_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.coloring_sequential_button)?.isEnabled = !isDirected
            it.findViewById<Button>(R.id.tsp_button)?.isEnabled = !isDirected

            // Кнопки для ориентированных графов
            it.findViewById<Button>(R.id.floyd_button)?.isEnabled = isDirected
            it.findViewById<Button>(R.id.ford_button)?.isEnabled = isDirected

            // Видимость кнопок
            it.findViewById<Button>(R.id.floyd_button)?.visibility = if (isDirected) View.VISIBLE else View.GONE
            it.findViewById<Button>(R.id.ford_button)?.visibility = if (isDirected) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runBFS() {
        try {
            (activity as? MainActivity)?.apply {
                graph?.let { graph ->
                    if (graph.vertices.isEmpty()) {
                        outputText.text = "Граф пуст"
                        return@let
                    }
                    val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: return@let
                    val path = graph.getBFSPath(start)

                    saveCurrentAlgorithmData(
                        path = path,
                        type = "BFS",
                        coloring = null
                    )

                    outputText.text = "BFS обход: ${path.joinToString(" -> ")}"
                    animateAlgorithm(path)
                }
            }
        } catch (e: Exception) {
            outputText.text = "Ошибка при выполнении BFS: ${e.message}"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runDFS() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: 0
                val path = graph.getDFSPath(start)

                saveCurrentAlgorithmData(
                    path = path,
                    type = "DFS",
                    coloring = null
                )

                outputText.text = "DFS обход: ${path.joinToString(" -> ")}"
                animateAlgorithm(path)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runDijkstra() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: 0
                val end = endVertexInput.text.toString().toIntOrNull() ?: graph.vertices.lastOrNull() ?: 0
                val shortestPath = graph.findShortestPath(start, end)
                if (shortestPath != null) {
                    saveCurrentAlgorithmData(
                        path = shortestPath,
                        type = "DIJKSTRA",
                        coloring = null
                    )
                    outputText.text = "Кратчайший путь от $start до $end: ${shortestPath.joinToString(" -> ")}"
                    animateAlgorithm(shortestPath)
                } else {
                    outputText.text = "Путь от $start до $end не существует"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun findSimplePaths() {
        (activity as? MainActivity)?.graph?.let { graph ->
            val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: 0
            val end = endVertexInput.text.toString().toIntOrNull() ?: graph.vertices.lastOrNull() ?: 0

            simplePaths = graph.findSimplePaths(start, end)
            currentPathIndex = 0

            if (simplePaths.isNotEmpty()) {
                showCurrentPath()
            } else {
                outputText.text = "Простых путей от $start до $end не найдено"
            }
        }
    }

    private fun showNextPath() {
        if (simplePaths.isNotEmpty()) {
            currentPathIndex = (currentPathIndex + 1) % simplePaths.size
            showCurrentPath()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showCurrentPath() {
        val path = simplePaths[currentPathIndex]
        (activity as? MainActivity)?.saveCurrentAlgorithmData(
            path = path,
            type = "SIMPLE_PATHS",
            coloring = null
        )
        outputText.text = "Простая цепь ${currentPathIndex + 1}/${simplePaths.size}:\n${path.joinToString(" -> ")}"
        animateAlgorithm(path)
    }

    @SuppressLint("SetTextI18n")
    private fun runPrim() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                val mst = graph.prim()
                val mstGraph = Graph()
                graph.vertices.forEach { vertex ->
                    mstGraph.vertices.add(vertex)
                    graph.getVertexPosition(vertex)?.let { pos ->
                        mstGraph.setVertexPosition(vertex, pos.first, pos.second)
                    }
                }
                mst.forEach { (v1, v2) ->
                    val weight = graph.weights[Pair(v1, v2)] ?: 1
                    mstGraph.addEdge(v1, v2, weight)
                }
                this.graph = mstGraph

                saveCurrentAlgorithmData(
                    path = mst.flatMap { listOf(it.first, it.second) }.distinct(),
                    type = "PRIM",
                    coloring = null
                )

                outputText.text = "Минимальное остовное дерево (Прима): ${mst.size} ребер"
                animateAlgorithm(mst.flatMap { listOf(it.first, it.second) }.distinct())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runKruskal() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                val mst = graph.kruskal()
                val mstGraph = Graph()
                graph.vertices.forEach { vertex ->
                    mstGraph.vertices.add(vertex)
                    graph.getVertexPosition(vertex)?.let { pos ->
                        mstGraph.setVertexPosition(vertex, pos.first, pos.second)
                    }
                }
                mst.forEach { (v1, v2) ->
                    val weight = graph.weights[Pair(v1, v2)] ?: 1
                    mstGraph.addEdge(v1, v2, weight)
                }
                this.graph = mstGraph

                saveCurrentAlgorithmData(
                    path = mst.flatMap { listOf(it.first, it.second) }.distinct(),
                    type = "KRUSKAL",
                    coloring = null
                )

                outputText.text = "Минимальное остовное дерево (Краскала): ${mst.size} ребер"
                animateAlgorithm(mst.flatMap { listOf(it.first, it.second) }.distinct())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showPruferCode() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                if (graph.isTree()) {
                    val code = graph.toPruferCode()
                    outputText.setTextColor(Color.parseColor("#333333"))
                    if (code != null) {
                        outputText.text = "Код Прюфера: ${code.joinToString(" ")}"
                    } else {
                        outputText.text = "Не удалось сгенерировать код Прюфера"
                    }
                } else {
                    outputText.setTextColor(Color.parseColor("#FF0000"))
                    outputText.text = "Граф не является деревом (должен быть связным без циклов)"
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runGreedyColoring() {
        try {
            (activity as? MainActivity)?.apply {
                graph?.let { graph ->
                    val coloring = graph.greedyColoring()
                    val colorMap = coloring.mapValues { entry ->
                        val colors = arrayOf(
                            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                            Color.CYAN, Color.MAGENTA, Color.GRAY
                        )
                        colors[entry.value % colors.size]
                    }
                    outputText.text = "Жадная раскраска: ${colorMap.entries.joinToString(", ") { "${it.key}=${it.value}" }}"

                    saveCurrentAlgorithmData(
                        path = colorMap.keys.toList(),
                        type = "GREEDY_COLORING",
                        coloring = colorMap
                    )

                    visualizeGraphWithColors(colorMap)
                }
            }
        } catch (e: Exception) {
            outputText.text = "Ошибка при выполнении жадной раскраски: ${e.message}"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runSequentialColoring() {
        (activity as? MainActivity)?.apply {
            graph?.let { graph ->
                val coloring = graph.sequentialColoring()
                val colorMap = coloring.mapValues { entry ->
                    val colors = arrayOf(
                        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
                        Color.CYAN, Color.MAGENTA, Color.GRAY
                    )
                    colors[entry.value % colors.size]
                }
                outputText.text = "Последовательная раскраска: ${colorMap.entries.joinToString(", ") { "${it.key}=${it.value}" }}"

                saveCurrentAlgorithmData(
                    path = colorMap.keys.toList(),
                    type = "SEQUENTIAL_COLORING",
                    coloring = colorMap
                )

                visualizeGraphWithColors(colorMap)
            }
        }
    }

    // Новые методы алгоритмов:
    @SuppressLint("SetTextI18n")
    private fun runFloyd() {
        try {
            (activity as? MainActivity)?.apply {
                graph?.let { graph ->
                    if (graph.vertices.isEmpty()) {
                        outputText.text = "Граф пуст"
                        return@let
                    }

                    val startTime = System.currentTimeMillis()
                    val distances = graph.floydWarshall()
                    val endTime = System.currentTimeMillis()

                    // Визуализация кратчайших путей
                    visualizeFloydResults(distances)

                    outputText.text = "Алгоритм Флойда-Уоршелла выполнен за ${endTime - startTime} мс"
                    Toast.makeText(context, "Алгоритм Флойда выполнен за ${endTime - startTime} мс", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            outputText.text = "Ошибка при выполнении алгоритма Флойда: ${e.message}"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runFord() {
        try {
            (activity as? MainActivity)?.apply {
                graph?.let { graph ->
                    if (graph.vertices.isEmpty()) {
                        outputText.text = "Граф пуст"
                        return@let
                    }

                    val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: return@let

                    val startTime = System.currentTimeMillis()
                    val result = graph.fordBellman(start)
                    val endTime = System.currentTimeMillis()

                    if (result == null) {
                        outputText.text = "Обнаружен отрицательный цикл"
                    } else {
                        outputText.text = "Алгоритм Форда-Беллмана выполнен за ${endTime - startTime} мс\n" +
                                "Кратчайшие пути от вершины $start:\n" +
                                result.entries.joinToString("\n") { "${it.key} = ${it.value}" }
                        Toast.makeText(context, "Алгоритм Форда выполнен за ${endTime - startTime} мс", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            outputText.text = "Ошибка при выполнении алгоритма Форда-Беллмана: ${e.message}"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Проверяем текущий режим графа при создании фрагмента
        (activity as? MainActivity)?.graph?.let {
            updateButtonsAvailability(it.isDirected)
        }
    }

    private fun visualizeFloydResults(distances: Map<Pair<Int, Int>, Int>) {
        // Реализация визуализации результатов алгоритма Флойда
        (activity as? MainActivity)?.let { activity ->
            activity.showGraphVisualization()
            // Здесь можно добавить логику выделения путей на графе
        }
    }

    @SuppressLint("SetTextI18n")
    private fun runGreedyTSP() {
        (activity as? MainActivity)?.graph?.let { graph ->
            val start = startVertexInput.text.toString().toIntOrNull() ?: graph.vertices.firstOrNull() ?: 0
            val tspPath = graph.greedyTSP(start)
            if (tspPath != null) {
                // Рассчитываем общий вес пути
                var totalWeight = 0
                for (i in 0 until tspPath.size - 1) {
                    totalWeight += graph.weights[Pair(tspPath[i], tspPath[i+1])] ?: 0
                }

                (activity as? MainActivity)?.saveCurrentAlgorithmData(
                    path = tspPath,
                    type = "TSP",
                    coloring = null
                )
                outputText.text = "Жадный TSP путь (вес: $totalWeight): ${tspPath.joinToString(" -> ")}"
                animateAlgorithm(tspPath)
            } else {
                outputText.text = "Не удалось найти TSP путь (граф не полный или нет гамильтонова цикла)"
            }
        }
    }

    private fun visualizeGraphWithColors(coloring: Map<Int, Int>) {
        (activity as? MainActivity)?.let { activity ->
            activity.showGraphVisualization()
            activity.graph.setColoring(coloring)
        }
    }

    private fun animateAlgorithm(path: List<Int>) {
        (activity as? MainActivity)?.let { activity ->
            activity.showGraphVisualization()
            activity.graph.setAlgorithmPath(path)
        }
    }
}