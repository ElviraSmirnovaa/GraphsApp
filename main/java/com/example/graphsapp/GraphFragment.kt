package com.example.graphsapp

import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class GraphFragment : Fragment() {

    // Объявление переменных для View элементов
    private lateinit var inputEdges: EditText
    private lateinit var outputText: TextView
    private lateinit var inputTypeSpinner: Spinner
    private lateinit var visualizeButton: Button
    private var selectedInputType = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Надуваем layout для фрагмента
        val view = inflater.inflate(R.layout.fragment_graph, container, false)

        // Инициализация View элементов
        inputEdges = view.findViewById(R.id.input_edges)
        outputText = view.findViewById(R.id.output_text)
        inputTypeSpinner = view.findViewById(R.id.input_type_spinner)
        val calculateButton = view.findViewById<Button>(R.id.calculate_button)
        visualizeButton = view.findViewById(R.id.visualize_button)

        // Настройка Spinner
        setupSpinner()

        // Обработчик кнопки "Рассчитать"
        calculateButton.setOnClickListener {
            when (selectedInputType) {
                0 -> processEdgeList()
                1 -> processBinaryTree()
                2 -> processPruferCode()
            }
        }

        // Обработчик кнопки "Визуализировать"
        visualizeButton.setOnClickListener {
            (activity as? MainActivity)?.showGraphVisualization()
        }

        return view
    }

    private fun setupSpinner() {
        // Получаем массив вариантов из ресурсов
        val inputTypes = resources.getStringArray(R.array.input_types_array)

        // Создаем адаптер для Spinner
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,  // Стандартный layout для элемента
            inputTypes
        ).apply {
            // Устанавливаем layout для выпадающего списка
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Привязываем адаптер к Spinner
        inputTypeSpinner.adapter = adapter

        // Обработчик выбора элемента в Spinner
        inputTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedInputType = position
                updateInputHint()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedInputType = 0
                updateInputHint()
            }
        }
    }

    private fun updateInputHint() {
        // Обновляем подсказку в поле ввода в зависимости от выбранного типа
        inputEdges.hint = when (selectedInputType) {
            0 -> "Формат: v1 v2 weight\nНапример:\n1 2 5\n2 3 3"
            1 -> "Введите значения через пробел\nНапример: 8 3 10 1 6"
            2 -> "Введите код Прюфера через пробел\nНапример: 2 2 3"
            else -> ""
        }
    }

    @SuppressLint("SetTextI18n")
    private fun processEdgeList() {
        try {
            val edgesText = inputEdges.text.toString().trim()
            if (edgesText.isEmpty()) {
                showError("Введите список ребер")
                return
            }

            val graph = Graph()
            edgesText.split("\n").forEach { line ->
                val parts = line.split(" ").mapNotNull { it.toIntOrNull() }
                when {
                    parts.size >= 2 -> {
                        val v1 = parts[0]
                        val v2 = parts[1]
                        val weight = if (parts.size >= 3) parts[2] else 1
                        graph.addEdge(v1, v2, weight)
                    }
                    parts.size == 1 -> {
                        showError("Неверный формат ребра: $line")
                        return
                    }
                }
            }

            showGraphInfo(graph)
            (activity as? MainActivity)?.graph = graph
            visualizeButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            showError("Ошибка обработки: ${e.message}")
        }
    }

    private fun processBinaryTree() {
        val values = inputEdges.text.toString().trim().split(" ")
            .mapNotNull { it.toIntOrNull() }

        if (values.isEmpty()) {
            showError("Введите числа через пробел")
            return
        }

        val binaryTree = BinaryTree().apply { values.forEach { insert(it) } }
        val graph = createGraphFromBinaryTree(binaryTree)

        // Сохраняем граф и переходим к визуализации
        (activity as? MainActivity)?.apply {
            this.graph = graph
            showGraphVisualization()
        }

        outputText.text = "Бинарное дерево построено!\nInorder обход: ${binaryTree.inorder().joinToString(", ")}"
        visualizeButton.visibility = View.VISIBLE
    }

    private fun processPruferCode() {
        try {
            val code = inputEdges.text.toString().trim().split(" ")
                .filter { it.isNotBlank() }
                .map { it.toInt() }

            if (code.any { it < 0 }) {
                showError("Код Прюфера должен содержать только неотрицательные числа")
                return
            }

            val graph = Graph().fromPruferCode(code)
            showGraphInfo(graph)
            (activity as? MainActivity)?.graph = graph
            visualizeButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            showError("Ошибка в формате кода Прюфера: ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showGraphInfo(graph: Graph) {
        val numVertices = graph.vertices.size
        val numEdges = graph.edges.size
        val adjacencyMatrix = graph.getAdjacencyMatrix()
        val incidenceMatrix = graph.getIncidenceMatrix()
        val adjacencyList = graph.getAdjacencyList()

        outputText.text = """
            Количество вершин: $numVertices
            Количество ребер: $numEdges

            Матрица смежности:
            ${adjacencyMatrix.joinToString("\n") { it.joinToString(" ") }}

            Матрица инцидентности:
            ${incidenceMatrix.joinToString("\n") { it.joinToString(" ") }}

            Список смежности:
            ${adjacencyList.entries.joinToString("\n") { "${it.key} -> ${it.value.joinToString(", ") { "${it.first}(${it.second})" }}" }}
            """.trimIndent()
    }

    private fun createGraphFromBinaryTree(tree: BinaryTree): Graph {
        val graph = Graph()
        if (tree.root == null) return graph

        // Рекурсивная функция для обхода дерева и добавления вершин/ребер
        fun traverse(node: BinaryTree.Node?, parentValue: Int?, x: Float, y: Float, level: Int, horizontalSpacing: Float) {
            if (node == null) return

            // Добавляем вершину
            graph.vertices.add(node.value)
            graph.setVertexPosition(node.value, x, y)

            // Добавляем ребро к родителю
            parentValue?.let { graph.addEdge(it, node.value) }

            // Вычисляем позиции для дочерних узлов
            val childSpacing = horizontalSpacing / 2
            val nextLevel = level + 1
            val verticalSpacing = 200f

            // Рекурсивно обрабатываем детей
            traverse(node.left, node.value, x - childSpacing, y + verticalSpacing, nextLevel, childSpacing)
            traverse(node.right, node.value, x + childSpacing, y + verticalSpacing, nextLevel, childSpacing)
        }

        // Начинаем обход с корня
        val screenCenter = resources.displayMetrics.widthPixels / 2f
        traverse(tree.root, null, screenCenter, 200f, 0, 600f)

        return graph
    }

    // Функция для анализа структуры дерева
    private fun analyzeTreeStructure(root: BinaryTree.Node?): Map<Int, List<Int>> {
        val structure = mutableMapOf<Int, MutableList<Int>>()
        fun traverse(node: BinaryTree.Node?, level: Int) {
            if (node == null) return

            structure.getOrPut(level) { mutableListOf() }.add(node.value)
            traverse(node.left, level + 1)
            traverse(node.right, level + 1)
        }
        traverse(root, 0)
        return structure
    }

    private fun showError(message: String) {
        outputText.text = message
        visualizeButton.visibility = View.GONE
    }
}